package com.javatechie.aws.rds;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import static com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder.standard;

@Configuration
public class ApplicationConfig {

    private Gson gson = new Gson();
    String secretName = "db-credential";
    String region = "us-east-2";

    @Value("${cloud.aws.credentials.access-key}")
    private String awsAccessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String awsSecretKey;



    @Bean
    public DataSource dataSource() {
        AwsSecret secret = getSecret();
        return DataSourceBuilder
                .create()
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .url("jdbc:" + secret.getEngine() + "://" + secret.getHost() + ":" + secret.getPort() + "/javatechie")
                .username(secret.getUsername())
                .password(secret.getPassword())
                .build();
    }

    private AwsSecret getSecret() {
        AWSSecretsManager secretsManager = standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey)))
                .build();
        String secret;
        GetSecretValueRequest secretValueRequest = new GetSecretValueRequest()
                .withSecretId(secretName);
        GetSecretValueResult secretValueResult = null;
        try {
            secretValueResult = secretsManager.getSecretValue(secretValueRequest);
        } catch (Exception ex) {
            throw ex;
        }
        if (secretValueResult.getSecretString() != null) {
            secret = secretValueResult.getSecretString();
            return gson.fromJson(secret, AwsSecret.class);
        }
        return null;
    }
}
