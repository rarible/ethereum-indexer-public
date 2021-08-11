package com.rarible.protocol.nftorder.listener.test

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.core.test.ext.KafkaTestExtension.Companion.kafkaContainer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.NftOrderItemEventDto
import com.rarible.protocol.dto.NftOrderItemEventTopicProvider
import com.rarible.protocol.dto.NftOrderOwnershipEventDto
import com.rarible.protocol.dto.NftOrderOwnershipEventTopicProvider
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.unlockable.api.client.LockControllerApi
import io.mockk.mockk
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class IntegrationTestConfiguration {

    @Bean
    fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
        return ApplicationEnvironmentInfo("test", "test.com")
    }

    // In case when we have dedicated mocks, it's better to define them as beans instead of using
    // @MockkBean - that allow Spring to reuse launched context for different tests and, as a result,
    // gives significant speedup for test's run

    @Bean
    @Primary
    fun testNftItemControllerApi(): NftItemControllerApi = mockk()

    @Bean
    @Primary
    fun testNftOwnershipControllerApi(): NftOwnershipControllerApi = mockk()

    @Bean
    @Primary
    fun testOrderControllerApi(): OrderControllerApi = mockk()

    @Bean
    @Primary
    fun testLockControllerApi(): LockControllerApi = mockk()

    @Bean
    fun testItemConsumer(blockchain: Blockchain): RaribleKafkaConsumer<NftOrderItemEventDto> {
        val topic = NftOrderItemEventTopicProvider.getTopic(applicationEnvironmentInfo().name, blockchain.value)
        return RaribleKafkaConsumer(
            clientId = "test-nft-order-item-consumer",
            consumerGroup = "test-nft-order-item-group",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = NftOrderItemEventDto::class.java,
            defaultTopic = topic,
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    @Bean
    fun testOwnershipConsumer(blockchain: Blockchain): RaribleKafkaConsumer<NftOrderOwnershipEventDto> {
        val topic = NftOrderOwnershipEventTopicProvider.getTopic(applicationEnvironmentInfo().name, blockchain.value)
        return RaribleKafkaConsumer(
            clientId = "test-nft-order-ownership-consumer",
            consumerGroup = "test-nft-order-ownership-group",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = NftOrderOwnershipEventDto::class.java,
            defaultTopic = topic,
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

}