package pl.fhframework.core.session;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pl.fhframework.ISessionManagerImpl;
import pl.fhframework.UserSession;

import java.util.Set;
/**
 * Session manager implementation that holds UserSession object.
 */
@RequiredArgsConstructor
@Getter
public class SessionHoldingSessionManager implements ISessionManagerImpl {
    private final Set<UserSession> sessionsInCurrentScope;

    @Override
    public UserSession getSession() {
        //This type of session manager is used to hold conversations (UserSession objects) for the same http session.
        throw new UnsupportedOperationException();
    }
}
