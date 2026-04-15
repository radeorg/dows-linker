package org.dows.linker.python.example;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public class PdfController {

    private final PdfService pdfService;

    public PdfController(PdfService pdfService) {
        this.pdfService = pdfService;
        System.out.println("PDFToMDController initialized");
    }

    @PostMapping("/convert")
    public ResponseEntity<String> convertPDFToMD(@RequestParam String pdfPath) {
        try {
            System.out.println("收到转换请求: " + pdfPath);
            
            // 验证PDF文件
            File pdfFile = new File(pdfPath);
            if (!pdfFile.exists()) {
                return ResponseEntity.badRequest().body("PDF文件不存在: " + pdfPath);
            }
            
            // 执行转换
            System.out.println("开始转换: " + pdfPath);
            // 使用PdfService调用Python服务
            String mdPath = pdfService.convertByFilename(pdfFile.getName());
            System.out.println("转换成功! MD文件路径: " + mdPath);
            
            return ResponseEntity.ok(mdPath);
            
        } catch (Exception e) {
            System.err.println("转换失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("转换失败: " + e.getMessage());
        }
    }

    @PostMapping("/convert-file")
    public ResponseEntity<String> convertPDFToMDFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("请上传PDF文件");
            }
            
            System.out.println("收到文件上传请求: " + file.getOriginalFilename());
            
            // 执行转换
            String mdPath = pdfService.uploadAndConvert(file);
            System.out.println("转换成功! MD文件路径: " + mdPath);
            
            return ResponseEntity.ok(mdPath);
            
        } catch (Exception e) {
            System.err.println("转换失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("转换失败: " + e.getMessage());
        }
    }

    @PostMapping("/convert-async")
    public ResponseEntity<String> convertPDFToMDAsync(@RequestParam String pdfPath) {
        try {
            // 验证PDF文件存在
            File pdfFile = new File(pdfPath);
            if (!pdfFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("PDF文件不存在: " + pdfPath);
            }

            // 执行异步转换，使用回调方式
            System.out.println("开始异步转换: " + pdfPath);
            // 使用PdfService调用Python服务
            String mdPath = pdfService.convertAsyncWithCallback(pdfFile.getAbsolutePath());
            System.out.println("异步转换成功! MD文件路径: " + mdPath);

            return ResponseEntity.ok(mdPath);

        } catch (Exception e) {
            System.err.println("异步转换失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("异步转换失败: " + e.getMessage());
        }
    }

    @PostMapping("/convert-file-async")
    public ResponseEntity<String> convertPDFToMDFileAsync(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("请上传PDF文件");
            }
            
            System.out.println("收到文件上传异步请求: " + file.getOriginalFilename());
            
            // 执行异步转换
            CompletableFuture<String> future = pdfService.uploadAndConvertAsync(file);
            String mdPath = future.join();
            System.out.println("异步转换成功! MD文件路径: " + mdPath);
            
            return ResponseEntity.ok(mdPath);
            
        } catch (Exception e) {
            System.err.println("异步转换失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("异步转换失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/convert-multiple")
    public ResponseEntity<Map<String, String>> convertMultiplePDFToMD(
            @RequestParam("directory") String directory,
            @RequestParam("files") List<String> files) {
        try {
            System.out.println("收到多个PDF文件转换请求: 目录=" + directory + ", 文件数=" + files.size());
            
            // 执行并发转换
            Map<String, String> results = pdfService.convertMultipleByFilenames(directory, files);
            System.out.println("多个PDF文件转换成功! 结果数=" + results.size());
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            System.err.println("多个PDF文件转换失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(null);
        }
    }
    
    @PostMapping("/convert-multiple-files")
    public ResponseEntity<Map<String, String>> convertMultiplePDFToMDFile(
            @RequestParam("files") MultipartFile[] files) {
        try {
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest().body(null);
            }
            
            System.out.println("收到多个文件上传请求: 文件数=" + files.length);
            
            // 执行并发转换
            Map<String, String> results = pdfService.uploadAndConvertMultiple(files);
            System.out.println("多个文件转换成功! 结果数=" + results.size());
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            System.err.println("多个文件转换失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(null);
        }
    }
}