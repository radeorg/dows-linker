package org.dows.linker.verticle;

import io.vertx.core.Vertx;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VertxDeploy {

    private final Vertx vertx;

    private final UploadVerticle uploadVerticle;
    private final AcceptVerticle acceptVerticle;
    private final AnalysisVerticle analysisVerticle;

    @PostConstruct
    public void deploy() {
        vertx.deployVerticle(uploadVerticle);
        vertx.deployVerticle(acceptVerticle);
        vertx.deployVerticle(analysisVerticle);
    }
}
