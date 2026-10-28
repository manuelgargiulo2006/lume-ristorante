package it.manuel.ristorante.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

import java.util.Locale;

@Configuration
public class WebConfig {

    /** Date, giorni e numeri sempre in italiano. */
    @Bean
    public LocaleResolver localeResolver() {
        return new FixedLocaleResolver(Locale.ITALY);
    }
}
