package pl.fhframework.dp.commons.services.auditlog;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pl.fhframework.dp.commons.els.repositories.AuditLogESRepository;
import pl.fhframework.dp.commons.services.facade.GenericDtoService;
import pl.fhframework.dp.transport.auditlog.AuditLogDto;
import pl.fhframework.dp.transport.auditlog.AuditLogDtoQuery;
import pl.fhframework.dp.transport.auditlog.AuditLogTypeEnum;
import pl.fhframework.dp.transport.dto.document.SeverityEnum;
import pl.fhframework.dp.transport.service.IAuditLogDtoService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:jacek.borowiec@asseco.pl">Jacek Borowiec</a>
 * @version :  $, :  $
 * @created 15/09/2020
 */
@Service
@Slf4j
public class AuditLogDtoService extends GenericDtoService<String, AuditLogDto, AuditLogDto, AuditLogDtoQuery, AuditLogDto> implements IAuditLogDtoService {

    private final IAuditLogDao auditLogDao;
    private final AuditLogESRepository auditLogESRepository;

    public AuditLogDtoService(IAuditLogDao auditLogDao, AuditLogESRepository auditLogESRepository) {
        super(AuditLogDto.class, AuditLogDto.class, AuditLogDto.class);
        this.auditLogDao = auditLogDao;
        this.auditLogESRepository = auditLogESRepository;
    }

    @Override
    public List<AuditLogDto> listDto(AuditLogDtoQuery query) {
        if(query.getSortProperty() == null || "id".equals(query.getSortProperty())) {
            query.setSortProperty("eventTime");
        }
        return super.listDto(query);
    }

    @Override
    public String persistDto(AuditLogDto auditLogDto) {
        return auditLogDao.persistDto(auditLogDto);
    }

    @Override
    public AuditLogDto getDto(String key) {
        AuditLogDto ret = auditLogESRepository.findById(key).orElse(null);
        if(ret == null) {
            ret = auditLogDao.getDto(key);
        } else {
            ret.setOpData(auditLogDao.getOpdata(key, ret.getOpData()));
        }
        return ret;
    }

    @Scheduled(initialDelay = 60, fixedDelay = 1, timeUnit = TimeUnit.SECONDS)
    public void indexData() {
//        log.info("Start indexing auditLog...");
        auditLogDao.indexData();
    }


    @Scheduled(cron = "0 30 23 * * *")
    public void removeIndexedEntries() {
        log.info("Removing indexed entries...");
        auditLogDao.removeIndexedEntries();
    }


    @Override
    protected BoolQueryBuilder extendQueryBuilder(BoolQueryBuilder builder, AuditLogDtoQuery query) {
        if(query.getType() != null) {
            builder.must(QueryBuilders.termQuery("type.keyword", query.getType().name()));
        }
        if(query.getSeverity() != null) {
            builder.must(QueryBuilders.termQuery("severity.keyword", query.getSeverity().name()));
        }
        if(query.getCategory() != null) {
            builder.must(QueryBuilders.termQuery("category.keyword", query.getCategory()));
        }
        if(query.getMessageKey() != null) {
            builder.must(QueryBuilders.wildcardQuery("messageKey", query.getMessageKey() + "*"));
        }
        if(query.getComment() != null) {
            builder.must(QueryBuilders.wildcardQuery("comment", query.getComment() + "*"));
        }
        if(query.getProcessID() != null) {
            builder.must(QueryBuilders.termQuery("processID.keyword", query.getProcessID()));
        }
        if(query.getStepID() != null) {
            builder.must(QueryBuilders.termQuery("stepID.keyword", query.getStepID()));
        }
        if(query.getOperationGUID() != null) {
            builder.must(QueryBuilders.termQuery("operationGUID.keyword", query.getOperationGUID()));
        }
        if(query.getUserLogin() != null) {
            builder.must(QueryBuilders.wildcardQuery("userLogin", query.getUserLogin() + "*"));
        }
        if(query.getDocId() != null) {
            builder.must(QueryBuilders.termQuery("docId.keyword", query.getDocId()));
        }
        if(query.getDocType() != null) {
            builder.must(QueryBuilders.termQuery("docType.keyword", query.getDocType()));
        }
        if(query.getDocNumberLocal() != null) {
            builder.must(QueryBuilders.termQuery("docNumberLocal.keyword", query.getDocNumberLocal()));
        }
        if(query.getDocNumberFormal() != null) {
            builder.must(QueryBuilders.termQuery("docNumberFormal.keyword", query.getDocNumberFormal()));
        }
        LocalDateTime dateFrom = query.getEventTimeFrom();
        LocalDateTime dateTo = query.getEventTimeTo();
        if (dateFrom != null || dateTo != null) {
            builder.filter(QueryBuilders.rangeQuery("eventTime")
                    .from(dateFrom)
                    .to(dateTo)
                    .includeLower(dateFrom != null)
                    .includeUpper(dateTo != null));
        }
        return builder;
    }

