package com.example.ragexample.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChromaConfig {

    @Value("${chroma.tenant-name}")
    private String tenantName;

    @Value("${chroma.database-name}")
    private String databaseName;

    @Value("${chroma.collection-name}")
    private String collectionName;

    public String getTenantName() { return tenantName; }
    public String getDatabaseName() { return databaseName; }
    public String getCollectionName() { return collectionName; }
}