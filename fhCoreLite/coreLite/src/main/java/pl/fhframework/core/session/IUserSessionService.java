package pl.fhframework.core.session;

import org.springframework.core.io.Resource;
import pl.fhframework.core.security.model.SessionInfo;

import java.util.List;

/**
 * Interface of a user session managing API.
 */
public interface IUserSessionService {

    /**
     * Lists user sessions
     * @return user sessions list
     */
    public List<SessionInfo> getUserSessions();

    /**
     * Forcibly logs out user session with given id
     * @param httpSessionId http session id stored in user session shared data
     * @return True, if user has been successfully logged off. False, if exception has occurred or session does not exists.
     */
    public boolean forceLogout(String httpSessionId);

    /**
     * Downloads log associated with user conversation with given id
     * @param userConversationId user conversation id
     * @return log file resource
     */
    public Resource donwloadUserLog(String userConversationId);

    /**
     * Downloads log associated with user sessionInfo
     * @param sessionInfo user session info
     * @return log file resource
     */
    public Resource donwloadUserLog(SessionInfo sessionInfo);

    /**
     * Displays a message to users. Message may include new line characters.
     * @param userConversationIds user conversation ids or empty list if should be sent to all
     * @param title title
     * @param message message
     * @return count of sessions that message was successfully send to
     */
    public int sendMessage(List<String> userConversationIds, String title, String message);

    /**
     * Returns name of active use case for given user conversation.
     * @param userConversationId user conversation id
     * @return name of active use case
     */
    String getUserActiveFunctionality(String userConversationId);

}
