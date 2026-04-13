package org.dows.linker.python;

import lombok.Data;

import java.util.List;

@Data
public class PythonLinker {
    private String clientName;
    private int javaPort;
    private int pythonPort;

    private int threadPoolSize;

    private List<Class<? extends Entrypoint>> entrypoints;
}
