package it.manuel.ristorante.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Client di Spring AI collegato a Groq (vedi application.properties). */
@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        Sei il maître e sommelier di Lume, ristorante gourmet di cucina campana contemporanea a Napoli.
                        Scrivi sempre in italiano, con tono elegante ma concreto, senza esagerazioni.
                        Quando ti viene chiesto un formato JSON rispondi solo con il JSON richiesto.
                        """)
                .build();
    }
}
