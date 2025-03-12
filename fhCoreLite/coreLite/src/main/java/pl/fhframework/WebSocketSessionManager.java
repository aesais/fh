package pl.fhframework;


import lombok.Getter;
import lombok.Setter;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.socket.WebSocketSession;
import pl.fhframework.aspects.ApplicationContextHolder;
import pl.fhframework.core.logging.FhLogger;
import pl.fhframework.core.logging.LogLevel;
import pl.fhframework.core.session.UserSessionRepository;
import pl.fhframework.model.security.SystemUser;

import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

/**
 * Manager of user context in terms of interrelated web socket session , http session and user session
 */
public class WebSocketSessionManager implements ISessionManagerImpl {

    @Setter
    @Getter
    public static class WebSocketRequestContext implements Serializable {

        private boolean responseAlreadySent = false;

        @Setter
        private EventProcessState eventProcessState;

        private String requestId;
    }

    enum EventProcessState {
        Start,
        Step1,
        Step2,
        Step3,
        Step4,
        End
    }

    private static final WebSocketSessionManager INSTANCE = new WebSocketSessionManager();

    /**
     * key value used in WS Session atribute map to indicate coresponding HttpSession
     */
    public static final String HTTP_SESSION_KEY = "sesionHttp";

    /**
     * key value used in WS Session atribute map to indicate that communication is blocked for this WS, because only one
     * WS is allowed for one userName (login)
     */
    public static final String BLOCKED_WS_KEY = "blockedWs";

    /**
     * Timeout of http session when WS session is interrupted
     * This time should allow user to reconnect (to the same server or other node in cluster)
     */
    @Getter
    @Setter
    private static int sustainTimeout = 5 * 60;

    /**
     * Infinitive HttpSesion timeout
     * This value is used during normal web socket conwerstion
     */
    public static final int INFINITIVE_TIMEOUT = -1;

    /**
     * Thread local to keep web socket session
     */
    private static final ThreadLocal<WebSocketSession> threadLocalSession = new ThreadLocal<>();

    /**
     * Thread local to keep request time context
     */
    private static final ThreadLocal<WebSocketRequestContext> threadLocalRequestContext = new ThreadLocal<>();

    /**
     * Prepare httpSession to web socket conversation during which httpSesion never expires.
     * This allows  to keep session alive without heartbeayt
     *
     * @param httpSession
     */
    public static void prepareHttpSession(HttpSession httpSession) {
        httpSession.setMaxInactiveInterval(INFINITIVE_TIMEOUT);
    }

    /**
     * Sustain corresponding httpSession to value SUSTAIN_TIMEOUT which allow the user to safely reconnect
     * (including problem with the browser or crash of cluster node)
     *
     * @param webSocketSession
     */
    public static void sustainSession(WebSocketSession webSocketSession) {
        HttpSession sessionHttp = getHttpSession();
        try {
            // include current inactive time - FH-7448
            int sustainTimeout = WebSocketSessionManager.getSustainTimeout();
            Integer sustainTimeoutOverride = getInstance().getSession().getSustainTimeOutMinutesOverride();
            if (sustainTimeoutOverride != null) {
                sustainTimeout = sustainTimeoutOverride * 60;
            }
            int currentInactiveTime = (int) ((System.currentTimeMillis() - sessionHttp.getLastAccessedTime()) / 1000);
            sessionHttp.setMaxInactiveInterval(currentInactiveTime + sustainTimeout);
            FhLogger.info( "HttpSession marked for sustain timeout");
        } catch (IllegalStateException ise) {
            // session allready invalidated
            FhLogger.log(LogLevel.DEBUG, "HttpSession for web socket '{}' is already invalid", webSocketSession.getId());
        }
    }



    public static void prepareSessionScope() {
        final HttpSession httpSession = getHttpSession();
        WebSocketSessionRequestAttribute attributes = new WebSocketSessionRequestAttribute(httpSession);

        final Object locale = httpSession.getAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME);
        if (locale instanceof Locale) {
            LocaleContextHolder.setLocale((Locale) locale);
        }

