package org.reactome.server.tools.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.Collection;

@Data
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ReportsSubmission {
    private Collection<CurationReport> reports;
    private Collection<String> deleteAll;
    @Builder.Default
    private String resourceId = "reactome";
}