    /**
     *
     * @param severity
     * @param category
     * @param messageKey - format: $.key\bparam1\bparam2
     * @param comment
     * @param processID
     * @param operationGUID
     * @param stepID
     * @param userLogin
     */
    public void logBusiness(SeverityEnum severity,
                            String category,
                            String messageKey,
                            String comment,
                            String processID,
                            String operationGUID,
                            String stepID,
                            String userLogin){
        AuditLogDto dto = new AuditLogDto(AuditLogTypeEnum.business,
                severity,
                category,
                LocalDateTime.now(),
                messageKey,
                comment,
                processID,
                operationGUID,
                stepID,
                userLogin);
        persistDto(dto);

    }

//    @Deprecated
//    public void logOperationStepStart(String messageKey,
//                          String processID,
//                          String operationGUID,
//                          String stepID) {
//        AuditLogDto dto = new AuditLogDto(AuditLogTypeEnum.business,
//                SeverityEnum.info,
//                "operationSteps",
//                LocalDateTime.now(),
//                messageKey,
//                "start",
//                processID,
//                operationGUID,
//                stepID,
//                "system");
//        persistDto(dto);
//    }
//
//    @Deprecated
//    public void logOperationStepFinish(String processID,
//                                      String operationGUID,
//                                      String stepID) {
//        AuditLogDtoQuery query = new AuditLogDtoQuery();
//        query.setProcessID(processID);
//        query.setOperationGUID(operationGUID);
//        query.setStepID(stepID);
//        query.setCategory("operationSteps");
//        query.setAscending(false);
//        List<AuditLogDto> auditSteps = listDto(query);
//        if(!auditSteps.isEmpty()) {
//            AuditLogDto dto = auditSteps.get(0);
//            dto.setEndTime(LocalDateTime.now());
//            persistDto(dto);
//        }
//    }

    public void logBusinessInfo(
                            String category,
                            String messageKey,
                            String comment,
                            String processID,
                            String operationGUID,
                            String stepID,
                            String userLogin){
        AuditLogDto dto = new AuditLogDto(AuditLogTypeEnum.business,
                SeverityEnum.info,
                category,
                LocalDateTime.now(),
                messageKey,
                comment,
                processID,
                operationGUID,
                stepID,
                userLogin);
        persistDto(dto);

    }

    public void logBusinessInfo(
            String category,
            String messageKey,
            String comment,
            Long docId,
            String userLogin){
        AuditLogDto dto = new AuditLogDto(AuditLogTypeEnum.business,
                SeverityEnum.info,
                category,
                LocalDateTime.now(),
                messageKey,
                comment,
                null,
                null,
                null,
                userLogin);
        dto.setDocId(docId);
        persistDto(dto);

    }

    public void logBusinessError(
            String category,
            String messageKey,
            String comment,
            String processID,
            String operationGUID,
            String stepID,
            String userLogin){
        AuditLogDto dto = new AuditLogDto(AuditLogTypeEnum.business,
                SeverityEnum.error,
                category,
                LocalDateTime.now(),
                messageKey,
                comment,
                processID,
                operationGUID,
                stepID,
                userLogin);
        persistDto(dto);
    }

    public void logBusinessError(
            String category,
            String messageKey,
            String comment,
            Long docId,
            String userLogin){
        AuditLogDto dto = new AuditLogDto(AuditLogTypeEnum.business,
                SeverityEnum.error,
                category,
                LocalDateTime.now(),
                messageKey,
                comment,
                null,
                null,
                null,
                userLogin);
        dto.setDocId(docId);
        persistDto(dto);

    }

    public void logTechnicalInfo(
            String category,
            String messageKey,
            String comment,
            String processID,
            String operationGUID,
            String stepID,
            String userLogin){
        AuditLogDto dto = new AuditLogDto(AuditLogTypeEnum.technical,
                SeverityEnum.info,
                category,
                LocalDateTime.now(),
                messageKey,
                comment,
                processID,
                operationGUID,
                stepID,
                userLogin);
        persistDto(dto);
    }

    public void logTechnicalInfo(
            String category,
            String messageKey,
            String comment,
            Long docId,
            String userLogin){
        AuditLogDto dto = new AuditLogDto(AuditLogTypeEnum.technical,
                SeverityEnum.info,
                category,
                LocalDateTime.now(),
                messageKey,
                comment,
                null,
                null,
                null,
                userLogin);
        dto.setDocId(docId);
        persistDto(dto);
    }

    public void logTechnicalError(
            String category,
            String messageKey,
            String comment,
            String processID,
            String operationGUID,
            String stepID,
            String userLogin){
        AuditLogDto dto = new AuditLogDto(AuditLogTypeEnum.technical,
                SeverityEnum.error,
                category,
                LocalDateTime.now(),
                messageKey,
                comment,
                processID,
                operationGUID,
                stepID,
                userLogin);
        persistDto(dto);
    }

    public void logTechnicalError(
            String category,
            String messageKey,
            String comment,
            Long docId,
            String userLogin){
        AuditLogDto dto = new AuditLogDto(AuditLogTypeEnum.technical,
                SeverityEnum.error,
                category,
                LocalDateTime.now(),
                messageKey,
                comment,
                null,
                null,
                null,
                userLogin);
        dto.setDocId(docId);
        persistDto(dto);
    }
}
