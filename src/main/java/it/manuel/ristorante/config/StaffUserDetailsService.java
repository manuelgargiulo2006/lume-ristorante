package it.manuel.ristorante.config;

import it.manuel.ristorante.repository.StaffRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class StaffUserDetailsService implements UserDetailsService {

    private final StaffRepository staffRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        return staffRepository.findByUsernameIgnoreCase(username.trim())
                .map(StaffUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username));
    }

    public StaffUserDetailsService(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }
}
