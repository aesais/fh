package pl.fhframework;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import pl.fhframework.accounts.SingleLoginLockManager;
import pl.fhframework.core.session.ForceLogoutService;
import pl.fhframework.event.dto.ForcedLogoutEvent;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class FhMultiWindowLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    @Autowired
    private ForceLogoutService forceLogoutService;
    @Autowired
    private SingleLoginLockManager singleLoginLockManager;


    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("FhMultiwindowLogoutSuccessHandler onLogoutSuccess. HTTPSessionId: {}", request.getSession().getId());
        if(authentication != null) {
            String userName = ((UserDetails) authentication.getPrincipal()).getUsername();
            singleLoginLockManager.logout(userName);
            forceLogoutService.forceLogoutByUsername(userName, ForcedLogoutEvent.Reason.LOGOUT_FORCE);
        }
        super.onLogoutSuccess(request, response, authentication);
    }

}
