package com.example.ClimaAPI.controller;

import com.example.ClimaAPI.service.WeatherService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

    private final WeatherService weatherService;

    public Controller(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/clima")
    public Map<String, Object> getClima() {
        return weatherService.obterClimaBH();
    }
}
