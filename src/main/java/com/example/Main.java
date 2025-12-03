package com.example;

public class Main {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Azure Key Vault Database Credentials Retriever");
            System.out.println("Usage: java -jar app.jar <key-vault-url>");
            System.out.println("Example: java -jar app.jar https://mykeyvault.vault.azure.net/");
            System.exit(1);
        }
        
        new KeyVaultApp(args[0]);
    }
}