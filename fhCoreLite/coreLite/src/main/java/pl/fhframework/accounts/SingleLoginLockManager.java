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

    private final SingleLoginLockCache singleLoginLockCache;

    public SingleLoginLockManager(SingleLoginLockCache singleLoginLockCache) {
        this.singleLoginLockCache = singleLoginLockCache;
    }


//    @Value("${fh.single.login:true}")
//    private Boolean turnedOn;

    public String assignUserLogin(String userName, String sessionId) {
//        if (isTrunedOn()) {
            synchronized (WebSocketSessionManager.getHttpSession()) {
//                if (!containsKey(userName)) {
                    String prevSessionId = singleLoginLockCache.get(userName);
                    singleLoginLockCache.update(userName, sessionId);
                    return prevSessionId;
//                }
//                throw new RuntimeException("User is already logged");
            }
//        }
//        return null;
    }

    public void logout(String userName) {
        synchronized (singleLoginLockCache) {
            singleLoginLockCache.update(userName, null);
        }
    }

    private boolean containsKey(String userName) {
        return singleLoginLockCache.get(userName) != null;
    }

    public boolean releaseUserLogin(String userName, String sessionId) {
        if (isTrunedOn()) {
            synchronized (WebSocketSessionManager.getHttpSession()) {
                if (sessionId.equals(singleLoginLockCache.get(userName))) {
                    singleLoginLockCache.update(userName, null);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isLoggedIn(String userName) {
        return containsKey(userName);
    }

    public boolean isLoggedInWithDifferentSession(String userName, String sessionId) {
        String currentSessionId = singleLoginLockCache.get(userName);
        return currentSessionId != null && !Objects.equals(currentSessionId, sessionId);
    }

    public boolean isLoggedInWithTheSameSession(String userName, String sessionId) {
        return sessionId.equals(singleLoginLockCache.get(userName));
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

    @Deprecated
    public boolean isTrunedOn() {
//        return fhConfiguration.isProdModeActive() && turnedOn;
        return true;
    }
}
