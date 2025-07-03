package pl.fhframework.core.session;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.fhframework.UserSessionSharedData;
import pl.fhframework.core.logging.FhLogger;
import pl.fhframework.event.dto.ForcedLogoutEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.time.Instant;
import javax.annotation.PreDestroy;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Mechanism for temporary session deletion. Contains cron which seeks for abandoned session and removes it.
 */
@Lazy(false)
@Component
@Profile("app")
public class LeakedSessionRemoverCron {
    @Autowired
    private UserSessionRepository userSessionRepository;
    @Autowired
    private ForceLogoutService forceLogoutService;

    /**
     * Controls whether the emergency removal of inactive user sessions is enabled.
     * If {@code true} (default), the system periodically checks for and removes leaked sessions.
     */
    @Value("${fh.session.emergency_removal_unused_sessions:true}")
    private boolean emergencyRemovalUnusedSessions;

    /**
     * The maximum number of seconds an unused session can survive - default value = 12 hours (43,200 seconds)
     */
    @Value("${fh.session.emergency_removal_time_unused_session:43200}")
    private int emergencyRemovalTimeUnusedSessionInSeconds;

    /**
     * The period for scheduler - default value = 1 hour (3600 seconds)
     */
    @Value("${fh.session.leaked_session_remover_cron_period:3600}")
    private int leakedSessionRemoverPeriod;

    /**
     * A flag that controls which scheduling mechanism is used for removing leaked sessions.
     * If set to {@code true} (default), a manual thread-based scheduler will be used.
     * If set to {@code false}, the standard Spring Boot scheduling (@Scheduled) mechanism will be used instead.
     * This flag should be set to {@code false} only if the application is running within a Spring container
     * that supports scheduled tasks. For standalone or non-Spring environments, keep it {@code true}.
     */
    @Value("${fh.session.leaked_session_remover_manual_cron:true}")
    private boolean manualCron;


    /**
     * Time in the millis when the last time cron has been working
     */
    private long lastCronTime = 0;

    /**
     * Runs the leaked session cleanup periodically using Spring's scheduling.
     * Works only if the application is running within a Spring container and {@code manualCron} is {@code false}.
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 120000)
    public void cleanupLeakedSessionsCron() {
        if (!manualCron) {
            cleanupLeakedSessions();
        }
    }

    /**
     * Removes leaked sessions if the configured interval has passed since the last run.
     * Triggered either by manual or Spring scheduler depending on {@code manualCron}.
     * Executes only if {@code emergencyRemovalUnusedSessions} is {@code true}.
     * Uses {@code fh.session.leaked_session_remover_cron_period} to determine the interval.
     */
    public void cleanupLeakedSessions() {
        if (emergencyRemovalUnusedSessions && lastCronTime <= System.currentTimeMillis() - (leakedSessionRemoverPeriod * 1000L)) {
            lastCronTime = System.currentTimeMillis();
            FhLogger.info("Cleanup leaked sessions cron: Seeking for outdated sessions. There are {} sessions. The oldest one hasn't been used since {} seconds.", userSessionRepository.getHttpSessionCount(), getInactivityTimeForMostExpiredSessions() / 1000);
            Collection<UserSessionSharedData> sessionsToRemove = getSessionsToRemove(userSessionRepository.getAllSessionsSharedData());
            if (!sessionsToRemove.isEmpty()) {
                //Removing sessions in a convenient way
                emergencySessionRemoval(sessionsToRemove);
                FhLogger.warn("Cleanup leaked sessions cron: removed {} sessions. Now the oldest one hasn't been used since {} seconds.", sessionsToRemove.size(), getInactivityTimeForMostExpiredSessions() / 1000);
            }
        }
    }

    /**
     * Returns inactivity time for most expired session
     *
     * @return Value in millis
     */
    private long getInactivityTimeForMostExpiredSessions() {
        UserSessionSharedData mostExpiredOne = getMostExpiredSession();
        if (mostExpiredOne != null) {
            return mostExpiredOne.getHowLongIsUnusedInMillis();
        } else {
            return 0;
        }
    }

