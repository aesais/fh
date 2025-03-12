package pl.fhframework;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.fhframework.core.session.ForceLogoutService;
import pl.fhframework.core.session.UserSessionRepository;
import pl.fhframework.event.dto.ForcedLogoutEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@ConditionalOnProperty(value = "fh.web.inactive_session_auto_logout")
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Profile("app")
public class SessionTimeoutManager {
    @Value("${fh.web.inactive_session_auto_logout:false}")
    private boolean active;

    @Value("${fh.web.inactive_session_max_time:10}")
    private int maxInactivityMinutes;

    @Value("${fh.web.inactive_session_counter_id:}")
    private String counterElementId;

    @Autowired
    private ForceLogoutService forceLogoutService;

    @Autowired
    private UserSessionRepository userSessionRepository;

    public Session.TimeoutData keepSessionAlive(UserSessionSharedData userSessionSharedData) {
        if (!active) {
            throw new IllegalStateException("Session timeout management is not enabled.");
        }

        Instant activityLimitDate = userSessionSharedData.getActivityLimitDate();
        if (activityLimitDate != null) {
            boolean allowedDuration = Instant.now().isBefore(activityLimitDate);
            if (allowedDuration) {
                activityLimitDate = getInstantMinutesLater();
                userSessionSharedData.setActivityLimitDate(activityLimitDate);
            }
            userSessionRepository.onSessionKeepAlive(userSessionSharedData);
            return new Session.TimeoutData(activityLimitDate, counterElementId, maxInactivityMinutes);
        } else {
            return null;
        }
    }

    Session.TimeoutData initSessionTimeout(UserSessionSharedData sharedData) {
        sharedData.setActivityLimitDate(getInstantMinutesLater());
        return this.keepSessionAlive(sharedData);
    }

    private Instant getInstantMinutesLater() {
        return Instant.now().plus(maxInactivityMinutes, ChronoUnit.MINUTES);
    }

    @Scheduled(fixedDelay = 500L)
    public synchronized void serverSideInactiveSessionsLogout() {
        if (active) {
            userSessionRepository.getAllSessionsSharedData().forEach(sharedData -> {
                if (Instant.now().isAfter(sharedData.getActivityLimitDate())) {
                    forceLogoutService.forceLogout(sharedData, ForcedLogoutEvent.Reason.LOGOUT_TIMEOUT);
                }
            });
        }
    }
}
