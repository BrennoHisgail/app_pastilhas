package br.com.ddametalurgica.estoque.domain;

/** Perfil de acesso do usuario (RF12, RF13). */
public enum PerfilUsuario {

    /** Acesso completo: cadastro/edicao/inativacao de pastilhas e gestao de usuarios. */
    ADMIN,

    /** So lanca entrada/saida de estoque e consulta as telas. */
    OPERADOR
}
