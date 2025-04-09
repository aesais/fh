package pl.fhframework.csrf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;
import pl.fhframework.CommonHttpHeaders;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class CsrfTokenFilter extends OncePerRequestFilter {
    public static final String FH_INSTANCE_NAME = "fh.instanceName";

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {


        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        if (csrf != null) {
            Cookie cookie = WebUtils.getCookie(request, CommonHttpHeaders.CSRF);

            String token = csrf.getToken();

            if (cookie == null || token != null && !token.equals(cookie.getValue())) {
                cookie = new Cookie(CommonHttpHeaders.CSRF, token);
                cookie.setPath("/");

                response.addCookie(cookie);
            }
        }
        if(System.getProperty(FH_INSTANCE_NAME) == null) {
            String instanceNamePrefix = contextPath;
            if(instanceNamePrefix == null) {
                instanceNamePrefix = "instance";
            } else {
                instanceNamePrefix = instanceNamePrefix.replace("/", "");
            }
            String instanceName = String.format("%s-%s-%d", instanceNamePrefix, request.getLocalAddr(), request.getLocalPort());
            System.setProperty(FH_INSTANCE_NAME, instanceName);
        }

        filterChain.doFilter(request, response);
    }

}