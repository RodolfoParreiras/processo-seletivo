package br.gov.pmps.processoseletivo.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    // A API só devolve JSON e arquivos; nada nela precisa carregar scripts, estilos ou ser embutido em frames.
    private static final String API_CONTENT_SECURITY_POLICY = "default-src 'none'; frame-ancestors 'none'";

    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // Deny by default (AI_RULES §73): cada endpoint novo precisa ser liberado explicitamente aqui.
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().denyAll())
                // Autenticação por sessão em cookie: CSRF com token em cookie lido pelo frontend e reenviado em header.
                .csrf(csrf -> csrf.spa())
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(API_CONTENT_SECURITY_POLICY))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
                        .frameOptions(frameOptions -> frameOptions.deny()));
        return http.build();
    }
}
