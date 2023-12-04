package org.reactome.server.tools;

import org.apache.http.HttpResponse;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubmitterTest {
    @Test
    public void testSubmit() throws IOException {
        File reports = new File(Objects.requireNonNull(ExporterTest.class.getResource("/reports.json")).getFile());
        HttpResponse response = Main.submitReports(reports, Main.getApiKey(), Main.DEV_SERVER + "/api/reports/bulk");
        assertEquals(201, response.getStatusLine().getStatusCode());
    }
}
