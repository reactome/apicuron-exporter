package org.reactome.server.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.martiansoftware.jsap.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClients;
import org.reactome.server.graph.exception.CustomQueryException;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.model.apicuron.CurationReport;
import org.reactome.server.tools.model.apicuron.ReportsSubmission;
import org.reactome.server.tools.model.input.ReactomeCurators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
    public static final HttpClient client = HttpClients.createDefault();
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
        HttpResponse response = submitReports(output, key, url);

        if (response.getStatusLine().getStatusCode() == 201) {
            log.info("APICURON accepted bulk request");
            System.exit(0);
        } else {
            log.error("APICURON rejected bulk request ==> " + response.getStatusLine().getStatusCode());
            log.error("APICURON rejected bulk request ==> " + response.getStatusLine().getReasonPhrase());
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

    public static String getApiKey() throws FileNotFoundException {
        return new Scanner(new File(Objects.requireNonNull(Main.class.getResource("/apicuron.key")).getFile())).next();
    }

    public static List<String> extractWhitelistedOrcid() throws IOException {
        URL url = Main.class.getResource("/curators.json");
        return mapper.readValue(url, ReactomeCurators.class)
                .getCurrent().stream()
                .map(ReactomeCurators.Curator::getOrcid)
                .collect(Collectors.toList());
    }

    public static void initializeNeo4j(JSAPResult config) {
        ReactomeGraphCore.initialise(config.getString(HOST), config.getString(USER), config.getString(PASSWORD), config.getString(NAME));
        advanced = ReactomeGraphCore.getService(AdvancedDatabaseObjectService.class);
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

    public static HttpResponse submitReports(File output, String key, String url) throws IOException {

        HttpPost post = new HttpPost(url);

        post.setHeader("version", "2");
        post.setHeader("authorization", "bearer " + key);

        HttpEntity entity = MultipartEntityBuilder.create()
                .setCharset(StandardCharsets.UTF_8)
                .addPart("reports", new FileBody(output, ContentType.APPLICATION_JSON))
                .addPart("delete_all[]", new StringBody("reactome", ContentType.TEXT_PLAIN))
                .build();

        post.setEntity(entity);

        return client.execute(post);
    }
}
