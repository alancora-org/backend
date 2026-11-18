package org.alancora.service;

import java.util.ArrayList;
import java.util.List;

import org.alancora.model.Emergency;
import org.alancora.model.HousingType;
import org.alancora.model.Orientation;
import org.alancora.model.User;

public final class PlanEngine {
    public static final int VERSION = 2;

    private static final String FLOOD = "https://www.defesacivil.rs.gov.br/saiba-como-se-prevenir-em-situacoes-de-risco";
    private static final String FIRE = "https://www.seguranca.pr.gov.br/Noticia/Corpo-de-Bombeiros-orienta-moradores-de-apartamentos-como-agir-em-casos-de-incendio";
    private static final String POWER = "https://portais.santoandre.sp.gov.br/defesacivil/queda-de-energia/";
    private static final String SUPPORT = "https://www.acgov.org/ready/disability.htm";

    public static List<Orientation> generate(final User user, final Emergency emergency) {
        final HousingType type = HousingType.parse(user.getHousingType());
        if (user.getLocation().isBlank()) {
            throw new IllegalArgumentException("Salve sua localização antes de gerar um plano.");
        }

        final List<Orientation> items = new ArrayList<>();

        final String source = switch (emergency) {
            case ENCHENTE -> FLOOD;
            case INCENDIO -> FIRE;
            case FALTA_ENERGIA -> POWER;
        };

        final String action = switch (emergency) {
            case ENCHENTE -> "Acompanhe os avisos oficiais para " + user.getLocation()
                    + ". Planeje um caminho que evite áreas alagadas; não atravesse água de enchente.";
            case INCENDIO -> "Em caso de incêndio, priorize sair com segurança e acione os Bombeiros"
                    + " pelo 193 de um local seguro.";
            case FALTA_ENERGIA -> "Separe lanternas, pilhas e água potável. Mantenha o celular carregado"
                    + " e consulte os avisos da distribuidora que atende " + user.getLocation()
                    + ".";
        };

        items.add(new Orientation(emergency.name() + "_PREPARACAO",
                "Emergência selecionada: " + emergency.getName() + "; localização: "
                        + user.getLocation(),
                action, source));

        items.add(new Orientation(emergency.name() + "_MORADIA_" + type.name(),
                "Tipo de moradia: " + type.getName() + "; emergência: " + emergency.getName(),
                home(type, emergency), source));

        for (final String need : user.getNeeds().stream().distinct().sorted().toList()) {
            final String text = switch (need) {
                case "MOBILIDADE_REDUZIDA" -> "Planeje com sua rede de apoio uma rota acessível e"
                        + " mantenha os recursos de mobilidade disponíveis para um deslocamento seguro.";
                case "DEPENDENCIA_ENERGIA" -> "Combine com a equipe de saúde uma alternativa segura"
                        + " para seus equipamentos se faltar energia. Verifique a autonomia das baterias"
                        + " e mantenha os contatos de assistência acessíveis.";
                case "ASSISTENCIA_COMUNICACAO" -> "Combine com alguém de confiança como receber"
                        + " avisos e pedir ajuda em um formato de comunicação acessível para você.";
                case "DIFICULDADE_ESCADAS" -> stairs(type, emergency);
                case "APOIO_DESLOCAMENTO" -> "Combine previamente quem poderá ajudar no deslocamento"
                        + " e como acionar essa pessoa durante a emergência.";
                case "DIFICULDADE_CARREGAR_OBJETOS" -> "Separe os itens essenciais em um volume que"
                        + " consiga manusear e combine ajuda para transportar o que for necessário.";
                case "SEM_TRANSPORTE" -> "Combine com sua rede de apoio ou com os serviços locais"
                        + " uma opção de transporte acessível caso precise sair de casa.";
                default -> throw new IllegalArgumentException(
                        "Ainda não há orientações para uma das necessidades selecionadas.");
            };

            items.add(new Orientation(emergency.name() + "_" + need,
                    "Necessidade cadastrada: " + need + "; emergência: " + emergency.getName()
                            + "; tipo de moradia: " + type.getName(),
                    text, need.equals("DEPENDENCIA_ENERGIA") ? POWER : SUPPORT));
        }

        return List.copyOf(items);
    }

