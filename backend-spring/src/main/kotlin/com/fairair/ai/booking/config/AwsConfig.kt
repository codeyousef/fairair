package com.fairair.ai.booking.config

import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient
import software.amazon.awssdk.services.lexruntimev2.LexRuntimeV2AsyncClient
import software.amazon.awssdk.services.polly.PollyAsyncClient
import software.amazon.awssdk.services.transcribestreaming.TranscribeStreamingAsyncClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import jakarta.annotation.PostConstruct

@Configuration
class AwsConfig {

    private val log = LoggerFactory.getLogger(AwsConfig::class.java)

    @Value("\${aws.access-key-id:}")
    private lateinit var accessKeyId: String

    @Value("\${aws.secret-access-key:}")
    private lateinit var secretAccessKey: String

    @Value("\${aws.region:us-east-1}")
    private lateinit var awsRegion: String

    @PostConstruct
    fun logConfig() {
        log.info("AWS Config initialized - accessKeyId present: ${accessKeyId.isNotBlank()}, region: $awsRegion")
        if (accessKeyId.isBlank()) {
            log.warn("AWS credentials not configured! Add them to application-local.yml or set aws.* properties.")
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

    @Bean
    fun pollyAsyncClient(): PollyAsyncClient {
        val builder = PollyAsyncClient.builder()
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
    fun transcribeStreamingAsyncClient(): TranscribeStreamingAsyncClient {
        val builder = TranscribeStreamingAsyncClient.builder()
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
