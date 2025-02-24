package pl.fhframework.core.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.fhframework.UserSession;
import pl.fhframework.UserSessionSharedData;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * UserSession finder for REST controllers which uses HttpSession attribute.
 */
@Component
@Slf4j
public class HttpSessionUserSessionFinder implements IRestUserSessionFinder {

    @Autowired
    private UserSessionRepository userSessionRepository;

    public Set<UserSession> getUserConversationsForSameHttpSession(HttpServletRequest httpServletRequest) {
        HttpSession httpSession = httpServletRequest.getSession(false);
        if (httpSession != null) {
            Set<UserSession> userSessions = userSessionRepository.getUserSessionsInHttpSession(httpSession);
            if (userSessions != null) {
                return Collections.unmodifiableSet(userSessions);
            } else {
                return Collections.emptySet();
            }
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public Optional<UserSessionSharedData> getUserSessionSharedData(HttpServletRequest httpServletRequest) {
        HttpSession httpSession = httpServletRequest.getSession(false);
        if (httpSession != null) {
            UserSessionSharedData userSessionSharedData = userSessionRepository.getUserSessionSharedData(httpSession);
            return Optional.ofNullable(userSessionSharedData);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<UserSession> getUserSession(HttpServletRequest httpServletRequest) {
        HttpSession httpSession = httpServletRequest.getSession(false);
        if (httpSession != null) {
            return getMatchingUserSession(httpServletRequest);
            //return mockImplementation(httpServletRequest);
            //return Optional.ofNullable(userSessionRepository.getUserSession(httpSession));
        } else {
            return Optional.empty();
        }
    }

    private Optional<UserSession> getMatchingUserSession(HttpServletRequest httpServletRequest) {
        HttpSession httpSession = httpServletRequest.getSession(false);
        if (httpSession != null) {
            Set<UserSession> conversationsInHttpSession = userSessionRepository.getUserSessionsInHttpSession(httpSession);
            if (conversationsInHttpSession == null || conversationsInHttpSession.isEmpty()) {
                return Optional.empty();
            } else {
                String webSocketSessionId = getWebSocketSessionId(httpServletRequest);

                if (webSocketSessionId != null) {
                    return conversationsInHttpSession.stream()
                            .filter(userSession -> userSession.getConversationId().equals(webSocketSessionId))
                            .findFirst();
                } else {
                    String conversationsIds = conversationsInHttpSession.stream().map(UserSession::getConversationId).reduce("", (a, b) -> a + ", " + b);
                    log.warn("Zwrócono pusty Optional bo brak parametru {}, chociaż w tej sesji są już konwersacje: {}", UserSession.WEB_SOCKET_SESSION_ID, conversationsIds);

                    return Optional.empty();
                }
            }
        } else {
            return Optional.empty();
        }
    }

    private String getWebSocketSessionId(HttpServletRequest httpServletRequest) {
        String webSocketSessionId = httpServletRequest.getHeader(UserSession.WEB_SOCKET_SESSION_ID);
        Map<String, String[]> map = httpServletRequest.getParameterMap();
        if (map.containsKey(UserSession.WEB_SOCKET_SESSION_ID)) {
            webSocketSessionId = map.get(UserSession.WEB_SOCKET_SESSION_ID)[0];
        }
//        else if (webSocketSessionId == null) {
//            throw new RuntimeException("Missing header attribute or parameter " + UserSession.WEB_SOCKET_SESSION_ID);
//        }
        return webSocketSessionId;
    }

    private Optional<UserSession> mockImplementation(HttpServletRequest httpServletRequest) {
        HttpSession httpSession = httpServletRequest.getSession(false);
        Set<UserSession> userSessions = userSessionRepository.getUserSessionsInHttpSession(httpSession);
        if (userSessions == null || userSessions.isEmpty()) {
            return Optional.empty();
        } else if (userSessions.size() == 1) {
            //If there is only one user session in the http session we simple return it
            return Optional.of(userSessions.iterator().next());
        } else {
            //If there is more than one user session in the http session we need to check if we pick some in httpServerletRequest header
            String webSocketSessionId = httpServletRequest.getHeader(UserSession.WEB_SOCKET_SESSION_ID);
            if (webSocketSessionId != null) {
                Optional<UserSession> foundUserSession = userSessionRepository.getUserSessionsInHttpSession(httpSession).stream()
                        .filter(userSession -> userSession.getConversationId().equals(webSocketSessionId))
                        .findFirst();
                if (foundUserSession.isPresent()) {
                    return foundUserSession;
                } else {
                    return Optional.of(userSessions.iterator().next());
                }
            } else {
                //If there is no header WEB_SOCKET_SESSION_ID or no user session is matched we pick the first user session in the http session
                log.warn("No header " + UserSession.WEB_SOCKET_SESSION_ID + " or no userSession is matched so I pick the first userSession in the current http session. It could be wrong if there is more than one userSession/windows in the sane http session.");
                return Optional.of(userSessions.iterator().next());
            }
        }
    }
}
