package org.dows.linker.python.example;

import org.dows.linker.python.Entrypoint;
import org.dows.linker.python.EntrypointCallback;

public interface PdfProcessEntrypoint extends Entrypoint {

    /**
     * 将PDF转换为MD格式
     *
     * @param pdfPath   PDF文件路径
     * @param outputDir 输出目录
     * @return MD文件路径
     */
    String convert_pdf_to_md(String pdfPath, String outputDir);

    /**
     * 异步将PDF转换为MD格式
     *
     * @param pdfPath   PDF文件路径
     * @param outputDir 输出目录
     * @param callback  回调对象
     */
    void convert_pdf_to_md_async(String pdfPath, String outputDir, EntrypointCallback callback);


}
