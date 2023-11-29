package org.reactome.server.tools;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactome.server.graph.exception.CustomQueryException;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.model.CurationReport;

import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ExporterTest {

    public static AdvancedDatabaseObjectService advanced;
    public Collection<CurationReport> reports;


    @BeforeAll
    public static void beforeAll() {
        ReactomeGraphCore.initialise(System.getProperty("neo4j.uri"), System.getProperty("neo4j.user"), System.getProperty("neo4j.password"), System.getProperty("neo4j.database"));
        advanced = ReactomeGraphCore.getService(AdvancedDatabaseObjectService.class);
    }

    @Test
    @BeforeEach
    public void testReports() throws CustomQueryException {
        reports = advanced.getCustomQueryResults(CurationReport.class, CurationReport.QUERY);
        assertFalse(reports.isEmpty());
        Set<String> allowedTerms = Set.of("authored-pathway", "authored-reaction", "reviewed-pathway", "reviewed-reaction");

        for (CurationReport report : reports) {
            assertNotNull(report.getTimestamp());
            assertNotNull(report.getCuratorOrcid());
            assertNotNull(report.getEntityUri());
            assertTrue(allowedTerms.contains(report.getActivityTerm()), "Activity term " + report.getActivityTerm() + " not included in allowed terms " + allowedTerms);
        }
    }

    @Test
    public void testSerialization() {
        assertDoesNotThrow(() -> {
            String json = Main.formatBody(reports);
            assertFalse(json.isBlank());
        });
    }
}
