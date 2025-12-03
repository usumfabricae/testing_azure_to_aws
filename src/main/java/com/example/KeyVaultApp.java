package com.example;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class KeyVaultApp {
    
    public KeyVaultApp(String keyVaultUrl) {
        try {
            SecretClient secretClient = new SecretClientBuilder()
                .vaultUrl(keyVaultUrl)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
            
            KeyVaultSecret secret = secretClient.getSecret("sql-db-credentials");
            DatabaseCredentials credentials = parseCredentials(secret.getValue());
            
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