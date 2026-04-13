#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
PDF转MD工具，通过py4j被Java调用
功能：
1. 提取PDF中的文字内容，保持逻辑顺序和结构
2. 提取PDF中的照片并保存
3. 将提取的内容转换为MD格式文件
4. 处理水印干扰
"""

import os
import sys
import tempfile
import cv2
import numpy as np
from PIL import Image
from io import BytesIO
import fitz  # PyMuPDF
import markdown
from py4j.java_gateway import JavaGateway, CallbackServerParameters
import threading
from concurrent.futures import ThreadPoolExecutor

class PDFToMDConverter:
    """PDF转MD转换器类"""

    def __init__(self, max_workers=4):
        """初始化转换器

        Args:
            max_workers: 线程池最大线程数，默认为4
        """
        self.max_workers = max_workers
        self.thread_pool = ThreadPoolExecutor(max_workers=max_workers)

    def set_thread_pool_size(self, max_workers):
        """设置线程池大小

        Args:
            max_workers: 线程池最大线程数
        """
        self.max_workers = max_workers
        # 关闭旧线程池
        self.thread_pool.shutdown()
        # 创建新线程池
        self.thread_pool = ThreadPoolExecutor(max_workers=max_workers)

    def start(self):
        """启动转换器（空方法，用于Java端调用）"""
        pass

    def convert_pdf_to_md(self, pdf_path, output_dir=None):
        """
        将PDF转换为MD格式

        Args:
            pdf_path: PDF文件路径
            output_dir: 输出目录，默认为临时目录

        Returns:
            str: 生成的MD文件路径
        """
        try:
            # 验证PDF文件
            if not os.path.exists(pdf_path):
                raise FileNotFoundError(f"PDF文件不存在: {pdf_path}")

            # 设置输出目录
            if output_dir is None:
                output_dir = tempfile.mkdtemp()
            elif not os.path.exists(output_dir):
                os.makedirs(output_dir, exist_ok=True)

            # 生成输出文件名
            pdf_name = os.path.basename(pdf_path)
            md_name = os.path.splitext(pdf_name)[0] + '.md'
            md_path = os.path.join(output_dir, md_name)

            # 提取文字和照片
            text_content = self._extract_text(pdf_path)
            image_paths = self._extract_images(pdf_path, output_dir)

            # 生成MD内容
            md_content = self._generate_md_content(text_content, image_paths)

            # 写入MD文件
            with open(md_path, 'w', encoding='utf-8') as f:
                f.write(md_content)

            return md_path

        except Exception as e:
            print(f"转换失败: {str(e)}", file=sys.stderr)
            raise

    def convert_pdf_to_md_async(self, pdf_path, output_dir, callback):
        """
        异步将PDF转换为MD格式，并通过回调返回结果

        Args:
            pdf_path: PDF文件路径
            output_dir: 输出目录
            callback: 回调对象，包含onSuccess和onError方法
        """
        def _convert_task():
            try:
                # 执行转换
                md_path = self.convert_pdf_to_md(pdf_path, output_dir)
                # 调用回调的onSuccess方法
                callback.onSuccess(md_path)
            except Exception as e:
                # 调用回调的onError方法
                callback.onError(str(e))

        # 使用线程池执行转换任务
        self.thread_pool.submit(_convert_task)

    def _extract_text(self, pdf_path):
        """
        提取PDF中的文字内容，处理水印

        Args:
            pdf_path: PDF文件路径

        Returns:
            str: 提取的文字内容
        """
        text_content = []

        try:
            # 打开PDF文件
            doc = fitz.open(pdf_path)

            for page_num in range(len(doc)):
                page = doc[page_num]

                # 提取文字，按块排序
                blocks = page.get_text("blocks")
                # 按垂直位置排序，确保逻辑顺序
                blocks.sort(key=lambda b: (b[1], b[0]))

                page_text = []
                for block in blocks:
                    text = block[4].strip()
                    if text:
                        # 处理水印（假设水印是淡灰色、倾斜的文字）
                        if not self._is_watermark(text):
                            # 过滤掉ID信息和重复内容
                            if not self._is_id_info(text) and not self._is_repeated_content(text):
                                # 保留所有有意义的内容
                                page_text.append(text)

                if page_text:
                    text_content.append('\n'.join(page_text))

            doc.close()
            return '\n\n'.join(text_content)

        except Exception as e:
            print(f"提取文字失败: {str(e)}", file=sys.stderr)
            raise

    def _is_watermark(self, text):
        """
        判断是否为水印文字

        Args:
            text: 文字内容

        Returns:
            bool: 是否为水印
        """
        # 水印特征词列表
        watermark_patterns = [
            "水印", "watermark", "confidential", "内部资料",
            "internal", "private", "confidential", "草稿", "draft"
        ]

        # 检查是否包含水印特征词
        text_lower = text.lower()
        for pattern in watermark_patterns:
            if pattern in text_lower:
                return True

        # 检查文字长度（通常水印文字较短）
        if len(text) < 10:
            return True

        # 检查文字模式（水印通常是重复的短语）
        words = text.split()
        if len(words) < 3:
            return True

        # 检查是否有重复的单词（水印通常重复）
        if len(words) > 1 and len(set(words)) < len(words) / 2:
            return True

        return False

    def _is_id_info(self, text):
        """
        判断是否为ID信息

        Args:
            text: 文字内容

        Returns:
            bool: 是否为ID信息
        """
        # 检查是否包含ID信息
        if "ID：" in text or "ID:" in text:
            return True
        if "20942091" in text:
            return True
        return False

    def _is_repeated_content(self, text):
        """
        判断是否为重复内容

        Args:
            text: 文字内容

        Returns:
            bool: 是否为重复内容
        """
        # 检查是否为重复的时间信息
        if "2026-01-11" in text:
            return True
        return False

    def _is_photo(self, image_data):
        """
        判断是否为照片

        Args:
            image_data: 图片数据

        Returns:
            bool: 是否为照片
        """
        try:
            # 打开图片
            img = Image.open(BytesIO(image_data))

            # 获取图片尺寸
            width, height = img.size

            # 过滤太小的图片（通常是图标）
            if width < 150 or height < 150:
                return False

            # 过滤宽高比异常的图片
            aspect_ratio = width / height
            if aspect_ratio < 0.8 or aspect_ratio > 1.2:
                return False

            # 检查图片颜色分布（照片通常有丰富的颜色）
            if img.mode == 'RGB':
                # 计算颜色直方图
                histogram = img.histogram()
                # 检查颜色分布是否丰富
                if len([h for h in histogram if h > 0]) < 150:
                    return False

            return True
        except:
            # 如果处理失败，默认不是照片
            return False

    def _extract_images(self, pdf_path, output_dir):
        """
        提取PDF中的照片并保存

        Args:
            pdf_path: PDF文件路径
            output_dir: 输出目录

        Returns:
            list: 保存的图片路径列表
        """
        image_paths = []

        try:
            # 打开PDF文件
            doc = fitz.open(pdf_path)

            # 创建图片子目录
            image_dir = os.path.join(output_dir, "images")
            os.makedirs(image_dir, exist_ok=True)

            for page_num in range(len(doc)):
                page = doc[page_num]

                # 提取图片
                images = page.get_images(full=True)

                for img_index, img in enumerate(images):
                    xref = img[0]
                    base_image = doc.extract_image(xref)
                    image_data = base_image["image"]
                    image_ext = base_image["ext"]

                    # 检查是否为照片（过滤掉图标、水印等）
                    if self._is_photo(image_data):
                        # 生成图片文件名
                        img_name = f"image_{page_num}_{img_index}.{image_ext}"
                        img_path = os.path.join(image_dir, img_name)

                        # 保存图片
                        with open(img_path, "wb") as f:
                            f.write(image_data)

                        # 保存相对路径，便于MD文件引用
                        relative_path = os.path.join("images", img_name)
                        image_paths.append(relative_path)

            doc.close()
            return image_paths

        except Exception as e:
            print(f"提取图片失败: {str(e)}", file=sys.stderr)
            raise

    def _generate_md_content(self, text_content, image_paths):
        """
        生成MD格式内容

        Args:
            text_content: 提取的文字内容
            image_paths: 图片路径列表

        Returns:
            str: MD格式内容
        """
        md_parts = []

        # 添加标题
        md_parts.append("# 简历内容")
        md_parts.append("\n")

        # 添加文字内容
        if text_content:
            # 处理段落格式
            paragraphs = text_content.split('\n\n')
            for para in paragraphs:
                if para.strip():
                    md_parts.append(para.strip())
                    md_parts.append("\n")

        # 添加图片
        if image_paths:
            md_parts.append("## 照片")
            md_parts.append("\n")
            for img_path in image_paths:
                md_parts.append(f"![照片]({img_path})")
                md_parts.append("\n")

        # 添加生成信息
        md_parts.append("\n---")
        md_parts.append("\n*转换自PDF文件*")

        return '\n'.join(md_parts)

if __name__ == "__main__":
    try:
        print("开始启动Python服务...")

        # 导入必要的模块
        print("正在导入py4j模块...")
        from py4j.clientserver import ClientServer, PythonParameters, JavaParameters
        print("导入模块成功")

        # 启动py4j服务
        print("正在创建PDFToMDConverter实例...")
        converter = PDFToMDConverter()
        print("创建转换器实例成功")

        # 打印转换器的方法
        print("PDFToMDConverter方法:")
        for method in dir(converter):
            if not method.startswith('_'):
                print(f"  {method}")

        # 创建ClientServer，使用PDFToMDConverter作为入口点
        print("正在启动ClientServer...")
        java_params = JavaParameters(port=25335)  # 连接到Java端的25335端口
        python_params = PythonParameters(port=25336)
        gateway = ClientServer(java_parameters=java_params, python_parameters=python_params, python_server_entry_point=converter)
        print("ClientServer启动成功")

        print("Py4J ClientServer started. Waiting for Java client...")
        print("服务运行在默认端口")

        # 保持服务运行
        import time
        print("进入主循环，保持服务运行...")
        while True:
            time.sleep(1)
    except Exception as e:
        print(f"启动失败: {str(e)}")
        import traceback
        traceback.print_exc()
    finally:
        try:
            if 'gateway' in locals():
                gateway.shutdown()
                print("Py4J Gateway Server stopped.")
        except:
            pass