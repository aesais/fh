package pl.fhframework.app;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;
import pl.fhframework.config.FhWebConfiguration;

import java.util.Arrays;
import java.util.List;

/**
 * @author tomasz.kozlowski (created on 27.06.2018)
 */
@Component
public class DefaultApplicationWebConfig implements FhWebConfiguration {

    @Override
    public void configure(HttpSecurity http) {
        try {
            // register management url access
            http.authorizeHttpRequests(
                  authorize -> authorize.mvcMatchers("/management/**").hasAuthority("IP_127_0_0_1"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> permitedToAllRequestUrls() {
        return Arrays.asList("/public/**", "/FormComponents/**", "/FormsManager.js",
                "/fhApplication*.bundle.css", "/fhApplication*.bundle.css.map",
                "/img/**", "/fonts/**", "/login","/gracefulShutdown", "/killInactive",
                "/externalInvokeCompleted", "/autologout");
    }

}
