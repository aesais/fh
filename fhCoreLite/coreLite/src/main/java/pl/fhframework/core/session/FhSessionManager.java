package pl.fhframework.core.session;


import pl.fhframework.ISessionManagerImpl;
import pl.fhframework.Session;
import pl.fhframework.UserSession;

import java.util.Collections;
import java.util.Set;

/**
 * Manager of user context in terms of interrelated web socket session , http session and user session
 */
public class FhSessionManager implements ISessionManagerImpl {

    /**
     * Fh session
     */
    private Session session;


    public Session getSession() {
        return session;
    }

    @Override
    public Set<UserSession> getSessionsInCurrentScope() {
        if (session != null && session instanceof UserSession) {
            return Collections.singleton((UserSession) session);
        }else {
            return Collections.emptySet();
        }
    }

    public FhSessionManager(Session session) {
        this.session = session;
    }
}
