package pl.fhframework.core.session;

import pl.fhframework.UserSession;
import pl.fhframework.UserSessionSharedData;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.Set;

/**
 * Interface of a UserSession finder for REST controllers.
 */
public interface IRestUserSessionFinder {

    Optional<UserSession> getUserSession(HttpServletRequest httpServletRequest);
    Set<UserSession> getUserConversationsForSameHttpSession(HttpServletRequest httpServletRequest);
}
