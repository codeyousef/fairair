package com.fairair.ai.booking.config

import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient
import software.amazon.awssdk.services.lexruntimev2.LexRuntimeV2AsyncClient
import software.amazon.awssdk.services.polly.PollyAsyncClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AwsConfig {

    @Value("\${aws.accessKeyId:}")
    private lateinit var accessKeyId: String

    @Value("\${aws.secretAccessKey:}")
    private lateinit var secretAccessKey: String

    @Bean
    fun bedrockRuntimeAsyncClient(): BedrockRuntimeAsyncClient {
        val builder = BedrockRuntimeAsyncClient.builder()
            .region(Region.US_EAST_1)

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
            .region(Region.US_EAST_1)

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
