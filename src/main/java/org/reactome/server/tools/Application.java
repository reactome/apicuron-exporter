package org.reactome.server.tools;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {"org.reactome.server"})
@EntityScan("org.reactome.server.graph.domain.model")
@ConfigurationPropertiesScan
public class Application  {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
