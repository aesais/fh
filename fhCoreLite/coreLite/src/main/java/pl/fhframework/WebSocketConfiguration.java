package pl.fhframework;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.List;

@Configuration
@EnableWebSocket
@Lazy
@Profile("app")
public class WebSocketConfiguration implements WebSocketConfigurer {
    @Value("${fh.web.socket.origins:}")
    private List<String> websocketOrigins;

    @Getter
    @Value("${fh.web.socket.textBufferSize:262144}")
    private int textBufferSize;

    @Getter
    @Value("${fh.web.socket.sendTimeLimit:60000}")
    private int sendTimeLimit;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        WebSocketFormsHandler webSocketFormsHandler = applicationContext.getBean(WebSocketFormsHandler.class);
        WebSocketHandlerRegistration regWebClient = registry.addHandler(webSocketFormsHandler.getSocketHandler(), "socketForms");
        if (!websocketOrigins.isEmpty()) {
            regWebClient.setAllowedOrigins(websocketOrigins.toArray(new String[0]));
        }
        regWebClient.addInterceptors(new HttpSessionInterceptor());
    }

    @Bean
    public ServletContextInitializer initializer() {
        return new ServletContextInitializer() {
            @Override
            public void onStartup(ServletContext servletContext) throws ServletException {
                servletContext.setInitParameter("org.apache.tomcat.websocket.textBufferSize", Integer.toString(textBufferSize));
            }
        };
    }

    /*@Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(262144);
        return container;
    }*/

    /*@Bean
    public HandlerMapping webSocketHandlerMapping() {
        ServletWebSocketHandlerRegistry registry = new ServletWebSocketHandlerRegistry(webSocketTaskScheduler());
        registerWebSocketHandlers(registry);
        return registry.getHandlerMapping();
    }*/

    @Bean
    public ThreadPoolTaskScheduler webSocketTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("WSHeartbeat");
        scheduler.setPoolSize(Runtime.getRuntime().availableProcessors());
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }
}
