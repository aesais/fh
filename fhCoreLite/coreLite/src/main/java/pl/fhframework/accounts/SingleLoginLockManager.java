package pl.fhframework.accounts;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import pl.fhframework.WebSocketSessionManager;
import pl.fhframework.configuration.FHConfiguration;

import java.util.HashSet;
import java.util.Objects;

import javax.annotation.PreDestroy;

/**
 * Created by pawel.ruta on 2017-02-27.
 */
@Service
public class SingleLoginLockManager {
    @Autowired
    private SingleLoginLockCache singleLoginLockCache;

    @Autowired
    FHConfiguration fhConfiguration;

    @Value("${fh.single.login:true}")
    private Boolean turnedOn;

    public void assignUserLogin(String userName, String sessionId) {
        if (isTrunedOn()) {
            synchronized (WebSocketSessionManager.getHttpSession()) {
                if (singleLoginLockCache.get(userName+"_"+sessionId) == null) {
                    singleLoginLockCache.update(userName+"_"+sessionId, sessionId);
                    return;
                }
                throw new RuntimeException("User is already logged");
            }
        }
    }

    /**
     * On giver serwer user can be logged only one time (TODO: check wnen multiple logins enabled).
     * Dlatego parametr sessionId - był ale przestałem go używać - to
     * teraz nawet jest zgodne z kodem assignUserLogin
     * @param userName
     * @param sessionId
     * @return
     */
    public boolean releaseUserLogin(String userName, String sessionId) {
        if (isTrunedOn()) {
            synchronized (WebSocketSessionManager.getHttpSession()) {
                singleLoginLockCache.update(userName+"_"+sessionId, null);
                return true;
            }
        }
        return false;
    }

    public boolean isLoggedInWithDifferentSession(String userName, String sessionId) {
        String currentSessionId = singleLoginLockCache.get(userName+"_"+sessionId);
        return currentSessionId != null && !Objects.equals(currentSessionId, sessionId);
    }

    public boolean isLoggedInWithTheSameSession(String userName, String sessionId) {
        return sessionId.equals(singleLoginLockCache.get(userName+"_"+sessionId));
    }

    @Scheduled(cron = "*/3 * * * * *")
    public void updateTtl() {
        for (String userName : new HashSet<>(singleLoginLockCache.keySet())) {
            String sessionId = singleLoginLockCache.getNoCache(userName);
            if (sessionId != null) {
                singleLoginLockCache.update(userName, sessionId);
            }
        }
    }

    @PreDestroy
    protected void onExit() {
        for (String userName : new HashSet<>(singleLoginLockCache.keySet())) {
            String sessionId = singleLoginLockCache.getNoCache(userName);
            if (sessionId != null) {
                singleLoginLockCache.update(userName, null);
            }
        }
    }

    public boolean isTrunedOn() {
        return fhConfiguration.isProdModeActive() && turnedOn;
    }
}
