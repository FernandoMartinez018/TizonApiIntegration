package com.lfmm.tizon_integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TizonIntegrationApplication {

	public static void main(String[] args) {
		SpringApplication.run(TizonIntegrationApplication.class, args);
	}

}
