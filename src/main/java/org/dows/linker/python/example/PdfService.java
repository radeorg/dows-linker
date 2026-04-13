package org.dows.linker.python.example;

import org.dows.linker.python.PythonHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@Service
public class PdfService {
    private final String PDF_DIR = System.getProperty("user.dir") + File.separator + "pdf";
    private final String RESULT_DIR = System.getProperty("user.dir") + File.separator + "result";


    /**
     * 上传PDF文件并同步转换为MD
     *
     * @param file 上传的PDF文件
     * @return MD文件路径
     * @throws Exception 转换异常
     */
    public String uploadAndConvert(MultipartFile file) throws Exception {
        // 创建目录
        Files.createDirectories(Path.of(PDF_DIR));
        Files.createDirectories(Path.of(RESULT_DIR));

        String filename = file.getOriginalFilename();
        Path savePath = Path.of(PDF_DIR, filename);

        // 保存文件
        file.transferTo(savePath.toFile());

        // 调用Python脚本，使用回调方式
        return convertAsyncWithCallback(savePath.toString());
    }

    /**
     * 根据文件名同步转换PDF为MD
     *
     * @param filename PDF文件名
     * @return MD文件路径
     */
    public String convertByFilename(String filename) {
        try {
            String pdfPath = PDF_DIR + File.separator + filename;
            return convertAsyncWithCallback(pdfPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 上传PDF文件并异步转换为MD
     *
     * @param file 上传的PDF文件
     * @return 包含MD文件路径的CompletableFuture
     */
    public CompletableFuture<String> uploadAndConvertAsync(MultipartFile file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 创建目录
                Files.createDirectories(Path.of(PDF_DIR));
                Files.createDirectories(Path.of(RESULT_DIR));

                String filename = file.getOriginalFilename();
                Path savePath = Path.of(PDF_DIR, filename);

                // 保存文件
                file.transferTo(savePath.toFile());

                // 调用Python脚本，使用回调方式
                return convertAsyncWithCallback(savePath.toString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 同步转换PDF为MD
     *
     * @param pdfPath PDF文件路径
     * @return MD文件路径
     */
    public String convert(String pdfPath) {
        try {
            PdfProcessEntrypoint converter = PythonHolder.getEntrypoint("python312", PdfProcessEntrypoint.class);
            // 调用Python端的convert_pdf_to_md方法
            return converter.convert_pdf_to_md(pdfPath, RESULT_DIR);
        } catch (Exception e) {
            throw new RuntimeException("PDF转换失败: " + e.getMessage(), e);
        }
    }

    /**
     * 异步转换PDF为MD，使用回调方式
     *
     * @param pdfPath PDF文件路径
     * @return MD文件路径
     */
    public String convertAsyncWithCallback(String pdfPath) {
        try {
            // 创建回调对象
            PdfProcessCallback callback = new PdfProcessCallback();

            PdfProcessEntrypoint converter = PythonHolder.getEntrypoint("python312", PdfProcessEntrypoint.class);

            // 调用Python端的convert_pdf_to_md_async方法
            converter.convert_pdf_to_md_async(pdfPath, RESULT_DIR, callback);

            // 等待回调完成
            if (callback.isSuccess()) {
                return callback.getResult();
            } else {
                throw new RuntimeException("PDF转换失败: " + callback.getError());
            }
        } catch (Exception e) {
            throw new RuntimeException("PDF转换失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据文件路径转换PDF为MD
     *
     * @param pdfPath PDF文件路径
     * @return MD文件路径
     * @throws Exception 转换异常
     */
    public String convertByPath(String pdfPath) throws Exception {
        // 构建Python命令
        String[] command = {
                "python",
                "pdf_process.py",
                pdfPath,
                RESULT_DIR
        };

        // 执行Python脚本
        Process process = Runtime.getRuntime().exec(command);

        // 读取输出
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        // 读取错误输出
        StringBuilder errorOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
        }

        // 等待进程完成
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Python脚本执行失败: " + errorOutput.toString());
        }

        // 返回MD文件路径
        return output.toString().trim();
    }

//    /**
//     * 并发转换多个PDF文件为MD
//     *
//     * @param directory PDF文件目录
//     * @param files     PDF文件名列表
//     * @return 文件名到MD文件路径的映射
//     */
//    public Map<String, String> convertMultipleByFilenames(String directory, List<String> files) {
//        // 创建线程池
//        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
//
//        // 存储转换结果
//        Map<String, String> results = new ConcurrentHashMap<>();
//
//        try {
//            // 提交所有转换任务
//            List<CompletableFuture<Void>> futures = files.stream()
//                    .map(file -> CompletableFuture.runAsync(() -> {
//                        try {
//                            String pdfPath = directory + File.separator + file;
//                            String mdPath = convertAsyncWithCallback(pdfPath);
//                            results.put(file, mdPath);
//                        } catch (Exception e) {
//                            results.put(file, "转换失败: " + e.getMessage());
//                        }
//                    }, executorService))
//                    .toList();
//
//            // 等待所有任务完成
//            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//
//            return results;
//        } finally {
//            // 关闭线程池
//            executorService.shutdown();
//        }
//    }
//
//    /**
//     * 上传多个PDF文件并并发转换为MD
//     *
//     * @param files 上传的PDF文件数组
//     * @return 文件名到MD文件路径的映射
//     */
//    public Map<String, String> uploadAndConvertMultiple(MultipartFile[] files) {
//        // 创建目录
//        try {
//            Files.createDirectories(Path.of(PDF_DIR));
//            Files.createDirectories(Path.of(RESULT_DIR));
//        } catch (IOException e) {
//            throw new RuntimeException("创建目录失败: " + e.getMessage(), e);
//        }
//
//        // 创建线程池
//        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
//
//        // 存储转换结果
//        Map<String, String> results = new ConcurrentHashMap<>();
//
//        try {
//            // 提交所有转换任务
//            List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();
//            for (MultipartFile file : files) {
//                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
//                    try {
//                        String filename = file.getOriginalFilename();
//                        Path savePath = Path.of(PDF_DIR, filename);
//
//                        // 保存文件
//                        file.transferTo(savePath.toFile());
//
//                        // 调用Python脚本，使用回调方式
//                        String mdPath = convertAsyncWithCallback(savePath.toString());
//                        results.put(filename, mdPath);
//                    } catch (Exception e) {
//                        results.put(file.getOriginalFilename(), "转换失败: " + e.getMessage());
//                    }
//                }, executorService);
//                futures.add(future);
//            }
//
//            // 等待所有任务完成
//            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//
//            return results;
//        } finally {
//            // 关闭线程池
//            executorService.shutdown();
//        }
//    }
}