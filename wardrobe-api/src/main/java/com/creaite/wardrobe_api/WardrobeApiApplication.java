package com.creaite.wardrobe_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class WardrobeApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(WardrobeApiApplication.class, args);
    }
}
