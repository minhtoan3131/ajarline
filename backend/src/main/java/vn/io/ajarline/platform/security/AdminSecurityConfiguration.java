package vn.io.ajarline.platform.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.List;
import java.util.function.Supplier;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;
import vn.io.ajarline.platform.web.ApiProblemWriter;

@Configuration(proxyBeanMethods = false)
public class AdminSecurityConfiguration {

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
            AdminCredentials credentials, PasswordEncoder encoder) {
        return new AdminAuthenticationProvider(credentials, encoder, Clock.systemUTC());
    }

    @Bean
    AuthenticationManager adminAuthenticationManager(AdminAuthenticationProvider provider) {
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
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-CSRF-TOKEN");
        return repository;
    }

    @Bean
    SessionAuthenticationStrategy adminSessionAuthenticationStrategy(
            HttpSessionCsrfTokenRepository csrfRepository) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(),
                new CsrfAuthenticationStrategy(csrfRepository)));
    }

    @Bean
    SecurityFilterChain apiSecurity(
            HttpSecurity http, AuthenticationManager manager,
            SecurityContextRepository contexts, HttpSessionCsrfTokenRepository csrfRepository,
            ApiProblemWriter problems) throws Exception {
        XorCsrfTokenRequestAttributeHandler xorHandler =
                new XorCsrfTokenRequestAttributeHandler();
        CsrfTokenRequestHandler tokenHandler =
                new CsrfTokenRequestHandler() {
                    @Override
                    public void handle(
                            HttpServletRequest request,
                            jakarta.servlet.http.HttpServletResponse response,
                            Supplier<CsrfToken> token) {
                        xorHandler.handle(request, response, token);
                    }

                    @Override
                    public String resolveCsrfTokenValue(
                            HttpServletRequest request, CsrfToken token) {
                        if (request.getHeader(token.getHeaderName()) == null) {
                            return null;
                        }
                        return xorHandler.resolveCsrfTokenValue(request, token);
                    }
                };

        http.authenticationManager(manager)
                .securityContext(context -> context
                        .securityContextRepository(contexts)
                        .requireExplicitSave(true))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(tokenHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/auth/v1/csrf", "/api/auth/v1/session").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/v1/login").permitAll()
                        .requestMatchers("/api/admin/**", "/api/auth/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                problems.write(request, response, 401,
                                        "AUTHENTICATION_REQUIRED", "Authentication required",
                                        "Authentication required."))
                        .accessDeniedHandler((request, response, exception) -> {
                            if (exception instanceof CsrfException) {
                                problems.write(request, response, 403, "CSRF_INVALID",
                                        "CSRF proof is missing or invalid",
                                        "CSRF proof is missing or invalid.");
                            } else {
                                problems.write(request, response, 500, "INTERNAL_ERROR",
                                        "Unexpected internal failure",
                                        "Unexpected internal failure.");
                            }
                        }))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
