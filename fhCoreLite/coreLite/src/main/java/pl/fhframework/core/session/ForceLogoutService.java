package pl.fhframework.core.session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import pl.fhframework.UserSession;
import pl.fhframework.UserSessionSharedData;
import pl.fhframework.WebSocketContext;
import pl.fhframework.WebSocketSessionRepository;
import pl.fhframework.core.logging.FhLogger;
import pl.fhframework.event.dto.ForcedLogoutEvent;

import javax.servlet.http.HttpSession;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Service used to logout user
 */
@Service
@Profile("app")
public class ForceLogoutService {

    @Autowired
    private WebSocketSessionRepository webSocketSessionRepository;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private UserSessionRepository userSessionRepository;
    
    public boolean forceLogoutByUsername(String username, ForcedLogoutEvent.Reason reason) {
        Set<UserSessionSharedData> sharedDataSet = userSessionRepository.findSharedDataByUserName(username);
        for (UserSessionSharedData sharedData : sharedDataSet) {
            forceLogout(sharedData, reason);
        }
        return true;
    }

    public boolean forceLogoutByHttpSessionId(String httpSessionId, ForcedLogoutEvent.Reason reason){
        return forceLogout(userSessionRepository.getUserSessionSharedData(httpSessionId), reason);
    }
    public boolean forceLogout(HttpSession httpSession, ForcedLogoutEvent.Reason reason){
        UserSessionSharedData sharedData = userSessionRepository.getUserSessionSharedData(httpSession);
        return forceLogout(sharedData, reason);
    }

    public boolean forceLogout(UserSessionSharedData userSessionSharedData, ForcedLogoutEvent.Reason reason) {
        final AtomicBoolean someUserSessionHasBeenLogout = new AtomicBoolean(false);
        userSessionSharedData.getConversations().forEach(userSession -> {
            if (forceLogoutFromConversation(userSession, reason)){
                someUserSessionHasBeenLogout.compareAndSet(false, true);
            }
        });
        invalidateHttpSession(userSessionSharedData.getHttpSession());

        return someUserSessionHasBeenLogout.get();
    }

    private boolean forceLogoutFromConversation(UserSession userSession, ForcedLogoutEvent.Reason reason) {
        if (userSession == null || userSession.isClosed()) {
            return false;
        }
        try {
            //HttpSession httpSession = userSession.getHttpSession();
            //String httpSessionId = httpSession.getId();
            try {
                userSession.clearUseCaseStack();
            } catch (Exception e) {
                FhLogger.error(e); // ignore
            }

            // push info to client
            Optional<WebSocketSession> wsSession = webSocketSessionRepository.getSession(userSession);
            if (wsSession.isPresent() && wsSession.get().isOpen()) {
                try {
                    userSession.pushForcedLogoutInfo(WebSocketContext.from(userSession, wsSession.get()), reason);
                    webSocketSessionRepository.closeSession(wsSession.get());
                } catch (Exception e) {
                    FhLogger.error(e); // ignore - user will be logout on server side and will have to authenticate again
                }
            }
            userSession.setAsClosed();


            return true;
        } catch (Exception e) {
            FhLogger.error(e);
            return false;
        }
    }

    private void invalidateHttpSession(HttpSession httpSession){
        String httpSessionId = httpSession.getId();
        SessionInformation si = sessionRegistry.getSessionInformation(httpSessionId);
        if (si != null) {
            si.expireNow();
        }
        try {
            httpSession.invalidate();
        } catch (IllegalStateException ise) {
            // it can be simultanously invalidated from browser with logout timer
            FhLogger.warn("Session " + httpSessionId + " hase been already invalidated");
        }
    }



    private Collection<UserSession> findUserSessionsByUsername(String username) {
        return userSessionRepository.getAllUserSessions().stream()
                .filter(s -> s.getSystemUser().getLogin().equals(username))
                .collect(Collectors.toList());
    }


}
