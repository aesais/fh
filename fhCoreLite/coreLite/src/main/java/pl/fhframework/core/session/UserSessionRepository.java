package pl.fhframework.core.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.socket.WebSocketSession;
import pl.fhframework.UserSession;
import pl.fhframework.UserSessionSharedData;
import pl.fhframework.WebSocketSessionManager;
import pl.fhframework.core.logging.FhLogger;
import pl.fhframework.core.security.model.SessionInfo;
import pl.fhframework.event.dto.ForcedLogoutEvent;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserSessionRepository implements HttpSessionListener, ApplicationListener<ContextRefreshedEvent> {

    @Autowired(required = false)
    private ForceLogoutService forceLogoutService;

    private final Map<String, UserSessionSharedData> userSessionSharedDataByHttpSessionId = new ConcurrentHashMap<>();
    private final Map<String, UserSession> userConversationsByConnectionId = new ConcurrentHashMap<>();
    private Set<Consumer<UserSession>> userSessionDestroyedListeners = new HashSet<>();
    private Set<Consumer<UserSession>> userSessionKeepAliveListeners = new HashSet<>();

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private final SessionInfoCache sessionInfoCache;
    @Autowired
    private SessionInfoAPIClient sessionInfoAPIClient;

    @Value("${fhframework.managementApi.enabled:false}")
    private boolean managementApiEnabled;

    @Value("${server.port}")
    private int serverPort;
    @Value("${fh.session.info.protocol:http}")
    private String sessionInfoProtocol;
    @Value("${fh.ws.closed.inactive_session_max_time:5}")
    private int sustainTimeOutMinutes;

    private String nodeUrl;



    @PostConstruct
    public void init() {
        ((WebApplicationContext) applicationContext).getServletContext().addListener(this);
    }


    @Override
    public synchronized void onApplicationEvent(ContextRefreshedEvent event) {
        WebSocketSessionManager.setSustainTimeout(sustainTimeOutMinutes * 60);

        // register node in cache
        nodeUrl = generateNodeUrl();
        int iter = 0;
        do {
            iter++;
            Set<String> nodes = sessionInfoCache.getNodes();
            nodes.add(nodeUrl);
            sessionInfoCache.putNodes(nodes);

            // wait a while and check whether node has been added, if not then try again
            try {
                Thread.sleep(new Random().nextInt(300) + 200);
            } catch (InterruptedException e) {
                // nothing
            }
        } while (!sessionInfoCache.getNodes().contains(nodeUrl) && iter < 5);

        // set empty collection for user sessions info
        sessionInfoCache.putSessionsInfoForNode(nodeUrl, new ConcurrentHashMap<>());
    }

    /**
     * Deprecated - use getHttpSessionCount() instead
     * @return
     */
    @Deprecated
    public int getUserSessionCount() {
        //return userConversations.size();
        return getHttpSessionCount();
    }

    public void addUserSessionDestroyedListener(Consumer<UserSession> listener) {
        userSessionDestroyedListeners.add(listener);
    }

    public void addUserSessionKeepAliveListener(Consumer<UserSession> listener) {
        userSessionKeepAliveListeners.add(listener);
    }

    public SessionInfo getSessionInfo(UserSession userConversation) {
        Map<String, SessionInfo> sessionsInfo = sessionInfoCache.getSessionsInfoForNode(nodeUrl);
        return sessionsInfo.get(userConversation.getConversationId());
    }


    public synchronized void putSessionInfo(String httpSessionId, UserSession userSession) {
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setHttpSessionId(httpSessionId);
        sessionInfo.setConversationId(userSession.getConversationId());
        sessionInfo.setConnectionId(userSession.getConnectionId());
        sessionInfo.setLogonTime(new Date(userSession.getCreationTimestamp().toEpochMilli()));
        sessionInfo.setUserName(userSession.getSystemUser().getLogin());
        sessionInfo.setNodeUrl(nodeUrl);
        sessionInfo.setClosed(userSession.isClosed());
        // put into cache
        Map<String, SessionInfo> sessionsInfo = sessionInfoCache.getSessionsInfoForNode(nodeUrl);
        sessionsInfo.put(userSession.getConversationId(), sessionInfo);
        sessionInfoCache.putSessionsInfoForNode(nodeUrl, sessionsInfo);
    }

    private synchronized void removeSessionInfo(UserSession userConversation) {
        String conversationId = userConversation.getConversationId();
        Map<String, SessionInfo> sessionsInfo = sessionInfoCache.getSessionsInfoForNode(nodeUrl);
        sessionsInfo.remove(conversationId);
        sessionInfoCache.putSessionsInfoForNode(nodeUrl, sessionsInfo);
    }

    private synchronized void updateSessionInfo(UserSession userSession){
        Map<String, SessionInfo> sessionsInfo = sessionInfoCache.getSessionsInfoForNode(nodeUrl);
        if(sessionsInfo.containsKey(userSession.getConversationId())){
            SessionInfo sessionInfo = sessionsInfo.get(userSession.getConversationId());
            sessionInfo.setClosed(userSession.isClosed());
            sessionInfo.setConnectionId(userSession.getConnectionId());
            sessionInfo.setHttpSessionId(userSession.getSharedData().getHttpSessionId());
        } else {
            log.warn("Could not find sessionInfo id for conversation {}", userSession.getConversationId());
        }

    }

    private String generateNodeUrl() {
        try {
            return String.format(
                "%s://%s:%s/",
                sessionInfoProtocol,
                InetAddress.getLocalHost().getHostAddress(),
                serverPort
            );
        } catch (UnknownHostException e) {
            FhLogger.errorSuppressed(e);
            return null;
        }
    }

    public Map<String, SessionInfo> getAllUserSessionsInfo() {
        Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
        Set<String> nodes = sessionInfoCache.getNodes();
        for (String node : nodes) {
            if (isNodeActive(node)) {
                sessions.putAll(sessionInfoCache.getSessionsInfoForNode(node));
            } /*else {
                removeNodeFromCache(node);
            }*/
        }
        return sessions;
    }

    /** Returns whether given node is active */
    public boolean isNodeActive(String nodeUrl) {
        if (managementApiEnabled) {
            return sessionInfoAPIClient.isNodeActive(nodeUrl);
        } else {
            return true; // only local user sessions
        }
    }

    private synchronized void removeNodeFromCache(String node) {
        Set<String> nodes = sessionInfoCache.getNodes();
        nodes.remove(node);
        sessionInfoCache.putNodes(nodes);
        sessionInfoCache.evictSessionsInfoForNode(node);
    }

    @Override
    public void sessionCreated(HttpSessionEvent httpSessionEvent) {
        // ignore
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
        onHttpSessionExpired(httpSessionEvent.getSession());
    }

    public void onSessionKeepAlive(UserSessionSharedData sharedData) {
        sharedData.getConversations().forEach(conversation -> {
            for (Consumer<UserSession> listener : userSessionKeepAliveListeners) {
                listener.accept(conversation);
            }
        });
    }

    public void removeUserConversation(UserSession userConversation) {
        for (Consumer<UserSession> listener : userSessionDestroyedListeners) {
            listener.accept(userConversation);
        }
//        if (userConversation.isClosed()) {
            userConversationsByConnectionId.remove(userConversation.getConnectionId());
            removeSessionInfo(userConversation);
//        }
        UserSessionSharedData sharedData = userConversation.getSharedData();
        sharedData.removeConversation(userConversation);
        if (sharedData.getConversations().isEmpty()) {
            removeHttpSession(sharedData.getHttpSession());
        }
    }

    private void removeHttpSession(HttpSession httpSession) {
        userSessionSharedDataByHttpSessionId.remove(httpSession.getId());
    }

    public void onHttpSessionExpired(HttpSession httpSession) {
        UserSessionSharedData sharedData = getUserSessionSharedData(httpSession);
        int noOfConversations = sharedData.getConversations().size();
        forceLogoutService.forceLogout(sharedData, ForcedLogoutEvent.Reason.LOGOUT_FORCE_LOGIN);
//        boolean result = removeHttpSessionWithAllConversations(httpSession);
//        if (result) {
//            FhLogger.info("Removed expired session for {} with id {} and {} conversations.", sharedData.getSystemUser().getLogin(), httpSession.getId(), noOfConversations);
//        } else {
//            FhLogger.error("Unsuccessful attempt to delete the session for {} with id {} and {} conversations.", sharedData.getSystemUser().getLogin(), httpSession.getId(), noOfConversations);
//        }
    }

//    private boolean removeHttpSessionWithAllConversations(HttpSession httpSession) {
//        UserSessionSharedData sharedData = userSessionSharedDataByHttpSessionId.get(httpSession.getId());
//        if (sharedData != null) {
//            try {
//                sharedData.getConversations().forEach(userConversation -> {
//                    for (Consumer<UserSession> listener : userSessionDestroyedListeners) {
//                        listener.accept(userConversation);
//                    }
//                    if (userConversation.isClosed()) {
//                        userConversationsByConnectionId.remove(userConversation.getConversationId());
//                        removeSessionInfo(userConversation);
//                    }
//                });
//                sharedData.clearConversations();
//                userSessionSharedDataByHttpSessionId.remove(httpSession.getId());
//                return true;
//            } catch (Exception e) {
//                FhLogger.errorSuppressed(e);
//                return false;
//            }
//        }else{
////            FhLogger.error("Can't get shared data for session for http session with id {}", httpSession.getId());
//            return false;
//        }
//    }

    public static String getUserLogin(UserSession userSession){
        try{
            return userSession.getSharedData().getSystemUser().getLogin();
        }catch (Exception ex){
            return "unknown user";
        }
    }

    public boolean areActiveConversationsInHttpSession(HttpSession httpSession) {
        return getUserSessionsInHttpSession(httpSession).isEmpty();
    }

    public Set<UserSession> getUserSessionsInHttpSession(HttpSession httpSession) {
        UserSessionSharedData sharedData = getUserSessionSharedData(httpSession);
        if (sharedData != null) {
//            log.info("Found {} user conversations in http session {}", sharedData.getConversations().size(), httpSession.getId());
            return Collections.unmodifiableSet(sharedData.getConversations());
        }else{
//            log.warn("No user sessions in http session {}", httpSession.getId());
            return Collections.emptySet();
        }
    }

    public UserSession getUserSession(WebSocketSession webSocketSession) {
        UserSession foundCurrentSession = userConversationsByConnectionId.get(webSocketSession.getId());
        if (foundCurrentSession == null) {
            String previousConversationId = (String) webSocketSession.getAttributes().get("conversationId");
            if (previousConversationId != null) {
                UserSession foundPreviousSession = getAllUserSessions().stream().
                        filter(us -> us.getConversationId().equals(previousConversationId))
                        .findFirst().orElse(null);
                if (foundPreviousSession != null) {
                    String oldConnectionId = foundPreviousSession.getConnectionId();
                    foundPreviousSession.setConnectionId(webSocketSession.getId());
                    userConversationsByConnectionId.remove(oldConnectionId);
                    userConversationsByConnectionId.put(foundPreviousSession.getConnectionId(), foundPreviousSession);
                }

            }
        }
        return foundCurrentSession;
    }

    public Set<UserSession> getAllUserSessions(){
        return new HashSet<>(userConversationsByConnectionId.values());
    }

    public UserSessionSharedData getUserSessionSharedData(HttpSession httpSession) {
        UserSessionSharedData sharedData = userSessionSharedDataByHttpSessionId.get(httpSession.getId());
        if (sharedData != null) {
            return sharedData;
        }else{
//            log.warn("Can't get shared data for session due to http session with id {}", httpSession.getId());
            return null;
        }
    }

    public UserSessionSharedData getUserSessionSharedData(String httpSessionId) {
        return userSessionSharedDataByHttpSessionId.get(httpSessionId);
    }
    public void registerNewConversation(UserSession newConversation) {
        UserSessionSharedData sharedData = newConversation.getSharedData();
        userSessionSharedDataByHttpSessionId.computeIfAbsent(sharedData.getHttpSessionId(), k -> sharedData);
        userConversationsByConnectionId.put(newConversation.getConnectionId(), newConversation);
        putSessionInfo(sharedData.getHttpSessionId(), newConversation);
    }

    public int getHttpSessionCount() {
        return userSessionSharedDataByHttpSessionId.size();
    }

    public Set<UserSessionSharedData> findSharedDataByUserName   (String userName) {
        return userSessionSharedDataByHttpSessionId.values().stream()
                .filter(sharedData -> sharedData.getSystemUser().getLogin().equals(userName))
                .collect(Collectors.toSet());
    }

    public Collection<UserSessionSharedData> getAllSessionsSharedData() {
        return Collections.unmodifiableCollection(userSessionSharedDataByHttpSessionId.values());
    }

    public UserSession getUserSession(SessionInfo sessionInfo) {
        return userConversationsByConnectionId.get(sessionInfo.getConnectionId());
    }

    public UserSession getPreviousUserConversation(WebSocketSession webSocketSession){
        UserSession foundCurrentSession = userConversationsByConnectionId.get(webSocketSession.getId());
        if (foundCurrentSession == null) {
            String previousConversationId = (String) webSocketSession.getAttributes().get("conversationId");
            if (previousConversationId != null) {
                return getAllUserSessions().stream().
                        filter(us -> us.getConversationId().equals(previousConversationId))
                        .findFirst().orElse(null);
            }
        }
        return foundCurrentSession;
    }

    public void restorePreviousUsersSession(UserSession userSession, WebSocketSession newWebSocketSession) {
        String newConnectionId = newWebSocketSession.getId();
        if (!userConversationsByConnectionId.containsKey(newConnectionId)){
            String oldConnectionId = userSession.getConnectionId();
            userConversationsByConnectionId.remove(oldConnectionId);
            userSession.setConnectionId(newConnectionId);
            userConversationsByConnectionId.put(newConnectionId, userSession);
            HttpSession httpSession = WebSocketSessionManager.getHttpSession();
            if(!userSession.getSharedData().getHttpSessionId().equals(httpSession.getId())){
                log.warn("Restored conversation {} changed HTTP session from {} to {}", userSession.getConversationId(), userSession.getSharedData().getHttpSessionId(), httpSession.getId());
                //TODO: compare users. If do not match...
                userSession.getSharedData().changeHttpSession(httpSession);
            }
            updateSessionInfo(userSession);
        }else{
            log.warn("Restoring was unnecessary - it already exist user conversation on connection {}", newConnectionId);
        }
    }

    public UserSession closeConversation(WebSocketSession webSocketSession) {
        UserSession conversation = getUserSession(webSocketSession);
        if(conversation != null){
            conversation.setClosed(true);
            updateSessionInfo(conversation);
        }
        return conversation;
    }
}
