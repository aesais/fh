package pl.fhframework.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import pl.fhframework.core.util.StringUtils;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    LocaleChangeInterceptor yourInjectedInterceptor;

    /** [/path1/, classpath:/resource1/][/path2/, classpath:/resource2/] to be cached*/
    @Value("${fh.cachedResourceMappings:null}")
    private String resourcePairs;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(yourInjectedInterceptor);
    }

//    @Override
//    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
//        converters.clear();
//        converters.add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
//        for(HttpMessageConverter converter: converters) {
//            log.info("************** Added converter {}", converter.getClass().getName());
//        }
//    }


    private static final String[] CLASSPATH_RESOURCE_LOCATIONS = {
            "classpath:/META-INF/resources/", "classpath:/resources/",
            "classpath:/static/", "classpath:/public/"};

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");
        registry.addViewController("/login").setViewName("login");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!registry.hasMappingForPattern("/webjars/**")) {
            registry.addResourceHandler("/webjars/**").addResourceLocations(
                    "classpath:/META-INF/resources/webjars/");
        }
        if (!registry.hasMappingForPattern("/**")) {
            if (!StringUtils.isNullOrEmpty(resourcePairs)){
                Pattern pattern = Pattern.compile("\\[(.*?),\\s*(.*?)\\]");
                Matcher matcher = pattern.matcher(resourcePairs);
                while (matcher.find()) {
                    String path = matcher.group(1);
                    String resourceLocation = matcher.group(2);
                    registry.addResourceHandler(path)
                            .addResourceLocations(resourceLocation)
                            .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());
                }
            }
            registry.addResourceHandler("/**").addResourceLocations(
                    CLASSPATH_RESOURCE_LOCATIONS);
        }
    }
}