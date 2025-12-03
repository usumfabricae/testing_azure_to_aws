package com.example;

import com.example.KeyVaultApp.DatabaseCredentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for KeyVaultApp that interact with actual AWS Secrets Manager.
 * These tests require:
 * 1. AWS credentials configured (via IAM role, environment variables, or AWS credentials file)
 * 2. Environment variable AWS_REGION set to the target region
 * 3. Appropriate IAM permissions: secretsmanager:CreateSecret, secretsmanager:GetSecretValue, secretsmanager:DeleteSecret
 * 
 * To run these tests: export AWS_INTEGRATION_TEST_ENABLED=true
 */
@EnabledIfEnvironmentVariable(named = "AWS_INTEGRATION_TEST_ENABLED", matches = "true")
class KeyVaultAppIntegrationTest {

    private static final String TEST_SECRET_NAME = "test-keyvault-app-integration-secret";
    private static final String TEST_REGION = System.getenv().getOrDefault("AWS_REGION", "us-east-1");

    @Test
    void testGetCredentials_RealAWSSecretsManager() throws Exception {
        SecretsManagerClient client = SecretsManagerClient.builder()
                .region(Region.of(TEST_REGION))
                .build();

        String secretValue = "{\"username\":\"integration-test-user\",\"password\":\"integration-test-pass\"}";

        try {
            // Create test secret
            CreateSecretRequest createRequest = CreateSecretRequest.builder()
                    .name(TEST_SECRET_NAME)
                    .secretString(secretValue)
                    .description("Integration test secret for KeyVaultApp")
                    .build();
            client.createSecret(createRequest);

            // Give AWS a moment to propagate the secret
            Thread.sleep(2000);

            // Create KeyVaultApp with custom client
            KeyVaultApp app = new KeyVaultApp(client);

            // Test credential retrieval
            DatabaseCredentials credentials = app.getCredentials(TEST_SECRET_NAME);

            // Assert
            assertNotNull(credentials);
            assertEquals("integration-test-user", credentials.getUsername());
            assertEquals("integration-test-pass", credentials.getPassword());

        } finally {
            // Cleanup: Delete the test secret
            try {
                DeleteSecretRequest deleteRequest = DeleteSecretRequest.builder()
                        .secretId(TEST_SECRET_NAME)
                        .forceDeleteWithoutRecovery(true)
                        .build();
                client.deleteSecret(deleteRequest);
            } catch (Exception e) {
                System.err.println("Warning: Failed to cleanup test secret: " + e.getMessage());
            }
        }
    }

    @Test
    void testGetCredentials_NonExistentSecret() {
        SecretsManagerClient client = SecretsManagerClient.builder()
                .region(Region.of(TEST_REGION))
                .build();

        KeyVaultApp app = new KeyVaultApp(client);

        // Test retrieval of non-existent secret
        assertThrows(ResourceNotFoundException.class, () -> {
            app.getCredentials("non-existent-secret-" + System.currentTimeMillis());
        });
    }

    @Test
    void testGetCredentials_ComplexJsonSecret() throws Exception {
        SecretsManagerClient client = SecretsManagerClient.builder()
                .region(Region.of(TEST_REGION))
                .build();

        String complexSecretName = "test-complex-secret-" + System.currentTimeMillis();
        String secretValue = "{\"username\":\"user@example.com\",\"password\":\"C0mpl3x!P@ssw0rd#2023\"}";

        try {
            // Create test secret with complex values
            CreateSecretRequest createRequest = CreateSecretRequest.builder()
                    .name(complexSecretName)
                    .secretString(secretValue)
                    .build();
            client.createSecret(createRequest);

            // Give AWS a moment to propagate the secret
            Thread.sleep(2000);

            // Create KeyVaultApp with custom client
            KeyVaultApp app = new KeyVaultApp(client);

            // Test credential retrieval
            DatabaseCredentials credentials = app.getCredentials(complexSecretName);

            // Assert
            assertNotNull(credentials);
            assertEquals("user@example.com", credentials.getUsername());
            assertEquals("C0mpl3x!P@ssw0rd#2023", credentials.getPassword());

        } finally {
            // Cleanup
            try {
                DeleteSecretRequest deleteRequest = DeleteSecretRequest.builder()
                        .secretId(complexSecretName)
                        .forceDeleteWithoutRecovery(true)
                        .build();
                client.deleteSecret(deleteRequest);
            } catch (Exception e) {
                System.err.println("Warning: Failed to cleanup test secret: " + e.getMessage());
            }
        }
    }
}