    /**
     * Return most expired session or null if there are no sessions
     *
     * @return Session which has the longest unused time
     */
    private UserSessionSharedData getMostExpiredSession() {
        UserSessionSharedData oldestUsedSession = null;
        for (UserSessionSharedData i : userSessionRepository.getAllSessionsSharedData()) {
            if (oldestUsedSession == null || oldestUsedSession.getLastUsageMoment() > i.getLastUsageMoment()) {
                oldestUsedSession = i;
            }
        }
        return oldestUsedSession;
    }

    /**
     * Removes session given in param
     *
     * @param sessionsToRemove Sessions, which must be removed
     */
    private void emergencySessionRemoval(Collection<UserSessionSharedData> sessionsToRemove) {
        sessionsToRemove.forEach(sharedData -> {
            FhLogger.warn(UserSessionRepository.class, "Emergency removal obsolete session {} for user {} which has been unused for {} seconds.", sharedData.getHttpSessionId(), sharedData.getSystemUser().getLogin(), sharedData.getHowLongIsUnusedInMillis() / 1000);
            boolean result = forceLogoutService.forceLogout(sharedData, ForcedLogoutEvent.Reason.LOGOUT_FORCE_LOGIN);
            if (!result) {
                FhLogger.info(UserSessionRepository.class, "Logout in browser for obsolete session {} and user {} failed - maybe the user has already closed the browser.", sharedData.getHttpSessionId(), sharedData.getSystemUser().getLogin());
            } else {
                FhLogger.info(UserSessionRepository.class, "Logout in browser for obsolete session {} and user {} succeeded.", sharedData.getHttpSessionId(), sharedData.getSystemUser().getLogin());
            }
        });
    }

    /**
     * Returns a set of sessions which should be removed
     *
     * @param entries Entry set of all sessions.
     * @return set of session keys to remove
     */
    private Set<UserSessionSharedData> getSessionsToRemove(Collection<UserSessionSharedData> entries) {
        Set<UserSessionSharedData> sessionKeysToRemove = new HashSet<>();
        entries.forEach(entry -> {
            if (doesSessionShouldBeRemoved(entry)) {
                sessionKeysToRemove.add(entry);
            }
        });
        return sessionKeysToRemove;
    }

    /**
     * Rule that determines whether the given session should be removed
     *
     * @param userSession Checked session
     * @return True if the session should be removed
     */
    private boolean doesSessionShouldBeRemoved(UserSessionSharedData userSession) {
        if (userSession == null) {
            throw new IllegalArgumentException("userSession cannot be null");
        } else {
            if (emergencyRemovalUnusedSessions & userSession.hasNotBeenUsedIn(emergencyRemovalTimeUnusedSessionInSeconds * 1000L)) {
                return true;
            } else {
                Instant activityLimitDate = userSession.getActivityLimitDate();
                if (activityLimitDate == null) {
                    return false;
                }
                Instant now = Instant.now();
                return activityLimitDate.isBefore(now);
            }
        }
    }

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;

    /**
     * Method to start cron manually. Only one thread will be running at a time.
     */
    public synchronized void startManuallyScheduler() {
        if (!manualCron) return;
        if (scheduler != null && !scheduler.isShutdown()) {
            // Already running
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("LeakedSessionRemoverCron-manual");
            return t;
        });
        scheduledTask = scheduler.scheduleWithFixedDelay(() -> {
            if (emergencyRemovalUnusedSessions) {
                try {
                    cleanupLeakedSessions();
                } catch (Exception e) {
                    FhLogger.error("Exception in LeakedSessionRemoverCron manual cleanup", e);
                }
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
        FhLogger.info("LeakedSessionRemoverCron is starting manually.");
    }

    public synchronized void stopScheduler() {
        if (scheduledTask != null) {
            scheduledTask.cancel(true);
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    @PreDestroy
    public void preDestroy() {
        stopScheduler();
    }
}
