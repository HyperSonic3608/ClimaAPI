package com.example.ClimaAPI.controller;

import com.example.ClimaAPI.service.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

Service service = new Service();

    @GetMapping("/clima")
    public String getClima() {
        return service.pegarTempoBH();
    }
}
