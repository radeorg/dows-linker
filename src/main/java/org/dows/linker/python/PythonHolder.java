package org.dows.linker.python;

import lombok.Data;
import py4j.ClientServer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class PythonHolder {

    private static Map<String, PythonClientServer> clients = new ConcurrentHashMap<>();

    public static void store(String clientName, ClientServer clientServer, Map<Class<? extends Entrypoint>, Object> endpoints) {
        PythonClientServer pythonClientServer = new PythonClientServer(clientServer, endpoints);
        clients.put(clientName, pythonClientServer);
    }


    /**
     * 获取Python端点
     * @param clientName
     * @param entrypointClass
     * @return
     */
    public static Entrypoint getEntrypoint(String clientName, Class<? extends Entrypoint> entrypointClass) {
        PythonClientServer pythonClientServer = clients.get(clientName);
        Object entrypoint = pythonClientServer.getEndpoints().get(entrypointClass);
        if (entrypoint == null) {
            throw new RuntimeException("Entrypoint not found");
        }
        return (Entrypoint) entrypoint;
    }
}
