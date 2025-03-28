package pl.fhframework.dp.commons.services.auditlog;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.fhframework.dp.commons.base.exception.AppException;
import pl.fhframework.dp.commons.model.entities.AuditLogIndexingQueue;
import pl.fhframework.dp.commons.model.repositories.AuditLogIndexingQueueJPARepository;
import pl.fhframework.dp.commons.utils.conversion.BeanConversionUtil;
import pl.fhframework.dp.transport.auditlog.AuditLogDto;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;


@Service
@Slf4j
public class AuditLogDao implements IAuditLogDao {
    private static final NumberFormat numberFormat = new DecimalFormat("\t#.#### [s]");

    @Autowired
    private AuditLogIndexingQueueJPARepository auditLogIndexingQueueRepository;
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Getter
    @Value("${elasticSearch.indexNamePrefix:}")
    private String indexNamePrefix;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String persistDto(AuditLogDto auditLogDto) {
        AuditLogIndexingQueue entity = BeanConversionUtil.mapObject(auditLogDto, false, AuditLogIndexingQueue.class);
        if(entity == null) {
            throw new AppException("Can not convert audit log to entity! AuditLogDto: " + BeanConversionUtil.toPrettyJson(auditLogDto));
        }
        if(entity.getId() == null) {
            entity.setId(UUID.randomUUID().toString());
        }
        AuditLogIndexingQueue saved = auditLogIndexingQueueRepository.save(entity);
        return saved.getId();
    }

    @Override
    @Transactional
    public void indexData() {
        long millis = System.currentTimeMillis();
        try {
            LocalDateTime now = LocalDateTime.now();
            Map<String, List<IndexQuery>> queriesMap = new HashMap<>();
            Pageable pageable = PageRequest.of(0, 300);
            Page<AuditLogIndexingQueue> page = auditLogIndexingQueueRepository.findByIndexed(false, pageable);
            page.getContent().forEach(entity -> {
                AuditLogDto dto = BeanConversionUtil.mapObject(entity, false, AuditLogDto.class);
                if (dto == null) {
                    throw new AppException("Can not convert metadata for AuditLogIndexingQueue entity : " + entity.getId());
                }
                addIndexData(dto, queriesMap);
                entity.setIndexed(true);
                Long lag = ChronoUnit.MILLIS.between(entity.getEventTime(), now);
                entity.setIndexingLag(lag);
                auditLogIndexingQueueRepository.save(entity);
            });
            queriesMap.keySet().forEach(key -> {
                List<IndexQuery> queries = queriesMap.get(key);
                if (!queries.isEmpty()) {
                    elasticsearchOperations.bulkIndex(queries, IndexCoordinates.of(key));
                }
            });
        } finally {
            millis = System.currentTimeMillis() - millis;
            if(millis > 1000) {
                log.warn("******* Long execution! indexData for AuditLog executed in {}",
                        numberFormat.format((float)millis/1000));
            } else	if (log.isInfoEnabled()) {
                log.debug("indexData for AuditLog executed in {}",
                        numberFormat.format((float)millis/1000));
            }
        }
    }


    private void addIndexData(AuditLogDto dto, Map<String, List<IndexQuery>> queriesMap) {
        String month = String.format("%02d", dto.getEventTime().getMonthValue());
        String indexName = indexNamePrefix + "_audit_log_" + dto.getEventTime().getYear() + "_" + month;
        IndexQuery query = new IndexQuery();
        query.setId(dto.getId());
        query.setObject(dto);
        queriesMap.computeIfAbsent(indexName, k -> new ArrayList<>()).add(query);
    }

    @Override
    public AuditLogDto getDto(String key) {
        AuditLogIndexingQueue entity = auditLogIndexingQueueRepository.findById(key).orElse(null);
        return entity == null? null: BeanConversionUtil.mapObject(entity, false, AuditLogDto.class);
    }

    @Override
    @Transactional
    public void removeIndexedEntries() {
        log.info("Removing indexed entries...");
        auditLogIndexingQueueRepository.deleteByIndexedTrue();
    }
}
