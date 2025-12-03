package com.example;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;
import software.amazon.awssdk.services.secretsmanager.model.InvalidRequestException;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class KeyVaultApp {
    
    private final SecretsManagerClient secretsClient;
    private final boolean autoRun;
    
    public KeyVaultApp(String region) {
        this(SecretsManagerClient.builder()
            .region(Region.of(region))
            .build(), true);
    }
    
    // Constructor for dependency injection (testing)
    public KeyVaultApp(SecretsManagerClient secretsClient) {
        this(secretsClient, false);
    }
    
    // Private constructor with autoRun control
    private KeyVaultApp(SecretsManagerClient secretsClient, boolean autoRun) {
        this.secretsClient = secretsClient;
        this.autoRun = autoRun;
        if (autoRun) {
            retrieveAndDisplayCredentials();
        }
    }
    
    private void retrieveAndDisplayCredentials() {
        try {
            DatabaseCredentials credentials = getCredentials("sql-db-credentials");
            
            System.out.println("Database credentials retrieved successfully:");
            System.out.println("Username: " + credentials.getUsername());
            System.out.println("Password: [HIDDEN]");
            
        } catch (ResourceNotFoundException e) {
            System.err.println("Error: Secret not found in AWS Secrets Manager: " + e.getMessage());
            System.exit(1);
        } catch (InvalidRequestException e) {
            System.err.println("Error: Invalid request to AWS Secrets Manager: " + e.getMessage());
            System.exit(1);
        } catch (SecretsManagerException e) {
            System.err.println("Error: AWS Secrets Manager error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error retrieving credentials: " + e.getMessage());
            System.exit(1);
        }
    }
    
    public DatabaseCredentials getCredentials(String secretId) throws Exception {
        GetSecretValueRequest request = GetSecretValueRequest.builder()
            .secretId(secretId)
            .build();
        GetSecretValueResponse response = secretsClient.getSecretValue(request);
        return parseCredentials(response.secretString());
    }
    
    public DatabaseCredentials parseCredentials(String secretValue) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(secretValue);
        
        String username = node.get("username").asText();
        String password = node.get("password").asText();
        
        return new DatabaseCredentials(username, password);
    }
    
    public static class DatabaseCredentials {
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