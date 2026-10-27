package vn.io.ajarline.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.time.ZoneOffset;

@TestConfiguration(proxyBeanMethods = false)
public class ContentApplicationTestConfiguration {

    @Bean
    @Primary
    MutableClock contentTestLock() {
        return new MutableClock(
                Instant.parse("2026-10-26T00:00:00Z"),
                ZoneOffset.UTC
        );
    }
}
