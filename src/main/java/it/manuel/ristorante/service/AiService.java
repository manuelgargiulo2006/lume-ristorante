package it.manuel.ristorante.service;

import it.manuel.ristorante.dto.AiPiatto;
import it.manuel.ristorante.dto.PiattoVenduto;
import it.manuel.ristorante.dto.Riepilogo;
import it.manuel.ristorante.model.Allergene;
import it.manuel.ristorante.model.Piatto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Funzioni di intelligenza artificiale (Groq tramite Spring AI). */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final ChatClient chatClient;
    private final boolean configurata;

    public AiService(ChatClient chatClient, @Value("${spring.ai.openai.api-key:}") String apiKey) {
        this.chatClient = chatClient;
        this.configurata = apiKey != null && !apiKey.isBlank() && !apiKey.equals("chiave-non-configurata");
        if (!configurata) {
            log.info("Chiave GROQ_API_KEY non impostata: le funzioni AI resteranno disattivate");
        }
    }

    /** true se è stata impostata una chiave Groq. */
    public boolean isConfigurata() {
        return configurata;
    }

    /** Descrizione da menù e abbinamento vino, restituiti come oggetto Java (structured output). */
    public AiPiatto testiPiatto(String nome, String categoria, String ingredienti, String descrizioneAttuale) {
        String prompt = """
                Scrivi i testi del menù per questo piatto.
                Nome: %s
                Categoria: %s
                Ingredienti principali: %s
                %s
                La descrizione deve far venire voglia di ordinarlo, citando gli ingredienti, senza aggettivi vuoti
                come "delizioso" o "squisito". L'abbinamento deve essere un vino reale, preferibilmente campano.
                """.formatted(nome, categoria,
                ingredienti == null || ingredienti.isBlank() ? "non indicati" : ingredienti,
                descrizioneAttuale == null || descrizioneAttuale.isBlank() ? ""
                        : "Descrizione attuale da migliorare: " + descrizioneAttuale);
        return chiama(() -> chatClient.prompt().user(prompt).call().entity(AiPiatto.class));
    }

    public AiPiatto testiPiatto(Piatto piatto) {
        String ingredienti = piatto.getRicetta().stream()
                .map(pi -> pi.getIngrediente().getNome())
                .collect(Collectors.joining(", "));
        if (!piatto.getAllergeni().isEmpty()) {
            ingredienti += (ingredienti.isEmpty() ? "" : "; ") + "allergeni: " + piatto.getAllergeni().stream()
                    .map(Allergene::getLabel).collect(Collectors.joining(", "));
        }
        return testiPiatto(piatto.getNome(), piatto.getCategoria().getNome(), ingredienti, piatto.getDescrizione());
    }

    /** Breve analisi delle vendite con consigli pratici per il titolare. */
    public String analisiVendite(Riepilogo riepilogo, List<PiattoVenduto> top, int giorni, String extra) {
        String classifica = top.stream()
                .map(p -> p.nome() + " (" + p.porzioni() + " porzioni)")
                .collect(Collectors.joining(", "));
        String prompt = """
                Ecco i dati degli ultimi %d giorni del ristorante:
                - incasso: %s euro (%s rispetto al periodo precedente)
                - conti: %d, coperti: %d
                - scontrino medio: %s euro, spesa media per coperto: %s euro
                - piatti più venduti: %s
                %s
                Scrivi un'analisi per il titolare in 3 punti brevi (massimo 2 frasi ciascuno), ognuno su una riga
                che inizia con "- ". Indica cosa va bene, cosa tenere d'occhio e un'azione concreta da fare
                questa settimana. Niente introduzioni né conclusioni.
                """.formatted(giorni, riepilogo.incasso(),
                riepilogo.variazione() == null ? "nessun confronto" : riepilogo.variazione() + "%",
                riepilogo.conti(), riepilogo.coperti(), riepilogo.scontrinoMedio(), riepilogo.spesaPerCoperto(),
                classifica.isEmpty() ? "nessuno" : classifica, extra == null ? "" : extra);
        return chiama(() -> chatClient.prompt().user(prompt).call().content());
    }

    private <T> T chiama(Supplier<T> richiesta) {
        if (!configurata) {
            throw new AiNonDisponibileException("Funzione AI non attiva: imposta la variabile d'ambiente GROQ_API_KEY "
                    + "con una chiave gratuita di console.groq.com e riavvia l'applicazione.");
        }
        T risultato;
        try {
            risultato = richiesta.get();
        } catch (RuntimeException e) {
            log.warn("Chiamata all'AI non riuscita: {}", e.getMessage());
            throw new AiNonDisponibileException(messaggio(e));
        }
        if (risultato == null) {
            throw new AiNonDisponibileException("L'AI ha restituito una risposta vuota. Riprova tra qualche secondo.");
        }
        return risultato;
    }

    private static String messaggio(RuntimeException e) {
        String m = String.valueOf(e.getMessage()).toLowerCase();
        if (m.contains("401") || m.contains("invalid_api_key") || m.contains("invalid api key")) {
            return "Chiave Groq mancante o non valida: impostala nella variabile d'ambiente GROQ_API_KEY e riavvia.";
        }
        if (m.contains("model_not_found") || m.contains("decommissioned") || m.contains("does not exist")) {
            return "Il modello AI non è più disponibile su Groq: scegline un altro su console.groq.com/docs/models "
                    + "e impostalo in GROQ_MODEL.";
        }
        if (m.contains("429") || m.contains("rate limit")) {
            return "Limite di richieste gratuite di Groq raggiunto: riprova tra un minuto.";
        }
        if (m.contains("connect") || m.contains("timed out") || m.contains("unknownhost") || m.contains("i/o error")) {
            return "Impossibile raggiungere Groq: controlla la connessione a internet.";
        }
        if (m.contains("json") || m.contains("deserializ") || m.contains("convert")) {
            return "L'AI ha risposto in un formato inatteso: riprova, di solito al secondo tentativo funziona.";
        }
        return "Il servizio di intelligenza artificiale non è disponibile in questo momento.";
    }

    /** L'AI non ha risposto: il messaggio è già pronto da mostrare. */
    public static class AiNonDisponibileException extends RuntimeException {
        public AiNonDisponibileException(String message) {
            super(message);
        }
    }
}
