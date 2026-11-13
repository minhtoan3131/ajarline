package vn.io.ajarline.content.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provides a Clock bean for Article services.
 *
 * <p>Using an injected Clock instead of calling Instant.now() directly
 * allows the application to use real time and tests to use a fixed
 * or controlled time.</p>
 */
@Configuration(proxyBeanMethods = false)
public class ContentRuntimeConfiguration {

    @Bean
    Clock contentClock() {
        return Clock.systemUTC();
    }
}