    private static String home(final HousingType type, final Emergency emergency) {
        return switch (emergency) {
            case ENCHENTE -> switch (type) {
                case CASA_TERREA -> "Na casa térrea, proteja documentos e itens essenciais da água,"
                        + " mantendo-os acessíveis. Planeje uma saída para um ponto seguro indicado pela Defesa Civil.";
                case SOBRADO ->
                    "No sobrado, mantenha documentos e itens essenciais em local elevado e acessível."
                            + " Planeje também a saída da casa e siga as orientações da Defesa Civil para deixar o local.";
                case APARTAMENTO -> "Combine com o condomínio como sair do prédio por um acesso seguro"
                        + " e acompanhe as orientações da Defesa Civil. Evite garagens e outras áreas alagadas.";
                case OUTRO -> "Identifique os acessos da moradia que podem ser atingidos pela água."
                        + " Consulte a Defesa Civil sobre áreas de risco e pontos seguros antes de planejar a saída.";
            };

            case INCENDIO -> switch (type) {
                case CASA_TERREA -> "Identifique as portas de saída da casa térrea e mantenha o caminho"
                        + " dos cômodos até a área externa livre de obstáculos.";
                case SOBRADO ->
                    "No sobrado, conheça o caminho de saída de cada piso e mantenha as escadas"
                            + " e os corredores desobstruídos. Combine como todos poderão sair com segurança.";
                case APARTAMENTO ->
                    "Conheça o caminho do apartamento até a saída e, se houver, as escadas"
                            + " de emergência do prédio. Mantenha as áreas de passagem livres e não use elevadores em um incêndio.";
                case OUTRO ->
                    "Identifique as saídas disponíveis na sua moradia e mantenha os acessos livres."
                            + " Se houver elevadores, não os use durante um incêndio.";
            };

            case FALTA_ENERGIA -> switch (type) {
                case CASA_TERREA ->
                    "Deixe lanternas em pontos acessíveis da casa térrea, perto dos caminhos"
                            + " entre os cômodos e a saída. Mantenha essas passagens livres de obstáculos.";
                case SOBRADO -> "Deixe lanternas acessíveis em cada piso do sobrado. Antes de usar as"
                        + " escadas, garanta iluminação suficiente para ver os degraus.";
                case APARTAMENTO -> "Combine com o condomínio como serão iluminadas as áreas comuns"
                        + " e como pedir ajuda se os elevadores ficarem indisponíveis. Planeje o acesso ao apartamento.";
                case OUTRO ->
                    "Verifique quais acessos da moradia dependem de energia e combine alternativas"
                            + " seguras. Deixe lanternas ao alcance para circular pelos espaços que você utiliza.";
            };
        };
    }

    private static String stairs(final HousingType type, final Emergency emergency) {
        return switch (type) {
            case CASA_TERREA -> "Mesmo na casa térrea, verifique se há degraus na entrada. Combine com"
                    + " sua rede de apoio como passar por eles com segurança, caso precise sair.";
            case SOBRADO -> "Combine previamente com sua rede de apoio como se deslocar entre os pisos"
                    + " do sobrado e sair com segurança, considerando sua dificuldade com escadas.";
            case APARTAMENTO -> emergency == Emergency.INCENDIO
                    ? "Combine previamente com a rede de apoio e os Bombeiros um plano de saída do prédio"
                            + " adequado à sua dificuldade com escadas. Não use elevadores durante um incêndio."
                    : "Combine com sua rede de apoio como acessar o apartamento se houver escadas"
                            + " e os elevadores estiverem indisponíveis. Planeje a ajuda necessária antes da emergência.";
            case OUTRO -> "Verifique se há degraus ou escadas no caminho de saída e combine com sua rede"
                    + " de apoio a ajuda necessária para se deslocar com segurança.";
        };
    }
}
