package org.reactome.server.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.reactome.server.tools.config.ApicuronConfig;
import org.reactome.server.tools.model.apicuron.CurationReport;
import org.reactome.server.tools.model.apicuron.ReportsSubmission;
import org.reactome.server.tools.model.curator.ReactomeCurators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class Exporter {

    @Autowired
    private AdvancedDatabaseObjectService advanced;
    @Autowired
    private ApicuronConfig config;

    @Value("classpath:curators.json")
    private Resource curators;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClients.createDefault();
    private final Logger log = LoggerFactory.getLogger("apicuron");


    public void run() throws IOException, CustomQueryException {

        log.info("Determine whitelist from resource file curators.json");
        List<String> whitelist = extractWhitelistedOrcid();

        log.info("Query curation reports from neo4j");
        Collection<CurationReport> reports = queryCurationReports(whitelist);

        log.info("Serialize curation reports to required format");

        File output = new File(config.getOutput());
        writeReports(reports, output);

        HttpResponse response = submitReports(output);

        if (response.getStatusLine().getStatusCode() == 201) {
            log.info("APICURON accepted bulk request");
            System.exit(0);
        } else {
            log.error("APICURON rejected bulk request ==> " + response.getStatusLine().getStatusCode());
            log.error("APICURON rejected bulk request ==> " + response.getStatusLine().getReasonPhrase());
            System.exit(1);
        }
    }

    public List<String> extractWhitelistedOrcid() throws IOException {
        return mapper.readValue(curators.getInputStream(), ReactomeCurators.class)
                .getAccepted().stream()
                .map(ReactomeCurators.Curator::getOrcid)
                .collect(Collectors.toList());
    }

    public Collection<CurationReport> queryCurationReports(List<String> whitelist) throws CustomQueryException {
        return advanced.getCustomQueryResults(CurationReport.class, CurationReport.QUERY, Map.of("whitelist", whitelist));
    }

    public void writeReports(Collection<CurationReport> reports, File output) throws IOException {
        ReportsSubmission reportsSubmission = ReportsSubmission.builder()
                .reports(reports)
                .deleteAll(List.of("reactome"))
                .build();

        mapper.writeValue(output, reportsSubmission);
    }

    public HttpResponse submitReports(File output) throws IOException {

        String url = config.getServer() + "/api/reports/bulk";
        log.info("Sending POST request to {}", url);

        HttpPost post = new HttpPost(url);

        post.setHeader("version", "2");
        post.setHeader("authorization", "bearer " + config.getKey());

        HttpEntity entity = MultipartEntityBuilder.create()
                .setCharset(StandardCharsets.UTF_8)
                .addPart("reports", new FileBody(output, ContentType.APPLICATION_JSON))
                .addPart("delete_all[]", new StringBody("reactome", ContentType.TEXT_PLAIN))
                .build();

        post.setEntity(entity);

        return client.execute(post);
    }
}
