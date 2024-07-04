package pl.fhframework;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;

public interface ISessionClusterCoordinator {
   void onConnect(UserSession session);
   void loginCountConfigure(HttpSecurity http, SessionRegistry sessionRegistry, int maxSessions);
}
