package pl.fhframework.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import pl.fhframework.ISessionClusterCoordinator;
import pl.fhframework.accounts.SecurityFilter;
import pl.fhframework.accounts.SingleLoginLockManager;
import pl.fhframework.config.FhWebConfiguration;
import pl.fhframework.core.security.IDefaultUser;
import pl.fhframework.core.security.SecurityProviderInitializer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Created by krzysztof.kobylarek on 2017-05-22.
 */

@Configuration
@Profile("app")
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
//    @Value("${fh.web.cors.origins:}")
//    private List<String> corsOrigins;
//    @Value("${fh.web.cors.methods:}")
//    private List<String> corsMethods;
//    @Value("${fh.web.cors.headers:}")
//    private List<String> corsHeaders;
//    @Value("${fh.web.cors.allowCredentials:false}")
//    private Boolean corsAllowCredentials;
    @Value("${fh.web.guests.allowed:false}")
    private boolean guestsAllowed;
    @Value("${fh.web.guests.authenticate.path:authenticateGuest}")
    private String authenticateGuestPath;
    @Value("${server.logout.path:logout}")
    private String logoutPath;

    @Value("${fh.login.maxPerUser:-1}")
    private int maxPerUser;

    private SecurityProviderInitializer securityProviderInitializer;

    @Autowired(required = false)
    private List<FhWebConfiguration> fhWebConfigurations = new ArrayList<>();

    @Autowired
    SingleLoginLockManager singleLoginManager;

    @Autowired
    private Optional<ISessionClusterCoordinator> sessionClusterCoordinator;

    @Autowired
    public void setSecurityProviderInitializer(SecurityProviderInitializer securityProviderInitializer) {
        this.securityProviderInitializer = securityProviderInitializer;
    }

    @Override
    public void configure(WebSecurity web) {
        web.httpFirewall(new HttpFirewall() {
            @Override
            public FirewalledRequest getFirewalledRequest(HttpServletRequest request) throws RequestRejectedException {
                return new FirewalledRequest(request) {
                    @Override
                    public void reset() {}
                };
            }

            @Override
            public HttpServletResponse getFirewalledResponse(HttpServletResponse response) {
                return response;
            }
        });
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();

        http.formLogin()
            .loginPage("/login")
            .failureUrl("/login?error").permitAll();

        http.httpBasic();
        http.cors();

        HttpSessionRequestCache hrc = new HttpSessionRequestCache();
        hrc.setCreateSessionAllowed(false);
        http.requestCache()
             .requestCache(hrc);

        int maxSessions = singleLoginManager.isTrunedOn() ? 1 : maxPerUser;

        if (sessionClusterCoordinator.isPresent()) {
            sessionClusterCoordinator.get().loginCountConfigure(http, sessionRegistry(), maxSessions);
        }

        http.sessionManagement()
            .maximumSessions(maxSessions)
            .sessionRegistry(sessionRegistry())
            .expiredUrl("/login");

        http.logout()
            .logoutRequestMatcher(new LogoutRequestMatcher("autologout", "logout"))
            .logoutSuccessUrl("/login?logout").deleteCookies("JSESSIONID")
            .invalidateHttpSession(true).permitAll();

        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry urlRegistry = http.authorizeHttpRequests();

        // if guests are not allowed FH Application Framework is not accessed without authentication (but still public html, thymeleaf templates are allowed)
        if (!guestsAllowed) {
            urlRegistry.antMatchers("/", "/index", "/socketForms").authenticated();
        }
        urlRegistry.antMatchers("/" + authenticateGuestPath).authenticated();

        // register urls available without restriction
        Set<String> publicUrls = new HashSet<>();
        publicUrls.add("/" + logoutPath);
        fhWebConfigurations.forEach(fhWebConfiguration -> publicUrls.addAll(fhWebConfiguration.permitedToAllRequestUrls()));
        urlRegistry.antMatchers(publicUrls.toArray(new String[0])).permitAll();

        http.addFilterBefore(customSecurityFilter(), UsernamePasswordAuthenticationFilter.class);
        fhWebConfigurations.forEach(fhWebConfiguration -> fhWebConfiguration.configure(http));

        // restrict all other request if guests are not allowed
        if (!guestsAllowed) {
            urlRegistry.anyRequest().authenticated();
        }
    }

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        Set<IDefaultUser> defaultUsers = new HashSet<>();
        fhWebConfigurations.forEach(fhWebConfiguration -> defaultUsers.addAll(fhWebConfiguration.getDefaultUsers()));
        securityProviderInitializer.configureAuthentication(auth, new ArrayList<>(defaultUsers));
    }

    @Bean // Password encoder used for JDBC Security Data Provider
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(8);
    }

    /*@Bean // Password encoder used for LDAP Security Data Provider
    public PasswordEncoder ldapPasswordEncoder() {
        return new LdapShaPasswordEncoder();
    }*/

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public SecurityFilter customSecurityFilter() {
        SecurityFilter sf = new SecurityFilter();
        SimpleUrlAuthenticationFailureHandler si = new SimpleUrlAuthenticationFailureHandler("/login?error");
        si.setAllowSessionCreation(false);
        sf.setAuthenticationFailureHandler(si);
        return sf;
    }

//    @Bean
//    CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//        if (!corsOrigins.isEmpty()) {
//            configuration.setAllowedOrigins(corsOrigins);
//        }
//        if (!corsMethods.isEmpty()) {
//            configuration.setAllowedMethods(corsMethods);
//        }
//        if (!corsHeaders.isEmpty()) {
//            configuration.setAllowedHeaders(corsHeaders);
//        }
//        configuration.setAllowCredentials(corsAllowCredentials);
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//        return source;
//    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    public static class LogoutRequestMatcher implements RequestMatcher {

        private final AntPathRequestMatcher pathMatcherLogout;
        private final AntPathRequestMatcher pathMatcherAutoLogout;

        public LogoutRequestMatcher(String autologout, String logout){
            this.pathMatcherLogout = new AntPathRequestMatcher("/" + logout);
            this.pathMatcherAutoLogout = new AntPathRequestMatcher("/" + autologout);
        }
        @Override
        public boolean matches(HttpServletRequest request) {
            return pathMatcherAutoLogout.matches(request) || pathMatcherLogout.matches(request);
        }
    }
}
