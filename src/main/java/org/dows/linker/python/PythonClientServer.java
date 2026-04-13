package org.dows.linker.python;

import lombok.Data;
import py4j.ClientServer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class PythonClientServer {
    private final Map<Class<? extends Entrypoint>, Object> endpoints = new ConcurrentHashMap<>();

    private ClientServer clientServer;

    public PythonClientServer(ClientServer clientServer, Map<Class<? extends Entrypoint>, Object> endpoints) {
        this.clientServer = clientServer;
        this.endpoints.putAll(endpoints);
    }
}
