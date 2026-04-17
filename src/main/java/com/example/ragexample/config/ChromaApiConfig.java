package com.example.ragexample.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class ChromaApiConfig {

    @Bean
    public ChromaApi chromaApi(RestClient.Builder builder,
                               JsonMapper mapper) {

        return new ChromaApi(
                "http://localhost:8000",
                builder,
                mapper
        );
    }
}