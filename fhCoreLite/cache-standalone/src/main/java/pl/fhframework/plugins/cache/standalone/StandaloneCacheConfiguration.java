package pl.fhframework.plugins.cache.standalone;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.fhframework.configuration.FHConfiguration;
import pl.fhframework.core.logging.FhLogger;
import pl.fhframework.core.util.StringUtils;

import java.util.concurrent.TimeUnit;


@Configuration
public class StandaloneCacheConfiguration {

    @Autowired
    private FHConfiguration fhConfiguration;

    @Value("${fh.cache.jgroups.configurationFile:jgroups_fh_default.xml}")
    private String jgroupsConfigurationFile;

    @Bean
    public EmbeddedCacheManager infiniSpanCacheManager() {
        String clusterName = fhConfiguration.getClusterName();
        FhLogger.info(this.getClass(), "Using infinispan cache cluster name: " + clusterName);

        return new DefaultCacheManager(new GlobalConfigurationBuilder().nonClusteredDefault()
                                                                       .transport()
                                                                       .clusterName(clusterName)
                                                                       .defaultTransport()
                                                                       .build(),true);
    }

    @Bean(name = "asyncReplicationConfig")
    public org.infinispan.configuration.cache.Configuration asyncReplicationConfig() {
        return new ConfigurationBuilder()
                .clustering()
                .cacheMode(CacheMode.REPL_ASYNC)
                .memory()
                .expiration().lifespan(10, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public Cache<String, String> loginsWithWebSocketCache(@Autowired org.infinispan.manager.EmbeddedCacheManager container, @Qualifier("asyncReplicationConfig") org.infinispan.configuration.cache.Configuration asyncReplicationConfig) {
        final String CACHE_NAME = "loginsWithWebSocket";
        container.defineConfiguration(CACHE_NAME, asyncReplicationConfig);
        return container.getCache(CACHE_NAME);
    }

    @Bean
    public Cache<String, Long> userLogouts(@Autowired org.infinispan.manager.EmbeddedCacheManager container, @Qualifier("asyncReplicationConfig") org.infinispan.configuration.cache.Configuration asyncReplicationConfig) {
        final String CACHE_NAME = "userLogouts";
        container.defineConfiguration(CACHE_NAME, asyncReplicationConfig);
        return container.getCache(CACHE_NAME);
    }

}
