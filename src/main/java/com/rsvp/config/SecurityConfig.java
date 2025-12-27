package com.rsvp.config;

import com.rsvp.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserRepository userRepository;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, UserRepository userRepository) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userRepository = userRepository;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ========== PUBLIC ENDPOINTS (MUST BE FIRST) ==========
                        .requestMatchers("/favicon.ico").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()

                        // ✅ PUBLIC RSVP PAGE ENDPOINTS
                        .requestMatchers(HttpMethod.GET, "/api/events/*/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/guests/by-guest-id/**").permitAll()

                        // ✅ PUBLIC RSVP SUBMISSION & CHECKING
                        .requestMatchers(HttpMethod.POST, "/api/rsvps/submit").permitAll()
                        .requestMatchers("/api/rsvps/submit-group").permitAll()  // ✅ Add this

                        .requestMatchers(HttpMethod.GET, "/api/rsvps/event/*/guest-id/*/latest").permitAll()

                        // ✅ PUBLIC TRACKING
                        .requestMatchers(HttpMethod.POST, "/api/tracking/**").permitAll()

                        // Group RSVP sync (if still needed)
                        .requestMatchers(HttpMethod.POST, "/api/rsvps/group-sync").permitAll()

                        // ========== SYSTEM-ONLY ENDPOINTS ==========
                        .requestMatchers(HttpMethod.POST, "/api/rsvps/sync").hasRole("SYSTEM")
                        .requestMatchers(HttpMethod.POST, "/api/whatsapp/sync").hasRole("SYSTEM")

                        // ========== EVENT MANAGEMENT (ADMIN & USER) ==========
                        .requestMatchers(HttpMethod.POST, "/api/events").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.PUT, "/api/events/*/close").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.PUT, "/api/events/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/events/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.GET, "/api/events/**").hasAnyRole("ADMIN", "USER")

                        // ========== GUEST OPERATIONS ==========
                        .requestMatchers(HttpMethod.POST, "/api/guests/import").hasAnyRole("ADMIN", "USER")
                        .requestMatchers("/api/guests/**").hasAnyRole("ADMIN", "USER")

                        // ========== DASHBOARD ==========
                        .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "USER")

                        // ========== ALL OTHER REQUESTS ==========
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:5173",
                "${CORS_ORIGINS:https://your-app.vercel.app,http://localhost:5173}",
                "https://www.yourdomain.com"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}