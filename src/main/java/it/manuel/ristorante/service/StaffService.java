package it.manuel.ristorante.service;

import it.manuel.ristorante.dto.StaffForm;
import it.manuel.ristorante.model.Ruolo;
import it.manuel.ristorante.model.Staff;
import it.manuel.ristorante.repository.StaffRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StaffService {

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<Staff> tutti() {
        return staffRepository.findAllByOrderByRuoloAscNomeCompletoAsc();
    }

    @Transactional(readOnly = true)
    public Staff staff(Long id) {
        return staffRepository.findById(id).orElseThrow(() -> new NonTrovatoException("Utente non trovato"));
    }

    @Transactional(readOnly = true)
    public List<Staff> camerieri() {
        return tutti().stream().filter(s -> s.isAttivo() && s.getRuolo() != Ruolo.CUCINA).toList();
    }

    @Transactional
    public Staff salva(Long id, StaffForm form, Long utenteCorrenteId) {
        String username = form.getUsername().trim().toLowerCase();
        Staff staff;
        if (id == null) {
            if (staffRepository.existsByUsernameIgnoreCase(username)) {
                throw new OperazioneNonValidaException("Il nome utente \"" + username + "\" è già in uso");
            }
            if (form.getPassword() == null || form.getPassword().length() < 8) {
                throw new OperazioneNonValidaException("La password deve avere almeno 8 caratteri");
            }
            staff = new Staff();
            staff.setUsername(username);
        } else {
            staff = staff(id);
            if (!staff.getUsername().equalsIgnoreCase(username) && staffRepository.existsByUsernameIgnoreCase(username)) {
                throw new OperazioneNonValidaException("Il nome utente \"" + username + "\" è già in uso");
            }
            boolean perdeAdmin = staff.getRuolo() == Ruolo.ADMIN
                    && (form.getRuolo() != Ruolo.ADMIN || !form.isAttivo());
            if (perdeAdmin && staffRepository.countByRuoloAndAttivoTrue(Ruolo.ADMIN) <= 1) {
                throw new OperazioneNonValidaException("Deve restare almeno un amministratore attivo");
            }
            if (id.equals(utenteCorrenteId) && !form.isAttivo()) {
                throw new OperazioneNonValidaException("Non puoi disattivare il tuo stesso account");
            }
            staff.setUsername(username);
            if (form.getPassword() != null && !form.getPassword().isBlank()) {
                if (form.getPassword().length() < 8) {
                    throw new OperazioneNonValidaException("La password deve avere almeno 8 caratteri");
                }
            }
        }
        staff.setNomeCompleto(Testi.nome(form.getNomeCompleto()));
        staff.setRuolo(form.getRuolo());
        staff.setAttivo(form.isAttivo());
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            staff.setPassword(passwordEncoder.encode(form.getPassword()));
        }
        return staffRepository.save(staff);
    }

    public StaffService(StaffRepository staffRepository, PasswordEncoder passwordEncoder) {
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
    }
}
