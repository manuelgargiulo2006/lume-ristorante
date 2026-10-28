package it.manuel.ristorante.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Testi generati dall'AI per un piatto: Spring AI converte la risposta direttamente in questo oggetto. */
public record AiPiatto(
        @JsonPropertyDescription("Descrizione evocativa del piatto per il menù, al massimo 2 frasi e 220 caratteri")
        String descrizione,
        @JsonPropertyDescription("Un vino campano o italiano consigliato in abbinamento, con denominazione e una breve motivazione, al massimo 120 caratteri")
        String abbinamento) {
}
