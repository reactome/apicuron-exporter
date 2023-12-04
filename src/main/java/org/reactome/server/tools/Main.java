package org.reactome.server.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.martiansoftware.jsap.*;
import org.reactome.server.graph.exception.CustomQueryException;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.model.apicuron.CurationReport;
import org.reactome.server.tools.model.input.ReactomeCurators;
import org.reactome.server.tools.model.apicuron.ReportsSubmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;


public class Main {
    public static final String HOST = "host";
    public static final String NAME = "name";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String OUTPUT = "output";
    public static final String VERBOSE = "verbose";
    public static final String PROD = "prod";


    public static final String SERVER = "https://apicuron.org";
    public static final String DEV_SERVER = "https://dev.apicuron.org";

    public static final ObjectMapper mapper = new ObjectMapper();
    public static final HttpClient client = HttpClient.newHttpClient();
    public static final Logger log = LoggerFactory.getLogger("apicuron");
    public static AdvancedDatabaseObjectService advanced;

    public static void main(String[] args) throws JSAPException, CustomQueryException, IOException, InterruptedException {

        final JSAPResult config = defineParameters(args);

        String key = getApiKey();

        log.info("Determine whitelist from resource file curators.json");
        List<String> whitelist = extractWhitelistedOrcid();

        log.info("Initialize neo4j");
        initializeNeo4j(config);

        log.info("Query curation reports from neo4j");
        Collection<CurationReport> reports = queryCurationReports(whitelist);

        log.info("Serialize curation reports to required format");

        File output = new File(config.getString(OUTPUT));
        writeReports(reports, output);

        String server = config.getBoolean(PROD) ? SERVER : DEV_SERVER;
        String url = server + "/api/reports/bulk";


        log.info("Sending POST request to {}", url);
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofFile(output.toPath()))
                        .header("version", "2")
                        .header("Content-Type", "multipart/form-data")
                        .header("authorization", "bearer " + key)
                        .uri(URI.create(url))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() == 201) {
            log.info("APICURON accepted bulk request");
            System.exit(0);
        } else {
            log.error("APICURON rejected bulk request ==> " + response.statusCode());
            log.error("APICURON rejected bulk request ==> " + response.body());
            System.exit(1);
        }
    }

    private static JSAPResult defineParameters(String[] args) throws JSAPException {
        final Parameter[] parameters = {
                new FlaggedOption(HOST, JSAP.STRING_PARSER, "bolt://localhost:7687", JSAP.NOT_REQUIRED, 'h', HOST, "The neo4j host"),
                new FlaggedOption(NAME, JSAP.STRING_PARSER, "graph.db", JSAP.NOT_REQUIRED, 'n', NAME, "The neo4j database name"),
                new FlaggedOption(USER, JSAP.STRING_PARSER, "neo4j", JSAP.NOT_REQUIRED, 'u', USER, "The neo4j user"),
                new FlaggedOption(PASSWORD, JSAP.STRING_PARSER, "neo4j", JSAP.REQUIRED, 'p', PASSWORD, "The neo4j password"),
                new FlaggedOption(OUTPUT, JSAP.STRING_PARSER, "target/reports.json", JSAP.NOT_REQUIRED, 'o', OUTPUT, "The path to the generated reports.json"),
                new QualifiedSwitch(PROD, JSAP.BOOLEAN_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'r', PROD, "Submit request to prod APICURON server"),
                new QualifiedSwitch(VERBOSE, JSAP.BOOLEAN_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'v', VERBOSE, "Requests verbose output")
        };
        final SimpleJSAP jsap = new SimpleJSAP("Reactome APICURON exporter", "Exports curation efforts to APICURON database (https://apicuron.org/)",
                parameters);
        final JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()) System.exit(1);
        return config;
    }

    private static String getApiKey() throws FileNotFoundException {
        String key = new Scanner(new File(Objects.requireNonNull(Main.class.getResource("/apicuron.key")).getFile())).next();
        return key;
    }

    public static Collection<CurationReport> queryCurationReports(List<String> whitelist) throws CustomQueryException {
        return advanced.getCustomQueryResults(CurationReport.class, CurationReport.QUERY, Map.of("whitelist", whitelist));
    }

    public static void writeReports(Collection<CurationReport> reports, File output) throws IOException {
        ReportsSubmission reportsSubmission = ReportsSubmission.builder()
                .reports(reports)
                .deleteAll(List.of("reactome"))
                .build();

        mapper.writeValue(output, reportsSubmission);
    }

    public static void initializeNeo4j(JSAPResult config) {
        ReactomeGraphCore.initialise(config.getString(HOST), config.getString(USER), config.getString(PASSWORD), config.getString(NAME));
        advanced = ReactomeGraphCore.getService(AdvancedDatabaseObjectService.class);
    }
}
