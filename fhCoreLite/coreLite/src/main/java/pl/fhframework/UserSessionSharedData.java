package pl.fhframework;

import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.Resource;
import pl.fhframework.io.TemporaryResource;
import pl.fhframework.model.security.SystemUser;

import javax.servlet.http.HttpSession;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Container for data shared between user sessions in the same http session
 */
@Getter
public class UserSessionSharedData {
    private final Map<String, TemporaryResource> uploadFileIndexes = new ConcurrentHashMap<>();
    private final Map<String, Resource> downloadFileIndexes = new ConcurrentHashMap<>();
    private final Set<UserSession> conversations = ConcurrentHashMap.newKeySet();
    private SystemUser systemUser;
    private HttpSession httpSession;

    @Setter
    private Instant activityLimitDate;

    public UserSessionSharedData(HttpSession httpSession, SystemUser systemUser) {
        this.systemUser = systemUser;
        this.httpSession = httpSession;
    }

    // I18n
    @Setter
    private Locale language;

    public void addConversation(UserSession newConversation) {
        conversations.add(newConversation);
    }

    public void removeConversation(UserSession conversation) {
        if (conversation.isClosed()){
        conversations.remove(conversation);
        } else {
            throw new IllegalStateException("Conversation is not closed and cannot be removed");
        }
    }

    public String getHttpSessionId(){
        return httpSession.getId();
    }

    public Set<UserSession> getConversations(){
        return Collections.unmodifiableSet(this.conversations);
    }

    public void changeHttpSession(HttpSession httpSession) {
        this.httpSession = httpSession;
    }
}
