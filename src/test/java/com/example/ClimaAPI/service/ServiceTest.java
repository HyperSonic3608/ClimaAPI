package com.example.ClimaAPI.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ServiceTest {

    private final WeatherService weatherService = new WeatherService();

    @Test
    void formatarRespostaDeveManterSomenteCamposDisponiveis() {
        String json = """
                {
                  "current": {
                    "temperature_2m": 24.5,
                    "relative_humidity_2m": 63,
                    "wind_speed_10m": 14.2,
                    "wind_direction_10m": 270,
                    "weather_code": 3
                  },
                  "daily": {
                    "temperature_2m_max": [29.1],
                    "temperature_2m_min": [18.4]
                  }
                }
                """;

        Map<String, Object> resultado = weatherService.formatarResposta(json);

        assertEquals("Belo Horizonte - MG", resultado.get("localizacaoDaCidade"));
        assertTrue(((String) resultado.get("dataHoraConsulta")).matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}"));
        assertEquals("24.5 °C", resultado.get("temperaturaAtual"));
        assertEquals("63.0 %", resultado.get("umidadeDoAr"));
        assertEquals("14.2 km/h", resultado.get("velocidadeDoVento"));
        assertEquals("270.0°", resultado.get("direcaoDoVento"));
        assertEquals("Nublado", resultado.get("condicaoClimatica"));
        assertEquals("Céu encoberto.", resultado.get("descricaoDasCondicoesDoTempo"));
        assertEquals("29.1 °C", resultado.get("temperaturaMaxima"));
        assertEquals("18.4 °C", resultado.get("temperaturaMinima"));
    }

    @Test
    void formatarRespostaNaoDeveAdicionarCamposAusentes() {
        String json = """
                {
                  "current": {
                    "temperature_2m": 20
                  }
                }
                """;

        Map<String, Object> resultado = weatherService.formatarResposta(json);

        assertEquals("20.0 °C", resultado.get("temperaturaAtual"));
        assertFalse(resultado.containsKey("umidadeDoAr"));
        assertFalse(resultado.containsKey("velocidadeDoVento"));
        assertFalse(resultado.containsKey("direcaoDoVento"));
        assertFalse(resultado.containsKey("condicaoClimatica"));
        assertFalse(resultado.containsKey("descricaoDasCondicoesDoTempo"));
        assertFalse(resultado.containsKey("temperaturaMaxima"));
        assertFalse(resultado.containsKey("temperaturaMinima"));
        assertTrue(resultado.containsKey("localizacaoDaCidade"));
        assertTrue(resultado.containsKey("dataHoraConsulta"));
    }
}