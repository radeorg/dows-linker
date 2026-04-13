package org.dows.linker.config;

import org.dows.linker.Entrypoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import py4j.ClientServer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//@RequiredArgsConstructor
@Configuration
@EnableConfigurationProperties(LinkerProperties.class)
public class Py4jClientConfig {


    //@Autowired
    private String embeddedPythonPath;

    private final LinkerProperties linkerProperties;

    private final Map<Class<?>, Object> endpoints = new ConcurrentHashMap<>();

    public Py4jClientConfig(LinkerProperties linkerProperties) {
        this.linkerProperties = linkerProperties;
        ClientServer clientServer = clientServer();

        List<Class<? extends Entrypoint>> entrypoints1 = linkerProperties.getEntrypoints();
        List<Entrypoint> entrypoints = (List<Entrypoint>) entrypoints1;
        for (Entrypoint ep : entrypoints) {
            Entrypoint entrypoint = (Entrypoint) clientServer.getPythonServerEntryPoint(new Class[]{ep.getClass()});
            entrypoint.set_thread_pool_size(threadPoolSize);
            // 初始化所有需要的端点
            endpoints.put(ep.getClass(), entrypoint);
        }

    }

    @Bean
    public ClientServer clientServer() {
        // 启动Python服务
        //startPythonService();
        // 创建ClientServer，用于Java调用Python
        int javaPort = linkerProperties.getJavaPort();
        int pythonPort = linkerProperties.getPythonPort();
        return new ClientServer.ClientServerBuilder()
                .javaPort(javaPort)
                .pythonPort(pythonPort)
                .build();
    }

    private void startPythonService() throws IOException {
        // 构建Python命令
        Path scriptPath = Paths.get("pdf_to_md.py");
        String[] command = {
                embeddedPythonPath,
                scriptPath.toString()
        };

        // 启动Python服务
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.inheritIO();
        processBuilder.start();

        // 等待Python服务启动
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}