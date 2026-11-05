package vn.io.ajarline.platform.security;

import jakarta.servlet.DispatcherType;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;
import vn.io.ajarline.platform.security.authentication.AdminAuthenticationProvider;
import vn.io.ajarline.platform.security.authentication.AdminCredentials;
import vn.io.ajarline.platform.security.csrf.HeaderOnlyCsrfTokenRequestHandler;
import vn.io.ajarline.platform.security.handler.ApiAccessDeniedHandler;
import vn.io.ajarline.platform.security.handler.ApiAuthenticationEntryPoint;
import vn.io.ajarline.platform.web.ApiProblemWriter;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    @Bean
    PasswordEncoder adminPasswordEncoder() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    AdminCredentials adminCredentials(Environment environment) {
        return new AdminCredentials(
                environment.getRequiredProperty("ajarline.security.admin.username"),
                environment.getRequiredProperty("ajarline.security.admin.password-hash"));
    }

    @Bean
    AdminAuthenticationProvider adminAuthenticationProvider(
            AdminCredentials credentials,
            PasswordEncoder passwordEncoder) {
        return new AdminAuthenticationProvider(
                credentials,
                passwordEncoder,
                Clock.systemUTC());
    }

    @Bean
    AuthenticationManager adminAuthenticationManager(
            AdminAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository());
    }

    @Bean
    HttpSessionCsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository =
                new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-CSRF-TOKEN");
        return repository;
    }

    @Bean
    CsrfTokenRequestHandler csrfTokenRequestHandler() {
        return new HeaderOnlyCsrfTokenRequestHandler();
    }

    @Bean
    SessionAuthenticationStrategy adminSessionAuthenticationStrategy(
            HttpSessionCsrfTokenRepository csrfRepository) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(),
                new CsrfAuthenticationStrategy(csrfRepository)));
    }

    @Bean
    AuthenticationEntryPoint apiAuthenticationEntryPoint(
            ApiProblemWriter problems) {
        return new ApiAuthenticationEntryPoint(problems);
    }

    @Bean
    AccessDeniedHandler apiAccessDeniedHandler(
            ApiProblemWriter problems) {
        return new ApiAccessDeniedHandler(problems);
    }

    @Bean
    SecurityFilterChain apiSecurity(
            HttpSecurity http,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContexts,
            HttpSessionCsrfTokenRepository csrfRepository,
            CsrfTokenRequestHandler csrfRequestHandler,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler) throws Exception {
        http.authenticationManager(authenticationManager)
                .securityContext(context -> context
                        .securityContextRepository(securityContexts)
                        .requireExplicitSave(true))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .requestCache(cache -> cache
                        .requestCache(new NullRequestCache()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(csrfRequestHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/v1/csrf",
                                "/api/auth/v1/session").permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/v1/login").permitAll()
                        .requestMatchers(
                                "/api/admin/**",
                                "/api/auth/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
