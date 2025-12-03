package com.example;

import com.example.KeyVaultApp.DatabaseCredentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;
import software.amazon.awssdk.services.secretsmanager.model.InvalidRequestException;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeyVaultAppTest {

    @Mock
    private SecretsManagerClient mockSecretsClient;

    @Test
    void testParseCredentials_Success() throws Exception {
        // Arrange
        String secretJson = "{\"username\":\"dbuser\",\"password\":\"dbpass123\"}";
        KeyVaultApp keyVaultApp = new KeyVaultApp(mockSecretsClient);

        // Act
        DatabaseCredentials credentials = keyVaultApp.parseCredentials(secretJson);

        // Assert
        assertNotNull(credentials);
        assertEquals("dbuser", credentials.getUsername());
        assertEquals("dbpass123", credentials.getPassword());
    }

    @Test
    void testParseCredentials_InvalidJson() {
        // Arrange
        String invalidJson = "{invalid json}";
        KeyVaultApp keyVaultApp = new KeyVaultApp(mockSecretsClient);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            keyVaultApp.parseCredentials(invalidJson);
        });
    }

    @Test
    void testParseCredentials_MissingUsername() {
        // Arrange
        String jsonWithoutUsername = "{\"password\":\"dbpass123\"}";
        KeyVaultApp keyVaultApp = new KeyVaultApp(mockSecretsClient);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            keyVaultApp.parseCredentials(jsonWithoutUsername);
        });
    }

    @Test
    void testParseCredentials_MissingPassword() {
        // Arrange
        String jsonWithoutPassword = "{\"username\":\"dbuser\"}";
        KeyVaultApp keyVaultApp = new KeyVaultApp(mockSecretsClient);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            keyVaultApp.parseCredentials(jsonWithoutPassword);
        });
    }

    @Test
    void testGetCredentials_Success() throws Exception {
        // Arrange
        String secretJson = "{\"username\":\"testuser\",\"password\":\"testpass\"}";
        GetSecretValueResponse mockResponse = GetSecretValueResponse.builder()
                .secretString(secretJson)
                .build();

        when(mockSecretsClient.getSecretValue(any(GetSecretValueRequest.class)))
                .thenReturn(mockResponse);

        KeyVaultApp keyVaultApp = new KeyVaultApp(mockSecretsClient);

        // Act
        DatabaseCredentials credentials = keyVaultApp.getCredentials("test-secret");

        // Assert
        assertNotNull(credentials);
        assertEquals("testuser", credentials.getUsername());
        assertEquals("testpass", credentials.getPassword());
        verify(mockSecretsClient, times(1)).getSecretValue(any(GetSecretValueRequest.class));
    }

    @Test
    void testGetCredentials_ResourceNotFoundException() {
        // Arrange
        when(mockSecretsClient.getSecretValue(any(GetSecretValueRequest.class)))
                .thenThrow(ResourceNotFoundException.builder()
                        .message("Secret not found")
                        .build());

        KeyVaultApp keyVaultApp = new KeyVaultApp(mockSecretsClient);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            keyVaultApp.getCredentials("non-existent-secret");
        });
        verify(mockSecretsClient, times(1)).getSecretValue(any(GetSecretValueRequest.class));
    }

    @Test
    void testGetCredentials_InvalidRequestException() {
        // Arrange
        when(mockSecretsClient.getSecretValue(any(GetSecretValueRequest.class)))
                .thenThrow(InvalidRequestException.builder()
                        .message("Invalid request")
                        .build());

        KeyVaultApp keyVaultApp = new KeyVaultApp(mockSecretsClient);

        // Act & Assert
        assertThrows(InvalidRequestException.class, () -> {
            keyVaultApp.getCredentials("invalid-secret-id");
        });
        verify(mockSecretsClient, times(1)).getSecretValue(any(GetSecretValueRequest.class));
    }

    @Test
    void testGetCredentials_SecretsManagerException() {
        // Arrange
        when(mockSecretsClient.getSecretValue(any(GetSecretValueRequest.class)))
                .thenThrow(SecretsManagerException.builder()
                        .message("AWS Secrets Manager error")
                        .build());

        KeyVaultApp keyVaultApp = new KeyVaultApp(mockSecretsClient);

        // Act & Assert
        assertThrows(SecretsManagerException.class, () -> {
            keyVaultApp.getCredentials("test-secret");
        });
        verify(mockSecretsClient, times(1)).getSecretValue(any(GetSecretValueRequest.class));
    }

    @Test
    void testDatabaseCredentials_GettersWork() {
        // Arrange
        DatabaseCredentials credentials = new DatabaseCredentials("myuser", "mypass");

        // Act & Assert
        assertEquals("myuser", credentials.getUsername());
        assertEquals("mypass", credentials.getPassword());
    }

    @Test
    void testParseCredentials_SpecialCharactersInPassword() throws Exception {
        // Arrange
        String secretJson = "{\"username\":\"admin\",\"password\":\"P@ssw0rd!#$%\"}";
        KeyVaultApp keyVaultApp = new KeyVaultApp(mockSecretsClient);

        // Act
        DatabaseCredentials credentials = keyVaultApp.parseCredentials(secretJson);

        // Assert
        assertEquals("admin", credentials.getUsername());
        assertEquals("P@ssw0rd!#$%", credentials.getPassword());
    }
}
