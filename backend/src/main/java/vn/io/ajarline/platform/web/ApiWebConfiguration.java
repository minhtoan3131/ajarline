package vn.io.ajarline.platform.web;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration(proxyBeanMethods = false)
public class ApiWebConfiguration {

    @Bean
    FilterRegistrationBean<ApiRequestFilter> apiRequestFilterRegistration() {
        FilterRegistrationBean<ApiRequestFilter> registration =
                new FilterRegistrationBean<>(new ApiRequestFilter());

        registration.setName("apiRequestFilter");
        registration.addUrlPatterns("/*");
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
