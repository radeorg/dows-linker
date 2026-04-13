package org.dows.linker.verticle;

import cn.hutool.json.JSONUtil;
import io.vertx.core.AbstractVerticle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dows.rade.event.TraceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RequiredArgsConstructor
@Component
@Slf4j
public class AnalysisVerticle extends AbstractVerticle {

    @Value("${setting.download.folder:data}")
    private String ATTACHMENT_SAVE_FOLDER;

    //private final LlmAnalysist llmAnalysist;

    @Override
    public void start() {
        vertx.eventBus().consumer("setting.mail.readed", message -> {
            try {
                String traceId = message.headers().get("traceId");
                TraceContext.set(traceId);
                Object body = message.body();
                analysis(body);
                log.info("body:{}", body);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                TraceContext.clear();
            }
        });
    }

    private void analysis(Object body) throws IOException {
/*        log.info("rfa analysis verticle received event body：{}", JSONUtil.toJsonStr(body));
        AttachmentSchema attachmentSchema = JSONUtil.parseObj(body).get("data", AttachmentSchema.class);
        Path path = Paths.get(ATTACHMENT_SAVE_FOLDER, attachmentSchema.getFileName() + ".txt");
        // 读取文件内容
        String content = Files.readString(path, StandardCharsets.UTF_8)
                .replaceAll("(?m)^\\s*$(\\n\\s*^\\s*$)+", "\n");
        log.info("Generated file content: {}", content);
        llmAnalysist.analysis(content);*/
    }

}
