package org.dows.linker.python;

import org.dows.linker.config.LinkerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import py4j.ClientServer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//@RequiredArgsConstructor
@Configuration
@EnableConfigurationProperties(LinkerProperties.class)
public class Py4jClientConfig {

    //@Autowired
    private String embeddedPythonPath;

    public Py4jClientConfig(LinkerProperties linkerProperties) {
        List<PythonLinker> pythons = linkerProperties.getPython();
        for (PythonLinker pythonLinker : pythons) {
            int javaPort = pythonLinker.getJavaPort();
            int pythonPort = pythonLinker.getPythonPort();
            String clientName = pythonLinker.getClientName();
            ClientServer clientServer = new ClientServer.ClientServerBuilder()
                    .javaPort(javaPort)
                    .pythonPort(pythonPort)
                    .build();

            List<Class<? extends Entrypoint>> entrypoints = pythonLinker.getEntrypoints();
            Map<Class<? extends Entrypoint>, Object> endpoints = new HashMap<>();
            for (Class<? extends Entrypoint> epc : entrypoints) {
                Entrypoint entrypoint = (Entrypoint) clientServer.getPythonServerEntryPoint(new Class[]{epc});
                entrypoint.set_thread_pool_size(pythonLinker.getThreadPoolSize());
                // 初始化所有需要的端点
                endpoints.put(epc, entrypoint);
            }
            PythonHolder.store(clientName,clientServer,endpoints);
        }


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