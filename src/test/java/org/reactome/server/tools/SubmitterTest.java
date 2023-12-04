package org.reactome.server.tools;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.http.HttpResponse;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubmitterTest {
    @Test
    public void testSubmit() {
        assertDoesNotThrow(() -> {
            File reports = new File(Objects.requireNonNull(ExporterTest.class.getResource("/reports.json")).getFile());
            HttpResponse<String> response = Main.submitReports(reports, Main.getApiKey(), Main.DEV_SERVER + "/api/reports/bulk");
            assertEquals(201, response.statusCode());
        });
    }
}
