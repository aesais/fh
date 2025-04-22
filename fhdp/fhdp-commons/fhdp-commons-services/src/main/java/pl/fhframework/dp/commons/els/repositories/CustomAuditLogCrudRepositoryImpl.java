package pl.fhframework.dp.commons.els.repositories;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Repository;
import pl.fhframework.dp.transport.auditlog.AuditLogDto;

@Repository
public class CustomAuditLogCrudRepositoryImpl implements CustomAuditLogCrudRepository<AuditLogDto> {

    @Getter
    @Value("${elasticSearch.indexNamePrefix:}")
    private String indexNamePrefix;

    @Autowired
    private ElasticsearchOperations operations;

    @Override
    public <S extends AuditLogDto> S save(S entity) {
        return operations.save(entity, indexName(entity));
    }

    @Override
    public <S extends AuditLogDto> Iterable<S> saveAll(Iterable<S> entities) {
        throw new UnsupportedOperationException("Not implemented!");
    }


    public IndexCoordinates indexName(AuditLogDto entity) {
        String month = String.format("%02d", entity.getEventTime().getMonthValue());
        String indexName = indexNamePrefix + "_audit_log_" + entity.getEventTime().getYear() + "_" + month;
        return IndexCoordinates.of(indexName);
    }

}
