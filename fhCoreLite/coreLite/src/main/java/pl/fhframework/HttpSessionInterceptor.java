package pl.fhframework;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import pl.fhframework.core.logging.FhLogger;

import javax.servlet.http.HttpSession;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class HttpSessionInterceptor extends HttpSessionHandshakeInterceptor {
    public HttpSessionInterceptor() {
        setCreateSession(true); // for guests
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        boolean res = super.beforeHandshake(request, response, wsHandler, attributes);
        HttpSession httpSession = getSession(request);
        WebSocketSessionManager.prepareHttpSession(httpSession);
        FhLogger.info("HttpSession prepared");

        List<NameValuePair> attrs = URLEncodedUtils.parse(request.getURI(), StandardCharsets.UTF_8);

        Optional<NameValuePair> conversationId = attrs.stream().filter(nameValuePair -> nameValuePair.getName().equals("conversationId")).findFirst();

        conversationId.ifPresent(conversationIdAttr -> attributes.put("conversationId", conversationIdAttr.getValue()));

        attributes.put(WebSocketSessionManager.HTTP_SESSION_KEY, httpSession);
        return res;
    }

    private HttpSession getSession(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest serverRequest = (ServletServerHttpRequest) request;
            return serverRequest.getServletRequest().getSession(isCreateSession());
        }
        return null;
    }
}
