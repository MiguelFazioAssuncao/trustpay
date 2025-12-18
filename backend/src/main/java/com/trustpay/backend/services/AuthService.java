package com.trustpay.backend.services;

/**
 * Serviço responsável por gerenciar autenticação e registro de usuários.
 */
public interface AuthService {

    /**
     * Autentica um usuário com email e senha.
     *
     * @param email O email do usuário
     * @param password A senha do usuário
     * @return Token JWT para autenticação
     * @throws org.springframework.security.authentication.BadCredentialsException Se as credenciais forem inválidas
     */
    String login(String email, String password);

    /**
     * Registra um novo usuário no sistema.
     *
     * @param email O email do novo usuário (deve ser único)
     * @param password A senha do novo usuário
     * @return Token JWT para o novo usuário autenticado
     * @throws org.springframework.dao.DataIntegrityViolationException Se o email já estiver em uso
     */
    String register(String email, String password);
}
