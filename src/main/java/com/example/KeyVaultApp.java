package com.example;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class KeyVaultApp {
    
    public KeyVaultApp(String region) {
        try {
            SecretsManagerClient secretsClient = SecretsManagerClient.builder()
                .region(Region.of(region))
                .build();
            
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId("sql-db-credentials")
                .build();
            GetSecretValueResponse response = secretsClient.getSecretValue(request);
            DatabaseCredentials credentials = parseCredentials(response.secretString());
            
            System.out.println("Database credentials retrieved successfully:");
            System.out.println("Username: " + credentials.getUsername());
            System.out.println("Password: [HIDDEN]");
            
        } catch (Exception e) {
            System.err.println("Error retrieving credentials: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private DatabaseCredentials parseCredentials(String secretValue) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(secretValue);
        
        String username = node.get("username").asText();
        String password = node.get("password").asText();
        
        return new DatabaseCredentials(username, password);
    }
    
    static class DatabaseCredentials {
        private final String username;
        private final String password;
        
        public DatabaseCredentials(String username, String password) {
            this.username = username;
            this.password = password;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getPassword() {
            return password;
        }
    }
}