package br.com.ddametalurgica.estoque.domain;

// Perfil de acesso do usuario (RF12, RF13)
public enum PerfilUsuario {

    // Acesso completo: cadastro de pastilhas e gestao de usuarios
    ADMIN,

    // Lanca entradas e saidas e consulta as telas
    OPERADOR
}
