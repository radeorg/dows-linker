package org.dows.linker.python.example;

import org.dows.linker.python.EntrypointCallback;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class PdfProcessCallback implements EntrypointCallback {
    private String result;
    private String error;
    private boolean success;
    private final CountDownLatch latch;

    public PdfProcessCallback() {
        this.latch = new CountDownLatch(1);
        this.success = false;
    }

    @Override
    public void onSuccess(String mdPath) {
        this.result = mdPath;
        this.success = true;
        this.latch.countDown();
    }

    @Override
    public void onError(String error) {
        this.error = error;
        this.success = false;
        this.latch.countDown();
    }

    public String getResult() {
        try {
            // 等待回调完成，最多等待60秒
            if (latch.await(60, TimeUnit.SECONDS)) {
                return result;
            } else {
                throw new RuntimeException("PDF转换超时");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("PDF转换被中断", e);
        }
    }

    public String getError() {
        try {
            // 等待回调完成，最多等待60秒
            latch.await(60, TimeUnit.SECONDS);
            return error;
        } catch (InterruptedException e) {
            throw new RuntimeException("PDF转换被中断", e);
        }
    }

    public boolean isSuccess() {
        try {
            // 等待回调完成，最多等待60秒
            latch.await(60, TimeUnit.SECONDS);
            return success;
        } catch (InterruptedException e) {
            throw new RuntimeException("PDF转换被中断", e);
        }
    }
}
