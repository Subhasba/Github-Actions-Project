package com.example.ragexample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreAutoConfiguration;

@SpringBootApplication(
		exclude = {ChromaVectorStoreAutoConfiguration.class}
)
public class RagexampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(RagexampleApplication.class, args);
	}
}