package com.chessnalysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChessNalysisBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChessNalysisBackendApplication.class, args);
	}

}
