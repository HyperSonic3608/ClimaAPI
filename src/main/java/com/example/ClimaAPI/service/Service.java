package com.example.ClimaAPI.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Service {

    private final String apiUrl = "https://api.open-meteo.com/v1/forecast?latitude=-19.9208&longitude=-43.9378&hourly=temperature_2m,relative_humidity_2m,wind_speed_10m,wind_direction_10m,weather_code,precipitation_probability,precipitation&timezone=auto";

    public String pegarTempoBH() {

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(apiUrl, String.class);

        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            return formatarRespostaOpenMeteo(responseEntity.getBody());
        } else {
            return "Falha ao obter dados meteorológicos de BH. Código: " + responseEntity.getStatusCode();
        }

    }

    private String formatarRespostaOpenMeteo(String body) {
        if (body == null || body.isBlank()) {
            return "Falha ao formatar dados meteorológicos de BH. Resposta vazia.";
        }

        String[] horarios = extrairArray(body, "\"time\":[");
        String[] temperaturas = extrairArray(body, "\"temperature_2m\":[");
        String[] umidades = extrairArray(body, "\"relative_humidity_2m\":[");
        String[] velocidadesVento = extrairArray(body, "\"wind_speed_10m\":[");
        String[] direcoesVento = extrairArray(body, "\"wind_direction_10m\":[");
        String[] codigosTempo = extrairArray(body, "\"weather_code\":[");

        if (temperaturas.length == 0 || umidades.length == 0 || velocidadesVento.length == 0
                || direcoesVento.length == 0 || codigosTempo.length == 0) {
            return "Falha ao formatar dados meteorológicos de BH. Campos obrigatórios não encontrados.";
        }

        int indiceAtual = encontrarIndiceAtual(horarios);

        double temperaturaAtual = parseDoubleSeguro(temperaturas, indiceAtual);
        double umidadeAtual = parseDoubleSeguro(umidades, indiceAtual);
        double velocidadeVentoAtual = parseDoubleSeguro(velocidadesVento, indiceAtual);
        double direcaoVentoAtual = parseDoubleSeguro(direcoesVento, indiceAtual);
        int codigoTempoAtual = (int) parseDoubleSeguro(codigosTempo, indiceAtual);

        double[] extremos = calcularExtremosTemperatura(temperaturas, indiceAtual);
        double temperaturaMaxima = extremos[0];
        double temperaturaMinima = extremos[1];

        String[] condicaoDescricao = traduzirWeatherCode(codigoTempoAtual);

        String dataHoraConsulta = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        StringBuilder saida = new StringBuilder();
        saida.append("{\n");
        saida.append("  \"localizacaoDaCidade\": \"Belo Horizonte - MG\",\n");
        saida.append("  \"dataHoraConsulta\": \"").append(escapeJson(dataHoraConsulta)).append("\",\n");
        saida.append("  \"temperaturaAtual\": \"").append(formatarNumero(temperaturaAtual)).append(" °C\",\n");
        saida.append("  \"umidadeDoAr\": \"").append(formatarNumero(umidadeAtual)).append(" %\",\n");
        saida.append("  \"velocidadeDoVento\": \"").append(formatarNumero(velocidadeVentoAtual)).append(" km/h\",\n");
        saida.append("  \"direcaoDoVento\": \"").append(formatarNumero(direcaoVentoAtual)).append("°\",\n");
        saida.append("  \"condicaoClimatica\": \"").append(escapeJson(condicaoDescricao[0])).append("\",\n");
        saida.append("  \"descricaoDasCondicoesDoTempo\": \"").append(escapeJson(condicaoDescricao[1])).append("\",\n");
        saida.append("  \"temperaturaMaxima\": \"").append(formatarNumero(temperaturaMaxima)).append(" °C\",\n");
        saida.append("  \"temperaturaMinima\": \"").append(formatarNumero(temperaturaMinima)).append(" °C\"\n");
        saida.append("}");

        return saida.toString();
    }

    private String[] extrairArray(String body, String chaveComArray) {
        int inicio = body.indexOf(chaveComArray);
        if (inicio == -1) {
            return new String[0];
        }

        inicio += chaveComArray.length();
        int fim = body.indexOf("]", inicio);
        if (fim == -1) {
            return new String[0];
        }

        String conteudo = body.substring(inicio, fim).trim();
        if (conteudo.isEmpty()) {
            return new String[0];
        }

        String[] tokens = conteudo.split(",");
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = limparToken(tokens[i]);
        }

        return tokens;
    }

    private String limparToken(String token) {
        String limpo = token.trim();
        if (limpo.startsWith("\"") && limpo.endsWith("\"") && limpo.length() >= 2) {
            return limpo.substring(1, limpo.length() - 1);
        }
        return limpo;
    }

    private int encontrarIndiceAtual(String[] horarios) {
        if (horarios.length == 0) {
            return 0;
        }

        String horaAtual = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00"));

        for (int i = 0; i < horarios.length; i++) {
            if (horaAtual.equals(horarios[i])) {
                return i;
            }
        }

        return 0;
    }

    private double parseDoubleSeguro(String[] valores, int indicePreferencial) {
        if (valores.length == 0) {
            return 0.0;
        }

        int indice = Math.max(0, Math.min(indicePreferencial, valores.length - 1));

        try {
            return Double.parseDouble(valores[indice]);
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private double[] calcularExtremosTemperatura(String[] temperaturas, int indiceInicial) {
        if (temperaturas.length == 0) {
            return new double[]{0.0, 0.0};
        }

        int inicio = Math.max(0, Math.min(indiceInicial, temperaturas.length - 1));
        int fim = Math.min(temperaturas.length - 1, inicio + 23);

        double maxima = Double.NEGATIVE_INFINITY;
        double minima = Double.POSITIVE_INFINITY;

        for (int i = inicio; i <= fim; i++) {
            try {
                double valor = Double.parseDouble(temperaturas[i]);
                maxima = Math.max(maxima, valor);
                minima = Math.min(minima, valor);
            } catch (NumberFormatException ignored) {
                // Ignora valores inválidos e continua com os próximos.
            }
        }

        if (maxima == Double.NEGATIVE_INFINITY || minima == Double.POSITIVE_INFINITY) {
            double fallback = parseDoubleSeguro(temperaturas, inicio);
            return new double[]{fallback, fallback};
        }

        return new double[]{maxima, minima};
    }

    private String[] traduzirWeatherCode(int code) {
        return switch (code) {
            case 0 -> new String[]{"Céu limpo", "Sem nuvens no céu."};
            case 1, 2 -> new String[]{"Parcialmente nublado", "Poucas nuvens no céu."};
            case 3 -> new String[]{"Nublado", "Céu encoberto."};
            case 45, 48 -> new String[]{"Neblina", "Presença de neblina ou névoa."};
            case 51, 53, 55 -> new String[]{"Garoa", "Chuva fraca e contínua."};
            case 56, 57 -> new String[]{"Garoa congelante", "Garoa com possibilidade de congelamento."};
            case 61, 63, 65 -> new String[]{"Chuva", "Chuva com intensidade variável."};
            case 66, 67 -> new String[]{"Chuva congelante", "Chuva com possibilidade de congelamento."};
            case 71, 73, 75 -> new String[]{"Neve", "Queda de neve com intensidade variável."};
            case 77 -> new String[]{"Grãos de neve", "Precipitação de pequenos grãos de gelo."};
            case 80, 81, 82 -> new String[]{"Pancadas de chuva", "Pancadas de chuva ao longo do período."};
            case 85, 86 -> new String[]{"Pancadas de neve", "Pancadas de neve ao longo do período."};
            case 95 -> new String[]{"Trovoada", "Trovoadas na região."};
            case 96, 99 -> new String[]{"Trovoada com granizo", "Trovoadas com chance de granizo."};
            default -> new String[]{"Condição desconhecida", "Código meteorológico não mapeado."};
        };
    }

    private String formatarNumero(double valor) {
        return String.format(Locale.US, "%.1f", valor);
    }

    private String escapeJson(String valor) {
        return valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
