package pl.fhframework.dp.commons.model.entities;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;
import pl.fhframework.dp.commons.base.model.IPersistentObject;
import pl.fhframework.dp.transport.auditlog.AuditLogTypeEnum;
import pl.fhframework.dp.transport.converters.CustomZonedDateTimeConverter;
import pl.fhframework.dp.transport.dto.document.SeverityEnum;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fhdp_audit_log_indexing_queue",
        indexes = {@Index(name = "idx_audit_log_indexing_queue_indexed",  columnList="indexed", unique = false)})
@Data
@NoArgsConstructor
public class AuditLogIndexingQueue implements Comparable<AuditLogIndexingQueue>, IPersistentObject<String> {
    @Id
    private String id;
    private AuditLogTypeEnum type;
    private SeverityEnum severity;
    private String category;
    private LocalDateTime eventTime;
    private LocalDateTime endTime;
    private String messageKey;
    private String comment;
    private String stepID;
    private String processID;
    private String operationGUID;
    private String userLogin;
    private Long docId;
    // Exclusive for entity
    private boolean indexed = false;
    private Long indexingLag;


    @Override
    public int compareTo(AuditLogIndexingQueue o) {
        if(this.eventTime.equals(o.eventTime)) return 0;
        return (this.eventTime.isAfter(o.eventTime))?-1:1;
    }

}
