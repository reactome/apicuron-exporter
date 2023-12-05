package org.reactome.server.tools;

import org.apache.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class SubmitterTest {

    @Autowired
    public Exporter exporter;
    @Test
    public void testSubmit() throws IOException {
        File reports = new File(Objects.requireNonNull(ExporterTest.class.getResource("/reports.json")).getFile());
        HttpResponse response = exporter.submitReports(reports);
        assertEquals(201, response.getStatusLine().getStatusCode());
    }
}
