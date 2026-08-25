package com.example.ClimaAPI.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WeatherService {

    private static final String API_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=-19.9208&longitude=-43.9378"
                    + "&current=temperature_2m,relative_humidity_2m,wind_speed_10m,wind_direction_10m,weather_code"
                    + "&daily=temperature_2m_max,temperature_2m_min,weather_code"
                    + "&timezone=America%2FSao_Paulo";
    private static final String CIDADE = "Belo Horizonte - MG";
    private static final ZoneId FUSO_HORARIO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATADOR_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Pattern NUMERO = Pattern.compile("-?\\d+(?:\\.\\d+)?");

    private final RestTemplate restTemplate;

    public WeatherService() {
        this(new RestTemplate());
    }

    WeatherService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> obterClimaBH() {
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(API_URL, String.class);

        if (!responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
            throw new ResponseStatusException(
                    responseEntity.getStatusCode(), "Falha ao obter dados meteorológicos.");
        }

        return formatarResposta(responseEntity.getBody());
    }

    Map<String, Object> formatarResposta(String json) {
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("localizacaoDaCidade", CIDADE);
        resposta.put("dataHoraConsulta", LocalDateTime.now(FUSO_HORARIO).format(FORMATADOR_DATA_HORA));

        String current = extrairObjeto(json, "current");
        if (current != null) {
            adicionarNumero(resposta, "temperaturaAtual", current, "temperature_2m", " °C");
            adicionarNumero(resposta, "umidadeDoAr", current, "relative_humidity_2m", " %");
            adicionarNumero(resposta, "velocidadeDoVento", current, "wind_speed_10m", " km/h");
            adicionarNumero(resposta, "direcaoDoVento", current, "wind_direction_10m", "°");

            Integer weatherCode = extrairInteiro(current, "weather_code");
            if (weatherCode != null) {
                adicionarTexto(resposta, "condicaoClimatica", descricaoCurta(weatherCode));
                adicionarTexto(resposta, "descricaoDasCondicoesDoTempo", descricaoDetalhada(weatherCode));
            }
        }

        String daily = extrairObjeto(json, "daily");
        if (daily != null) {
            adicionarNumeroArray(resposta, "temperaturaMaxima", daily, "temperature_2m_max", " °C");
            adicionarNumeroArray(resposta, "temperaturaMinima", daily, "temperature_2m_min", " °C");
        }

        return resposta;
    }

    private void adicionarNumero(Map<String, Object> resposta, String chave, String json, String campo, String sufixo) {
        Double numero = extrairNumero(json, campo);
        if (numero != null) {
            resposta.put(chave, numero + sufixo);
        }
    }

    private void adicionarNumeroArray(
            Map<String, Object> resposta, String chave, String json, String campo, String sufixo) {
        Double numero = extrairNumeroArrayPrimeiroElemento(json, campo);
        if (numero != null) {
            resposta.put(chave, numero + sufixo);
        }
    }

    private void adicionarTexto(Map<String, Object> resposta, String chave, String valor) {
        if (valor != null && !valor.isBlank()) {
            resposta.put(chave, valor);
        }
    }

    private String extrairObjeto(String json, String campo) {
        String marcador = "\"" + campo + "\"";
        int posicaoCampo = json.indexOf(marcador);
        if (posicaoCampo < 0) {
            return null;
        }

        int abreChave = json.indexOf('{', posicaoCampo);
        if (abreChave < 0) {
            return null;
        }

        int fechaChave = encontrarFimObjeto(json, abreChave);
        if (fechaChave < 0) {
            return null;
        }

        return json.substring(abreChave + 1, fechaChave);
    }

    private int encontrarFimObjeto(String json, int inicio) {
        int nivel = 0;
        boolean emString = false;
        boolean escape = false;

        for (int i = inicio; i < json.length(); i++) {
            char c = json.charAt(i);

            if (emString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    emString = false;
                }
                continue;
            }

            if (c == '"') {
                emString = true;
            } else if (c == '{') {
                nivel++;
            } else if (c == '}') {
                nivel--;
                if (nivel == 0) {
                    return i;
                }
            }
        }

        return -1;
    }

    private Double extrairNumero(String json, String campo) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(campo) + "\"\\s*:\\s*(" + NUMERO.pattern() + ")");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return Double.valueOf(matcher.group(1));
        }
        return null;
    }

    private Double extrairNumeroArrayPrimeiroElemento(String json, String campo) {
        Pattern pattern =
                Pattern.compile("\"" + Pattern.quote(campo) + "\"\\s*:\\s*\\[\\s*(" + NUMERO.pattern() + ")");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return Double.valueOf(matcher.group(1));
        }
        return null;
    }

    private Integer extrairInteiro(String json, String campo) {
        Double numero = extrairNumero(json, campo);
        return numero == null ? null : numero.intValue();
    }

    private String descricaoCurta(int codigo) {
        return switch (codigo) {
            case 0 -> "Céu limpo";
            case 1 -> "Predominantemente limpo";
            case 2 -> "Parcialmente nublado";
            case 3 -> "Nublado";
            case 45, 48 -> "Nevoeiro";
            case 51, 53, 55 -> "Garoa";
            case 61, 63, 65 -> "Chuva";
            case 66, 67 -> "Chuva congelante";
            case 71, 73, 75, 77 -> "Neve";
            case 80, 81, 82 -> "Pancadas de chuva";
            case 85, 86 -> "Pancadas de neve";
            case 95, 96, 99 -> "Trovoadas";
            default -> "Condição não informada";
        };
    }

    private String descricaoDetalhada(int codigo) {
        return switch (codigo) {
            case 0 -> "Sem nuvens e com visibilidade estável.";
            case 1 -> "Poucas nuvens no céu.";
            case 2 -> "Céu parcialmente coberto por nuvens.";
            case 3 -> "Céu encoberto.";
            case 45, 48 -> "Presença de neblina ou nevoeiro.";
            case 51, 53, 55 -> "Garoa em intensidade fraca a moderada.";
            case 61, 63, 65 -> "Chuvas em andamento.";
            case 66, 67 -> "Precipitação congelante.";
            case 71, 73, 75, 77 -> "Condições com neve ou precipitação sólida.";
            case 80, 81, 82 -> "Pancadas de chuva intermitentes.";
            case 85, 86 -> "Pancadas de neve intermitentes.";
            case 95, 96, 99 -> "Instabilidade com trovoadas.";
            default -> "Descrição meteorológica não disponível.";
        };
    }
}
