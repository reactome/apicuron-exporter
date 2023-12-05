package org.reactome.server.tools.config;

import org.reactome.server.graph.exception.CustomQueryException;
import org.reactome.server.tools.Exporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

@Profile("!test")
@Configuration
public class RunnerConfig implements CommandLineRunner {

    @Autowired
    private Exporter exporter;

    @Override
    public void run(String... args) throws CustomQueryException, IOException {
        exporter.run();
    }
}
