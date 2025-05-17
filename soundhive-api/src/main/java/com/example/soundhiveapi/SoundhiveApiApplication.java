package com.example.soundhiveapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.soundhiveapi", "org.springframework.di"})
// Scans for components, services, and configs in these packages

public class SoundhiveApiApplication {

	/**
	 * Main method: entry point of the Spring Boot application.
	 * Initializes the entire application context and starts the web server.
	 */
	public static void main(String[] args) {
		SpringApplication.run(SoundhiveApiApplication.class, args);
	}
}
