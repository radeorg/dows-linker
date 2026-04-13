package org.dows.linker.config;

import lombok.Data;
import org.dows.linker.Entrypoint;
import org.dows.linker.PythonEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "dows.linker")
public class LinkerProperties {


    private List<PythonEndpoint> python;

}
