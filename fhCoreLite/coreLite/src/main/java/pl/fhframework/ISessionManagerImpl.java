package pl.fhframework;

import java.util.Set;

/**
 * Interface of a session manager implementation.
 */
public interface ISessionManagerImpl {

    Session getSession();

    Set<UserSession> getSessionsInCurrentScope();
}
