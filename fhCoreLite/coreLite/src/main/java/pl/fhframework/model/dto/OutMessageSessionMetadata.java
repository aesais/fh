package pl.fhframework.model.dto;

import lombok.Getter;
import lombok.Setter;
import pl.fhframework.Commands;

import java.util.UUID;

/**
 * Message from a server containig user session information.
 */
@Getter
@Setter
public class OutMessageSessionMetadata extends AbstractMessage {

    private String sessionId;
    private String converstaionId = UUID.randomUUID().toString();

    public OutMessageSessionMetadata(String sessionId) {
        this();
        this.sessionId = sessionId;
    }

    public OutMessageSessionMetadata(String sessionId, String converstaionId) {
        this();
        this.sessionId = sessionId;
        this.converstaionId = converstaionId;
    }

    public OutMessageSessionMetadata() {
        super(Commands.OUT_CONNECTION_ID);
    }
}
