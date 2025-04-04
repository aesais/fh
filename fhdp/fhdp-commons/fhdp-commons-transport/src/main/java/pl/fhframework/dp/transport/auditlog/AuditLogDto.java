package pl.fhframework.dp.transport.auditlog;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import pl.fhframework.dp.commons.base.model.IPersistentObject;
import pl.fhframework.dp.transport.converters.CustomZonedDateTimeConverter;
import pl.fhframework.dp.transport.dto.document.SeverityEnum;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * @author <a href="mailto:jacek.borowiec@asseco.pl">Jacek Borowiec</a>
 * @version :  $, :  $
 * @created 15/09/2020
 */
@Document(indexName = "#{@indexNamePrefix}_audit_log", createIndex = false)
@Getter
@Setter
@NoArgsConstructor
public class AuditLogDto implements IPersistentObject<String> {
    @Id
    private String id;
    private AuditLogTypeEnum type;
    private SeverityEnum severity;
    private String category;
    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSX")
    @ValueConverter(CustomZonedDateTimeConverter.class)
    private LocalDateTime eventTime;
    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSX")
    @ValueConverter(CustomZonedDateTimeConverter.class)
    private LocalDateTime endTime;
    private String messageKey;
    private String comment;
    private String stepID;
    private String processID;
    private String operationGUID;
    private String userLogin;
    private Long docId;
    private String docType;
    private String docNumberLocal;
    private String docNumberFormal;
    private Object opData;
    private Object opResult;
    private Long duration;

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        if(endTime != null && eventTime != null) {
            setDuration(ChronoUnit.MILLIS.between(eventTime, this.endTime));
        }
    }

    public AuditLogDto(AuditLogTypeEnum type,
                       SeverityEnum severity,
                       String category,
                       LocalDateTime eventTime,
                       String messageKey,
                       String comment,
                       String processID,
                       String operationGUID,
                       String stepID,
                       String userLogin) {
        this.type = type;
        this.severity = severity;
        this.category = category;
        this.eventTime = eventTime;
        this.messageKey = messageKey;
        this.comment = comment;
        this.processID = processID;
        this.stepID = stepID;
        this.operationGUID = operationGUID;
        this.userLogin = userLogin;
    }

}
