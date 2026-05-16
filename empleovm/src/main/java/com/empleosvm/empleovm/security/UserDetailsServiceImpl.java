package com.empleosvm.empleovm.security;

import com.empleosvm.empleovm.model.entity.Usuario;
import com.empleosvm.empleovm.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        String rol = usuario.getTipo() != null ? usuario.getTipo().name() : "ROLE_USER";

        return new User(
                usuario.getEmail(),
                usuario.getPassword(), // ya va a estar hasheado con BCrypt
                List.of(new SimpleGrantedAuthority(rol))
        );
    }
}