package pl.fhframework.dp.commons.services.auditlog;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.PutTemplateRequest;
import org.springframework.stereotype.Component;
import pl.fhframework.dp.transport.auditlog.AuditLogDto;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class AuditLogTemplateInitializer {
    private static final String TEMPLATE_NAME = "_audit_log";
    private static final String TEMPLATE_PATTERN = "_audit_log_*";

    @Getter
    @Value("${elasticSearch.indexNamePrefix:}")
    private String indexNamePrefix;

    @Autowired
    private ElasticsearchOperations operations;

    @PostConstruct
    public void setup() {
        IndexOperations indexOps =  operations.indexOps(AuditLogDto.class);
        if (!indexOps.existsTemplate(indexNamePrefix + TEMPLATE_NAME)) {
            if(indexOps.exists()) {
                log.info("***** *** Deleting obsolete index {}", indexOps.getIndexCoordinates().getIndexName());
                indexOps.delete();
            }
            Document mapping = indexOps.createMapping();
            AliasActions aliasActions = new AliasActions().add(
                    new AliasAction.Add(AliasActionParameters.builderForTemplate()
                            .withAliases(indexOps.getIndexCoordinates().getIndexNames())
                            .build())
            );
            InputStream is = AuditLogTemplateInitializer.class.getResourceAsStream("/settings/settings_audit_log.json");
            Map<String, Object> settings = null;
            try {
                settings = new ObjectMapper().readValue(is, HashMap.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            PutTemplateRequest request = PutTemplateRequest.builder(indexNamePrefix + TEMPLATE_NAME, indexNamePrefix + TEMPLATE_PATTERN)
                    .withMappings(mapping)
                    .withSettings(settings)
                    .withAliasActions(aliasActions)
                    .build();
            indexOps.putTemplate(request);
        }
    }
}
