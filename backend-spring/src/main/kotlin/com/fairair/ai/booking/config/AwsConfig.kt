package com.fairair.ai.booking.config

import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient
import software.amazon.awssdk.services.lexruntimev2.LexRuntimeV2AsyncClient
import software.amazon.awssdk.services.polly.PollyAsyncClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import jakarta.annotation.PostConstruct

@Configuration
class AwsConfig {

    private val log = LoggerFactory.getLogger(AwsConfig::class.java)

    // Try direct property name first (Spring Boot native .env loading), then env. prefix (spring-dotenv)
    @Value("\${AWS_ACCESS_KEY_ID:\${env.AWS_ACCESS_KEY_ID:}}")
    private lateinit var accessKeyId: String

    @Value("\${AWS_SECRET_ACCESS_KEY:\${env.AWS_SECRET_ACCESS_KEY:}}")
    private lateinit var secretAccessKey: String

    @Value("\${AWS_REGION:\${env.AWS_REGION:us-east-1}}")
    private lateinit var awsRegion: String

    @PostConstruct
    fun logConfig() {
        log.info("AWS Config initialized - accessKeyId present: ${accessKeyId.isNotBlank()}, region: $awsRegion")
        if (accessKeyId.isBlank()) {
            log.warn("AWS_ACCESS_KEY_ID is blank! Check that .env file exists in backend-spring/ directory.")
        }
    }

    @Bean
    fun bedrockRuntimeAsyncClient(): BedrockRuntimeAsyncClient {
        val builder = BedrockRuntimeAsyncClient.builder()
            .region(Region.of(awsRegion))

        if (accessKeyId.isNotBlank() && secretAccessKey.isNotBlank()) {
            builder.credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                )
            )
        }
        
        return builder.build()
    }

    @Bean
    fun lexRuntimeV2AsyncClient(): LexRuntimeV2AsyncClient {
        val builder = LexRuntimeV2AsyncClient.builder()
            .region(Region.of(awsRegion))

        if (accessKeyId.isNotBlank() && secretAccessKey.isNotBlank()) {
            builder.credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                )
            )
        }

        return builder.build()
    }
}
