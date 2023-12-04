package org.reactome.server.tools;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.reactome.server.graph.exception.CustomQueryException;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.model.apicuron.CurationReport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ExporterTest {

    public static AdvancedDatabaseObjectService advanced;
    public static Collection<CurationReport> reports;
    public static Set<String> whitelist = Set.of();


    @BeforeAll
    public static void beforeAll() throws IOException, CustomQueryException {
        ReactomeGraphCore.initialise(System.getProperty("neo4j.uri"), System.getProperty("neo4j.user"), System.getProperty("neo4j.password"), System.getProperty("neo4j.database"));
        advanced = ReactomeGraphCore.getService(AdvancedDatabaseObjectService.class);
        whitelist = new HashSet<>(Main.extractWhitelistedOrcid());
        reports = advanced.getCustomQueryResults(CurationReport.class, CurationReport.QUERY, Map.of("whitelist", whitelist));
    }

    @Test
    @Order(1)
    public void testWhitelist() {
        assertFalse(whitelist.isEmpty());
    }

    @Test
    @Order(2)
    public void testReports() {
        assertFalse(reports.isEmpty());
        Set<String> allowedTerms = Set.of("authored-pathway", "authored-reaction", "reviewed-pathway", "reviewed-reaction");

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
            File reports = File.createTempFile("report", ".json");
            Main.writeReports(ExporterTest.reports, reports);
            ;
        });
    }
}
