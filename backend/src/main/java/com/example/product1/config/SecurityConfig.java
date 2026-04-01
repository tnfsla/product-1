  package com.example.product1.config;

  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;
  import org.springframework.http.HttpMethod;
  import org.springframework.security.config.Customizer;
  import org.springframework.security.config.annotation.web.builders.HttpSecurity;
  import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
  import org.springframework.security.web.SecurityFilterChain;
  import org.springframework.web.cors.CorsConfiguration;
  import org.springframework.web.cors.CorsConfigurationSource;
  import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

  import java.util.List;

  @Configuration
  @EnableWebSecurity
  public class SecurityConfig {

      @Bean
      public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
          http
              .cors(Customizer.withDefaults())
              .csrf(csrf -> csrf.disable())
              .authorizeHttpRequests(auth -> auth
                  .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                  .requestMatchers(
                      "/",
                      "/index.html",
                      "/main.js",
                      "/style.css",
                      "/api/login",
                      "/api/generate-hash",
                      "/api/parse",
                      "/api/save",
                      "/api/results"
                  ).permitAll()
                  .anyRequest().authenticated()
              );
          return http.build();
      }

      @Bean
      public CorsConfigurationSource corsConfigurationSource() {
          CorsConfiguration config = new CorsConfiguration();
          config.addAllowedOriginPattern("*");
          config.addAllowedMethod("*");
          config.addAllowedHeader("*");
          config.setMaxAge(3600L);
          UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
          source.registerCorsConfiguration("/**", config);
          return source;
      }
  }