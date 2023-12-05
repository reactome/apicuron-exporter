package org.reactome.server.tools.model.curator;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Data
@Builder
@Jacksonized
public class ReactomeCurators {
    private List<Curator> current;
    private List<Curator> former;

    @Data
    @Builder
    @Jacksonized
    public static class Curator {
        private String name;
        private String orcid;

    }
}
