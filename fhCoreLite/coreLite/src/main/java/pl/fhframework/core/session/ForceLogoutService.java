package pl.fhframework.core.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import pl.fhframework.*;
import pl.fhframework.core.logging.FhLogger;
import pl.fhframework.event.dto.ForcedLogoutEvent;

import javax.servlet.http.HttpSession;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Service used to logout user
 */
@Service
@Profile("app")
@Slf4j
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
        if(userSessionSharedData == null){
            return true;
        }
        final AtomicBoolean someUserSessionHasBeenLogout = new AtomicBoolean(false);
        userSessionSharedData.getConversations().forEach(userSession -> {
            if (forceLogoutFromConversation(userSession, reason)){
                someUserSessionHasBeenLogout.compareAndSet(false, true);
            }
        });
        invalidateHttpSession(userSessionSharedData.getHttpSession());

        return someUserSessionHasBeenLogout.get();
    }

    private boolean forceLogoutFromConversation(UserSession userConversation, ForcedLogoutEvent.Reason reason) {
        if (userConversation == null) {
            return false;
        }
        try {
            //HttpSession httpSession = userConversation.getHttpSession();
            //String httpSessionId = httpSession.getId();
            try {
                userConversation.clearUseCaseStack();
            } catch (Exception e) {
                FhLogger.error(e); // ignore
            }

            // push info to client
            Optional<WebSocketSession> wsSessionOptional = webSocketSessionRepository.getSession(userConversation);
            wsSessionOptional.ifPresent(wsSession -> {
                if (wsSession.isOpen()){
                    try {
                        log.info("Pushing shutdown info to web socket session {} related with conversation {}", wsSession.getId(), userConversation.getConversationId());
                        userConversation.pushCloseWindowInfo(WebSocketContext.from(userConversation, wsSession));
                        userConversation.pushForcedLogoutInfo(WebSocketContext.from(userConversation, wsSession), reason);
//                        userConversation.pushShutdownInfo(WebSocketContext.from(userConversation, wsSession), true);
                    } catch (Exception e) {
                        FhLogger.error(e); // ignore - user will be logout on server side and will have to authenticate again
                    }
                }
                webSocketSessionRepository.closeSession(wsSession);
            });
            log.info("Removing conversation {} data", userConversation.getConversationId());
            userConversation.setAsClosed();
            userSessionRepository.removeUserConversation(userConversation);
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

    @Scheduled(fixedDelay = 5, initialDelay = 5, timeUnit = TimeUnit.SECONDS)
    public void clearExpiredConversations() {
//        FhLogger.info("Clearing expired conversations...");
        Instant cutOff = Instant.now().minus(10, ChronoUnit.SECONDS);
        userSessionRepository.getAllUserSessions().stream().filter(u -> u.isClosed() && u.getLastUsedTime().isBefore(cutOff)).forEach(userSessionRepository::removeUserConversation);
    }
}
