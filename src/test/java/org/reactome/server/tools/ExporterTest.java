package org.reactome.server.tools;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.reactome.server.graph.exception.CustomQueryException;
import org.reactome.server.tools.model.apicuron.CurationReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(properties = {"spring.profiles.include=test"})
public class ExporterTest {

    @Autowired
    public Exporter exporter;

    public List<String> whitelist = List.of("0000-0002-3699-0937", "0000-0001-5193-0855", "0000-0001-5707-3065");
    Collection<CurationReport> reports;


    @Test
    @Order(1)
    public void testWhitelist() throws IOException {
        List<String> whitelist = exporter.extractWhitelistedOrcid();
        assertFalse(whitelist.isEmpty());
    }

    @Test
    @Order(2)
    public void testReports() throws CustomQueryException {
        reports = exporter.queryCurationReports(whitelist);
        assertFalse(reports.isEmpty());

        Set<String> allowedTerms = Set.of(
                "authored-pathway", "authored-reaction",
                "reviewed-pathway", "reviewed-reaction",
                "deleted-reaction", "deleted-pathway"
        );

        for (CurationReport report : reports) {
            assertNotNull(report.getTimestamp());
            assertNotNull(report.getCuratorOrcid());
            assertTrue(whitelist.contains(report.getCuratorOrcid()));
            assertNotNull(report.getEntityUri());
            assertTrue(allowedTerms.contains(report.getActivityTerm()), "Activity term " + report.getActivityTerm() + " not included in allowed terms " + allowedTerms);
        }
    }

    @Test
    @Order(3)
    public void testSerialization() {
        assertDoesNotThrow(() -> {
            File output = File.createTempFile("report", ".json");
            exporter.writeReports(reports, output);
        });
    }
}
