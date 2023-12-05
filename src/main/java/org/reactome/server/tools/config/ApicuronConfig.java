package org.reactome.server.tools.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotBlank;

@NoArgsConstructor
@Getter
@Setter
@ConfigurationProperties(prefix = "apicuron")
public class ApicuronConfig {
    @NotBlank
    private String key;
    @NotBlank
    private String server;
    @NotBlank
    private String output;
}





