package pl.fhframework.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


@Configuration
@Slf4j
@Order(0)
public class WebConfig extends DelegatingWebMvcConfiguration {

    @Value("${fhframework.language.default:pl}")
    private String defaultLang;

    @Value("${fhframework.language.supported:en,pl,lt,ru}")
    private String supportedLangs;



//    @Bean
//    @Override
//    @Primary
//    public LocaleResolver localeResolver() {
//        log.info("************* Start cookie locale resolver...");
//        CookieLocaleResolver resolver = new CookieLocaleResolver();
//        resolver.setCookieName("USERLANG");
//        resolver.setDefaultLocale(Locale.forLanguageTag(defaultLang));
//        return resolver;
//    }
    @Bean
    @Override
    @Primary
    public LocaleResolver localeResolver() {
        log.info("************* Start custom cookie locale resolver...");
        return new CustomCookieLocaleResolver(defaultLang, supportedLangs);
    }

    static class CustomCookieLocaleResolver implements LocaleResolver {

        private final String defaultLang;
        private final List<Locale> supportedLocales;
        private final String cookieName = "USERLANG";

        public CustomCookieLocaleResolver(String defaultLang, String supportedLangs) {
            this.defaultLang = defaultLang;
            this.supportedLocales = Arrays.stream(supportedLangs.split(","))
                                          .map(String::trim)
                                          .map(Locale::forLanguageTag)
                                          .collect(Collectors.toList());
            log.info("Supported locales: {}", supportedLocales);
        }

        @Override
        public Locale resolveLocale(HttpServletRequest request) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookieName.equals(cookie.getName())) {
                        String lang = cookie.getValue();
                        if (StringUtils.hasText(lang)) {
                            Locale locale = Locale.forLanguageTag(lang);
                            if (supportedLocales.contains(locale)) {
                                log.info("Resolved locale from cookie: {}", locale);
                                return locale;
                            } else {
                                log.warn("Locale from cookie {} is not supported, using default: {}", locale, defaultLang);
                                return getDefaultLocale();
                            }
                        }
                    }
                }
            }
            log.info("No cookie found, using default locale: {}", defaultLang);
            return getDefaultLocale();
        }

        @Override
        public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
            if (locale == null) {
                locale = getDefaultLocale();
            }

            if (!supportedLocales.contains(locale)) {
                log.warn("Attempted to set unsupported locale: {}, using default: {}", locale, defaultLang);
                locale = getDefaultLocale();
            }

            String langTag = locale.toLanguageTag();
            Cookie cookie = new Cookie(cookieName, langTag);
            cookie.setPath("/"); // Ważne: Ustaw ścieżkę cookie!
            //cookie.setSecure(true); // Ustaw jeśli używasz HTTPS
            cookie.setMaxAge(30 * 24 * 60 * 60); // Czas życia cookie (30 dni)
            response.addCookie(cookie);
            log.info("Setting locale to cookie {}: {}", cookieName, locale);

        }

        private Locale getDefaultLocale() {
            return Locale.forLanguageTag(defaultLang);
        }
    }


}
