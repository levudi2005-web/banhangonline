package com.banhangonline.config;

import com.banhangonline.common.security.ErrorWriter;
import com.banhangonline.common.security.RateLimitFilter;
import com.banhangonline.common.security.RequestedWithFilter;
import com.banhangonline.auth.security.SessionAuthFilter;
import com.banhangonline.auth.service.SessionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        List<String> origins = props.cors() == null || props.cors().allowedOrigins() == null
                ? List.of() : props.cors().allowedOrigins().stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(origins);
        c.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        c.setAllowedHeaders(List.of("Content-Type", "X-Requested-With"));
        c.setAllowCredentials(true);
        c.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", c);
        return source;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, SessionService sessions, AppProperties props) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // thay bằng RequestedWithFilter (xem class đó)
            .cors(Customizer.withDefaults())
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .logout(l -> l.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives(
                    "default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'")))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/me").authenticated()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> ErrorWriter.write(res, 401, "UNAUTHENTICATED", "Bạn chưa đăng nhập"))
                .accessDeniedHandler((req, res, ex) -> ErrorWriter.write(res, 403, "FORBIDDEN", "Bạn không có quyền truy cập")))
            .addFilterBefore(new RateLimitFilter(props), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new RequestedWithFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new SessionAuthFilter(sessions, props), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
