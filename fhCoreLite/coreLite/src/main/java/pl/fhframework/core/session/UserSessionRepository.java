package pl.fhframework.core.session;

import lombok.Getter;
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
import pl.fhframework.WebSocketSessionManager;
import pl.fhframework.core.logging.FhLogger;
import pl.fhframework.core.security.model.SessionInfo;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserSessionRepository implements HttpSessionListener, ApplicationListener<ContextRefreshedEvent> {

    private Map<String, UserSession> userSessionsByFhId = new ConcurrentHashMap<>();
    private Map<String, Set<UserSession>> userConversationsByHttpSessions = new ConcurrentHashMap<>();
    @Getter
    private Map<String, HttpSession> orphanSessions = new ConcurrentHashMap<>();
    @Getter
    private Map<String, UserSession> userSessionsByConversationId = new ConcurrentHashMap<>();
    //private Map<Integer, UserSession> userSessionsHash = new ConcurrentHashMap<>();
    private Map<String, UserSession> userConversations = new ConcurrentHashMap<>();
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

    public int getUserSessionCount() {
        return userConversations.size();
    }

    public void addUserSessionDestroyedListener(Consumer<UserSession> listener) {
        userSessionDestroyedListeners.add(listener);
    }

    public void addUserSessionKeepAliveListener(Consumer<UserSession> listener) {
        userSessionKeepAliveListeners.add(listener);
    }

    public void setUserSession(String httpSessionId, UserSession userSession) {
        Set<UserSession> userSessionsInHttpSession = userConversationsByHttpSessions.computeIfAbsent(httpSessionId, k -> new HashSet<>());
        userSessionsInHttpSession.add(userSession);

        userSessionsByConversationId.put(userSession.getConversationUniqueId(), userSession);
        userConversations.put(userSession.getConversationId(), userSession);
        putSessionInfo(httpSessionId, userSession);
    }

//    public void removeUserSession(String httpSessionId) {
//        UserSession userSession = userSessions.remove(httpSessionId);
//        userSessionsHash.remove(System.identityHashCode(userSession.getHttpSession()));
//        userSessionsByConversationId.remove(userSession.getConversationUniqueId());
//        removeSessionInfo(httpSessionId);
//    }

    public boolean removeUserSession(UserSession userSession) {
        userSessionsByConversationId.remove(userSession.getConversationUniqueId());
        userSessionsByConversationId.remove(userSession.getConversationUniqueId());
        userConversations.remove(userSession.getConversationId());
        userConversationsByHttpSessions.get(userSession.getHttpSession().getId()).remove(userSession);
        removeSessionInfo(userSession.getConversationUniqueId());
        return true;
    }

    private synchronized void putSessionInfo(String httpSessionId, UserSession userSession) {
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setHttpSessionId(httpSessionId);
        sessionInfo.setSessionId(userSession.getConversationUniqueId());
        sessionInfo.setLogonTime(new Date(userSession.getCreationTimestamp().toEpochMilli()));
        sessionInfo.setUserName(userSession.getSystemUser().getLogin());
        sessionInfo.setNodeUrl(nodeUrl);
        // put into cache
        Map<String, SessionInfo> sessionsInfo = sessionInfoCache.getSessionsInfoForNode(nodeUrl);
        sessionsInfo.put(userSession.getConversationUniqueId(), sessionInfo);
        sessionInfoCache.putSessionsInfoForNode(nodeUrl, sessionsInfo);
    }

    private synchronized void removeSessionInfo(String conversationId) {
        Map<String, SessionInfo> sessionsInfo = sessionInfoCache.getSessionsInfoForNode(nodeUrl);
        sessionsInfo.remove(conversationId);
        sessionInfoCache.putSessionsInfoForNode(nodeUrl, sessionsInfo);
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

//    public UserSession getUserSession(String httpSessionId) {
//        return userSessions.get(httpSessionId);
//    }

    @Override
    public void sessionCreated(HttpSessionEvent httpSessionEvent) {
        // ignore
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
        onHttpSessionExpired(httpSessionEvent.getSession());
    }

    public void onSessionKeepAlive(String conversationId) {
        UserSession session = userSessionsByConversationId.get(conversationId);
        if (session != null) {
            for (Consumer<UserSession> listener : userSessionKeepAliveListeners) {
                listener.accept(session);
            }
        }
    }

    private void onHttpSessionExpired(HttpSession httpSession) {
        Set<UserSession> sessions = getUserSessionsInHttpSession(httpSession);
        for (UserSession userSession : sessions) {
            if (userSession != null) {
                try {
                    for (Consumer<UserSession> listener : userSessionDestroyedListeners) {
                        listener.accept(userSession);
                    }
                } finally {
                    boolean response = removeUserSession(userSession);

                    if (response) {
                        FhLogger.info("Removed expired session for {}.", getUserLogin(userSession), userSession.getConversationId(), httpSession.getId());
                    } else {
                        FhLogger.error("Unsuccessful attempt to delete the session for {}", getUserLogin(userSession));
                    }
                }
            }
        }
    }

    public static String getUserLogin(UserSession userSession){
        try{
            return userSession.getSystemUser().getLogin();
        }catch (Exception ex){
            return "unknown user";
        }
    }

    public boolean areActiveConversationsInHttpSession(HttpSession httpSession) {
        return getUserSessionsInHttpSession(httpSession).isEmpty();
    }

    public Set<UserSession> getUserSessionsInHttpSession(HttpSession httpSession) {
        Set<UserSession> userSessions = userConversationsByHttpSessions.get(httpSession.getId());
        if (userSessions != null) {
            log.info("Found {} user sessions in http session {}", userSessions.size(), httpSession.getId());
            return Collections.unmodifiableSet(userSessions);
        }else{
            log.warn("No user sessions in http session {}", httpSession.getId());
            return Collections.emptySet();
        }
    }

    public UserSession getUserSession(WebSocketSession webSocketSession) {
        return userConversations.get(webSocketSession.getId());
    }

    public Set<UserSession> getAllUserSessions(){
        return new HashSet<>(userConversations.values());
    }

}
