package com.example;

public class Main {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("AWS Secrets Manager Database Credentials Retriever");
            System.out.println("Usage: java -jar app.jar <aws-region>");
            System.out.println("Example: java -jar app.jar us-east-1");
            System.exit(1);
        }
        
        new KeyVaultApp(args[0]);
    }
}