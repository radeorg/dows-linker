package org.dows.linker;

import lombok.Data;

import java.util.List;

@Data
public class PythonEndpoint {
    private int javaPort;
    private int pythonPort;

    private int threadPoolSize;

    private List<Class<? extends Entrypoint>> entrypoints;
}
