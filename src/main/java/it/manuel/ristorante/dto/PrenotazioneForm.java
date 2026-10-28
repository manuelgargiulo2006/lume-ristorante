package it.manuel.ristorante.dto;

import it.manuel.ristorante.model.Prenotazione;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public class PrenotazioneForm {

    @NotBlank(message = "Inserisci il nome")
    @Size(max = 80, message = "Massimo 80 caratteri")
    private String nome;

    @NotBlank(message = "Inserisci un numero di telefono")
    @Pattern(regexp = "^$|^[+0-9 ()./-]{6,30}$", message = "Numero di telefono non valido")
    private String telefono;

    @Email(message = "Indirizzo email non valido")
    @Size(max = 120, message = "Massimo 120 caratteri")
    private String email;

    @NotNull(message = "Scegli la data")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate data;

    @NotNull(message = "Scegli l’orario")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime ora;

    @NotNull(message = "Indica il numero di persone")
    @Min(value = 1, message = "Almeno 1 persona")
    @Max(value = 12, message = "Per gruppi oltre 12 persone chiamaci al telefono")
    private Integer persone = 2;

    @Size(max = 400, message = "Massimo 400 caratteri")
    private String note;

    /** Solo dal gestionale. */
    private Long tavoloId;

    public static PrenotazioneForm da(Prenotazione p) {
        PrenotazioneForm f = new PrenotazioneForm();
        f.setNome(p.getNome());
        f.setTelefono(p.getTelefono());
        f.setEmail(p.getEmail());
        f.setData(p.getData());
        f.setOra(p.getOra());
        f.setPersone(p.getPersone());
        f.setNote(p.getNote());
        f.setTavoloId(p.getTavolo() == null ? null : p.getTavolo().getId());
        return f;
    }

    public String getNome() {
        return nome;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getData() {
        return data;
    }

    public LocalTime getOra() {
        return ora;
    }

    public Integer getPersone() {
        return persone;
    }

    public String getNote() {
        return note;
    }

    public Long getTavoloId() {
        return tavoloId;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public void setOra(LocalTime ora) {
        this.ora = ora;
    }

    public void setPersone(Integer persone) {
        this.persone = persone;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setTavoloId(Long tavoloId) {
        this.tavoloId = tavoloId;
    }
}