        RequestContextHolder.setRequestAttributes(attributes);
    }


    /**
     * Set web socket session from web socket context and preserve it on thread local
     *
     * @param webSocketSession
     */
    public static WebSocketSession setWebSocketSession(WebSocketSession webSocketSession) {
        if (webSocketSession != null) {
            SessionManager.registerThreadSessionManager(INSTANCE);
            threadLocalRequestContext.set(new WebSocketRequestContext());
        } else {
            threadLocalRequestContext.remove();
            SessionManager.unregisterThreadSessionManager();
        }
        WebSocketSession prev = threadLocalSession.get();
        threadLocalSession.set(webSocketSession);
        return prev;
    }

    public static WebSocketRequestContext getRequestContext() {
        return threadLocalRequestContext.get();
    }

    public UserSession getSession() {
        return getUserSessionRepository().getUserSession(getWebSocketSession());
    }

    @Override
    public Set<UserSession> getSessionsInCurrentScope() {
        return Collections.singleton(getSession());
    }

    public static WebSocketSession getWebSocketSession() {
        return threadLocalSession.get();
    }

    public static WebSocketSessionManager getInstance() {
        return INSTANCE;
    }

    private WebSocketSessionManager() {
    }

    public static UserSession issueNewConversation(SystemUser systemUser, WebSocketSession session) {
        HttpSession httpSession = getHttpSession();
        //First we try take shared data from previous conversation - all conversations within the same http session share SharedData object.
        UserSessionSharedData sharedData = getUserSessionRepository().getUserSessionSharedData(httpSession);
        if (sharedData == null) {
            //If it is first conversation within http session we prepare structures and create new shared data between conversations in the same http session
            sharedData = new UserSessionSharedData(httpSession, systemUser);
        }
        UserSession newConversation = ApplicationContextHolder.getApplicationContext().getBean(UserSession.class, createDescription(session), sharedData, session);
        sharedData.addConversation(newConversation);
        getUserSessionRepository().registerNewConversation(newConversation);
        return newConversation;
    }

    public static HttpSession getHttpSession(WebSocketSession session) {
        return ((HttpSession) session.getAttributes().get(HTTP_SESSION_KEY));
    }

    public static HttpSession getHttpSession() {
        return getHttpSession(getWebSocketSession());
    }

    public static UserSession getUserSessionForWebSocketSession(WebSocketSession session) {
        return getUserSessionRepository().getUserSession(session);
    }

    private static UserSessionRepository getUserSessionRepository() {
        return ApplicationContextHolder.getApplicationContext().getBean(UserSessionRepository.class);
    }

    private static WebSocketSessionRepository getWebSocketSessionRepository(){
        return ApplicationContextHolder.getApplicationContext().getBean(WebSocketSessionRepository.class);
    }
    public static UserSession getPreviousUserSession(WebSocketSession webSocketSession){
        return getUserSessionRepository().getPreviousUserConversation(webSocketSession);
    }
    public static void restorePreviousUserSession(UserSession previousUserSession, WebSocketSession newWebSocketSession) {
        String oldConnectionId = previousUserSession.getConnectionId();
        getWebSocketSessionRepository().removeOldConnection(oldConnectionId);
        getUserSessionRepository().restorePreviousUsersSession(previousUserSession, newWebSocketSession);
    }

    private static UserSessionDescription createDescription(WebSocketSession session) {
        UserSessionDescription description = new UserSessionDescription();
        description.setServerAddress(session.getLocalAddress().toString());
        description.setClientInfo(session.getHandshakeHeaders().getFirst(HttpHeaders.USER_AGENT));
        description.setHandshakeHeaders(session.getHandshakeHeaders());
        description.setUserAddress(session.getRemoteAddress().toString());
        return description;
    }
}
