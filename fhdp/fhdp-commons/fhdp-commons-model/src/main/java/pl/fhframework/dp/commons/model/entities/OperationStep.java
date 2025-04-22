package pl.fhframework.dp.commons.model.entities;

import lombok.Getter;
import lombok.Setter;
import pl.fhframework.dp.commons.base.model.IPersistentObject;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fhdp_operation_steps",
       indexes = {@Index(name = "idx_docID",  columnList="doc_id", unique = false),
    		   @Index(name = "idx_operationGUID",  columnList="operation_guid", unique = false)})
@Getter @Setter
public class OperationStep implements Comparable<OperationStep>, IPersistentObject<String> {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "id", nullable = false, length = 255)
    private String id;
    private String description;
    private LocalDateTime started;
    private LocalDateTime finished;
    @Column(name = "doc_id")
    private Long docID;
    @Column(name = "operation_guid")
    private String operationGUID;
    @Column(name = "master_process_id")
    private String masterProcessId;
    @Column(name = "process_id")
    private String processId;
    @Column(name = "step_id")
    private String stepId;
    private float duration;

    @Override
    public int compareTo(OperationStep o) {
        if(this.started.equals(o.started)) return 0;
        return (this.started.isAfter(o.started))?-1:1;
    }
}
