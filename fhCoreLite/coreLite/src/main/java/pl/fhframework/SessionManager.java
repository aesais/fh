package pl.fhframework;


import pl.fhframework.core.session.SessionHoldingSessionManager;
import pl.fhframework.model.security.SystemUser;
import java.util.Set;

/**
 * Manager of user context and session.
 */
public abstract class SessionManager {

    private static final ThreadLocal<ISessionManagerImpl> THREAD_SESSION_MANAGER = new ThreadLocal<>();

    public static Session getSession() {
        ISessionManagerImpl instance = THREAD_SESSION_MANAGER.get();
        return instance != null ? instance.getSession() : null;
    }

    public static NoUserSession getNoUserSession() {
        ISessionManagerImpl instance = THREAD_SESSION_MANAGER.get();
        return instance != null && instance.getSession() instanceof NoUserSession ? (NoUserSession) instance.getSession() : null;
    }

    public static UserSession getUserSession() {
        ISessionManagerImpl instance = THREAD_SESSION_MANAGER.get();
        if (instance instanceof SessionHoldingSessionManager) {
            return null;
        }
        return instance != null && instance.getSession() instanceof UserSession ? (UserSession) instance.getSession() : null;
    }

    public static Set<UserSession> getUserSessionsInCurrentScope() {
        ISessionManagerImpl instance = THREAD_SESSION_MANAGER.get();
        return instance != null ? instance.getSessionsInCurrentScope() : null;
    }

    public static UserSessionSharedData getUserSessionSharedData() {
        //return THREAD_SESSION_SHARED_DATA.get();
        Set<UserSession> userSessions = getUserSessionsInCurrentScope();
        if (userSessions != null && !userSessions.isEmpty()) {
            return userSessions.iterator().next().getSharedData();
        } else{
            return null;
        }
    }

    public static SystemUser getSystemUser() {
        Session session = getSession();
        return session != null ? session.getSystemUser() : null;
    }

    public static String getUserLogin() {
        SystemUser user = getSystemUser();
        return user != null ? user.getLogin() : null;
    }

    public static String getUserFullName() {
        SystemUser user = getSystemUser();
        return user != null ? user.getFullName() : null;
    }

    public static void registerThreadSessionManager(ISessionManagerImpl sessionManager) {
        THREAD_SESSION_MANAGER.set(sessionManager);
    }

    public static void unregisterThreadSessionManager() {
        THREAD_SESSION_MANAGER.remove();
    }
}
