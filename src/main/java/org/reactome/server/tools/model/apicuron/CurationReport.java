package org.reactome.server.tools.model.apicuron;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.neo4j.driver.Record;
import org.reactome.server.graph.domain.result.CustomQuery;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@NoArgsConstructor
public class CurationReport implements CustomQuery {
    //2013-07-17 01:35:07
    public final static DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // language=cypher
    public final static String QUERY = " " +
            // Creation
            "MATCH (per:Person)-[:author]->(ie:InstanceEdit)-[r:authored|reviewed]->(e:Event) " +
            "WHERE per.orcidId IN $whitelist " +
            "RETURN per.orcidId AS orcid, ie.dateTime AS time, type(r) AS activity, e.stId AS stId, e:Pathway AS isPathway " +

            "UNION " +

            // Deletion
            "MATCH (per:Person)-[:author]->(ie:InstanceEdit)-[c:created]->(d:Deleted)-[:deletedInstance]->(di:DeletedInstance) " +
            "WHERE  " +
            "    per.orcidId IN $whitelist AND  " +
            "    di.clazz in ['Pathway', 'TopLevelPathway', 'CellLineagePath', 'Reaction', 'BlackBoxEvent', 'Depolymerisation', 'FailedReaction', 'Polymerisation'] " +
            "RETURN per.orcidId AS orcid, ie.dateTime AS time, 'deleted' as activity, di.deletedStId AS stId, di.clazz in ['Pathway', 'TopLevelPathway', 'CellLineagePath'] AS isPathway";

    private String timestamp;
    private String activityTerm;
    private String curatorOrcid;
    private String entityUri;

    @Override
    public CurationReport build(Record record) {
        CurationReport report = new CurationReport();

        report.setCuratorOrcid(record.get("orcid").asString());

        report.setActivityTerm(record.get("activity").asString() + "-" + (record.get("isPathway").asBoolean() ? "pathway" : "reaction"));

        report.setTimestamp(LocalDateTime.parse(record.get("time").asString(), inputFormat).toString());

        report.setEntityUri("https://reactome.org/content/detail/" + record.get("stId").asString());

        return report;
    }
}