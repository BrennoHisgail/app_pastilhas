package br.com.ddametalurgica.estoque.service;

import br.com.ddametalurgica.estoque.domain.Pastilha;
import br.com.ddametalurgica.estoque.domain.TipoMovimentacao;
import br.com.ddametalurgica.estoque.dto.IndicadorPastilhaResponse;
import br.com.ddametalurgica.estoque.dto.IndicadoresEstoqueResponse;
import br.com.ddametalurgica.estoque.repository.MovimentacaoRepository;
import br.com.ddametalurgica.estoque.repository.PastilhaRepository;
import br.com.ddametalurgica.estoque.repository.TotalMovimentado;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Calcula os indicadores de estoque do material da disciplina, sempre
// olhando as movimentacoes dos ultimos N dias:
//
//   consumo medio diario  d  = saidas do periodo / dias
//   Ponto de Pedido       PP = (d x lead time) + estoque de seguranca
//   Cobertura (dias)         = saldo atual / d
//   Giro                     = saidas do periodo / estoque medio
//   Curva ABC                = pastilhas ordenadas pelo valor consumido
//                              (saidas x custo); A ate 80% do total,
//                              B ate 95%, C o resto
@Service
public class IndicadorService {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal LIMITE_A = new BigDecimal("80");
    private static final BigDecimal LIMITE_B = new BigDecimal("95");

    private final PastilhaRepository pastilhaRepository;
    private final MovimentacaoRepository movimentacaoRepository;

    public IndicadorService(PastilhaRepository pastilhaRepository, MovimentacaoRepository movimentacaoRepository) {
        this.pastilhaRepository = pastilhaRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    @Transactional(readOnly = true)
    public IndicadoresEstoqueResponse calcular(int dias) {
        List<Pastilha> pastilhas = pastilhaRepository.findByAtivoTrueOrderByCodigoAsc();

        Map<Long, BigDecimal> entradas = new HashMap<>();
        Map<Long, BigDecimal> saidas = new HashMap<>();
        for (TotalMovimentado total : movimentacaoRepository.somarPorPastilhaDesde(
                LocalDateTime.now().minusDays(dias))) {
            if (total.tipo() == TipoMovimentacao.ENTRADA) {
                entradas.put(total.pastilhaId(), total.quantidade());
            } else if (total.tipo() == TipoMovimentacao.SAIDA) {
                saidas.put(total.pastilhaId(), total.quantidade());
            }
        }

        // Primeiro os numeros de cada pastilha, depois a Curva ABC,
        // que precisa do total consumido de todas
        List<Linha> linhas = new ArrayList<>();
        for (Pastilha p : pastilhas) {
            BigDecimal entrou = entradas.getOrDefault(p.getId(), BigDecimal.ZERO);
            BigDecimal saiu = saidas.getOrDefault(p.getId(), BigDecimal.ZERO);
            linhas.add(new Linha(p, entrou, saiu, dias));
        }

        linhas.sort(Comparator.comparing((Linha l) -> l.valorConsumido).reversed()
                .thenComparing(l -> l.pastilha.getCodigo()));

        BigDecimal totalConsumido = linhas.stream()
                .map(l -> l.valorConsumido)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<IndicadorPastilhaResponse> itens = new ArrayList<>();
        BigDecimal acumulado = BigDecimal.ZERO;
        long a = 0;
        long b = 0;
        long c = 0;
        long abaixoDoPp = 0;

        for (Linha l : linhas) {
            // A classe usa o percentual acumulado ANTES do item, assim
            // o item que mais consome sempre fica na classe A
            BigDecimal antes = percentual(acumulado, totalConsumido);
            acumulado = acumulado.add(l.valorConsumido);
            BigDecimal depois = percentual(acumulado, totalConsumido);

            String classe;
            if (l.valorConsumido.signum() == 0) {
                classe = "C";
            } else if (antes.compareTo(LIMITE_A) < 0) {
                classe = "A";
            } else if (antes.compareTo(LIMITE_B) < 0) {
                classe = "B";
            } else {
                classe = "C";
            }

            switch (classe) {
                case "A" -> a++;
                case "B" -> b++;
                default -> c++;
            }
            if (l.abaixoDoPontoDePedido()) {
                abaixoDoPp++;
            }

            itens.add(l.paraResposta(depois, classe));
        }

        return new IndicadoresEstoqueResponse(dias, a, b, c, abaixoDoPp, itens);
    }

    private static BigDecimal percentual(BigDecimal parte, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return parte.multiply(CEM).divide(total, 2, RoundingMode.HALF_UP);
    }

    // Os calculos de uma pastilha, antes de saber a classe ABC
    private static final class Linha {

        private final Pastilha pastilha;
        private final BigDecimal consumo;
        private final BigDecimal consumoDiario;
        private final BigDecimal pontoDePedido;
        private final BigDecimal coberturaDias;
        private final BigDecimal giro;
        private final BigDecimal valorConsumido;

        Linha(Pastilha p, BigDecimal entrou, BigDecimal saiu, int dias) {
            this.pastilha = p;
            this.consumo = saiu;
            this.consumoDiario = saiu.divide(BigDecimal.valueOf(dias), 3, RoundingMode.HALF_UP);

            this.pontoDePedido = consumoDiario
                    .multiply(BigDecimal.valueOf(p.getLeadTimeDias()))
                    .add(p.getEstoqueSeguranca())
                    .setScale(3, RoundingMode.HALF_UP);

            // Sem consumo o estoque "dura para sempre": deixamos nulo
            this.coberturaDias = consumoDiario.signum() == 0
                    ? null
                    : p.getSaldoAtual().divide(consumoDiario, 1, RoundingMode.HALF_UP);

            // O saldo no inicio do periodo e o atual desfazendo o que
            // entrou e saiu; o estoque medio e a media dos dois
            BigDecimal saldoInicial = p.getSaldoAtual().subtract(entrou).add(saiu);
            BigDecimal estoqueMedio = saldoInicial.add(p.getSaldoAtual())
                    .divide(BigDecimal.valueOf(2), 3, RoundingMode.HALF_UP);
            this.giro = estoqueMedio.signum() <= 0
                    ? null
                    : saiu.divide(estoqueMedio, 2, RoundingMode.HALF_UP);

            this.valorConsumido = saiu.multiply(p.getCustoUnitario()).setScale(2, RoundingMode.HALF_UP);
        }

        // So faz sentido avisar quando a pastilha tem consumo; sem consumo
        // o PP e so o estoque de seguranca e o alerta do nivel minimo ja cobre
        boolean abaixoDoPontoDePedido() {
            return consumo.signum() > 0 && pastilha.getSaldoAtual().compareTo(pontoDePedido) <= 0;
        }

        IndicadorPastilhaResponse paraResposta(BigDecimal percentualAcumulado, String classe) {
            return new IndicadorPastilhaResponse(
                    pastilha.getId(),
                    pastilha.getCodigo(),
                    pastilha.getDescricao(),
                    pastilha.getUnidadeMedida(),
                    pastilha.getSaldoAtual(),
                    consumo,
                    consumoDiario,
                    pontoDePedido,
                    abaixoDoPontoDePedido(),
                    coberturaDias,
                    giro,
                    valorConsumido,
                    percentualAcumulado,
                    classe);
        }
    }
}
