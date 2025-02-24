package pl.fhframework.core.session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import pl.fhframework.SessionManager;
import pl.fhframework.UserSession;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interceptor for REST controllers that attaches UserSession object to SessionManager.
 * UserSession is taken from available IRestUserSessionFinder implementations.
 */
@Component
public class RestUserSessionAttacher implements HandlerInterceptor {

    private static final String MARKER_ATTRIBUTE = "RestUserSessionAttacher.marker";

    @Autowired
    private List<IRestUserSessionFinder> sessionFinders;
    @Autowired
    UserSessionRepository userSessionRepository;

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) throws Exception {
        Set<UserSession> conversationsInScope = userSessionRepository.getUserSessionsInHttpSession(httpServletRequest.getSession());
        if (conversationsInScope!=null){
            SessionManager.registerThreadSessionManager(new SessionHoldingSessionManager(conversationsInScope));
            httpServletRequest.setAttribute(MARKER_ATTRIBUTE, true);
        }
        for (IRestUserSessionFinder sessionFinder : sessionFinders) {
            Set<UserSession> userConversations = sessionFinder.getUserConversationsForSameHttpSession(httpServletRequest);
            if (!userConversations.isEmpty()) {
                for (UserSession conversation : userConversations) {
                    Map<String, String[]> map = (HashMap<String, String[]>) conversation.getAttributes().computeIfAbsent(
                            "URL_PARAM", k -> new HashMap<String, String[]>());
                    map.putAll(httpServletRequest.getParameterMap());
                }
                break;
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {
        if (httpServletRequest.getAttribute(MARKER_ATTRIBUTE) != null) {
            SessionManager.unregisterThreadSessionManager();
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {
    }
}
