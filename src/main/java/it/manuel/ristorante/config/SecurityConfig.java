package it.manuel.ristorante.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Le chiamate JavaScript alle API ricevono 401 invece del redirect alla pagina di login. */
    private static final RequestMatcher API =
            request -> request.getRequestURI().startsWith(request.getContextPath() + "/api/");

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // sito pubblico per i clienti
                .requestMatchers("/", "/menu", "/prenota/**", "/prenotazione/**", "/api/public/**",
                        "/login", "/error").permitAll()
                .requestMatchers("/css/**", "/js/**", "/img/**", "/fonts/**").permitAll()
                // sezioni riservate all'amministratore
                .requestMatchers("/gestione/staff/**", "/gestione/menu/**", "/gestione/statistiche/**",
                        "/gestione/tavoli/**", "/api/gestione/tavoli/**", "/api/gestione/ai/**").hasRole("ADMIN")
                // magazzino: amministratore e cucina
                .requestMatchers("/gestione/magazzino/**").hasAnyRole("ADMIN", "CUCINA")
                // sala, comande e prenotazioni: amministratore e camerieri
                .requestMatchers("/gestione/sala/**", "/gestione/ordini/**", "/gestione/prenotazioni/**",
                        "/api/gestione/sala/**").hasAnyRole("ADMIN", "SALA")
                // cucina e dashboard: tutto lo staff
                .requestMatchers("/gestione/**", "/api/gestione/**").authenticated()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(successHandler())
                .failureUrl("/login?errore")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?uscito")
                .permitAll())
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), API));
        return http.build();
    }

    /** Dopo il login la cucina va direttamente al monitor degli ordini, gli altri alla dashboard. */
    private AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            boolean cucina = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_CUCINA"));
            response.sendRedirect(request.getContextPath() + (cucina ? "/gestione/cucina" : "/gestione"));
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
