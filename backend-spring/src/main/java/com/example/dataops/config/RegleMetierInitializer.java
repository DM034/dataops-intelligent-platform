package com.example.dataops.config;

import com.example.dataops.service.RegleMetierService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RegleMetierInitializer implements CommandLineRunner {
    private final RegleMetierService service;

    public RegleMetierInitializer(RegleMetierService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        service.ensureDefaultRules();
    }
}
