package pl.fhframework.dp.commons.ad.handler;

import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CustomAuditedAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {


    public CustomAuditedAuthenticationFailureHandler(String defaultFailureUrl) {
        super(defaultFailureUrl);
        this.setAllowSessionCreation(false);
        this.setUseForward(true);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.AuthenticationException exception) throws IOException, ServletException {
        request.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, exception);
        super.onAuthenticationFailure(request, response, exception);
    }

}
