package br.com.ddametalurgica.estoque.domain;

// Tipo do lancamento de estoque (RF05, RF06)
public enum TipoMovimentacao {

    // Recebimento ou compra: soma ao saldo
    ENTRADA,

    // Consumo na producao: subtrai do saldo
    SAIDA,

    // Correcao de inventario. Reservado: previsto no banco, ainda sem endpoint
    AJUSTE
}
