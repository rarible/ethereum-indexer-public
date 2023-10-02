package com.rarible.protocol.nft.core.producer

import com.rarible.core.kafka.Compression
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.ActivityTopicProvider
import com.rarible.protocol.dto.EthActivityEventDto
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionEventTopicProvider
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemEventTopicProvider
import com.rarible.protocol.dto.NftItemMetaEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipEventTopicProvider
import com.rarible.protocol.nft.core.model.ActionEvent

class ProducerFactory(
    private val kafkaReplicaSet: String,
    private val blockchain: Blockchain,
    private val environment: String,
    private val compression: Compression,
) {
    private val clientId = "$environment.${blockchain.value}.protocol-nft-events-importer"

    fun createCollectionEventsProducer(): RaribleKafkaProducer<NftCollectionEventDto> {
        return RaribleKafkaProducer(
            clientId = "$clientId.collection",
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = NftCollectionEventDto::class.java,
            defaultTopic = NftCollectionEventTopicProvider.getTopic(environment, blockchain.value),
            bootstrapServers = kafkaReplicaSet,
            compression = compression,
        )
    }

    fun createItemEventsProducer(): RaribleKafkaProducer<NftItemEventDto> {
        return RaribleKafkaProducer(
            clientId = "$clientId.item",
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = NftItemEventDto::class.java,
            defaultTopic = NftItemEventTopicProvider.getTopic(environment, blockchain.value),
            bootstrapServers = kafkaReplicaSet,
            compression = compression,
        )
    }

    fun createItemMetaEventsProducer(): RaribleKafkaProducer<NftItemMetaEventDto> {
        return RaribleKafkaProducer(
            clientId = "$clientId.itemMeta",
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = NftItemMetaEventDto::class.java,
            defaultTopic = NftItemEventTopicProvider.getItemMetaTopic(environment, blockchain.value),
            bootstrapServers = kafkaReplicaSet,
            compression = compression,
        )
    }

    fun createOwnershipEventsProducer(): RaribleKafkaProducer<NftOwnershipEventDto> {
        return RaribleKafkaProducer(
            clientId = "$clientId.ownership",
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = NftOwnershipEventDto::class.java,
            defaultTopic = NftOwnershipEventTopicProvider.getTopic(environment, blockchain.value),
            bootstrapServers = kafkaReplicaSet,
            compression = compression,
        )
    }

    fun createItemActivityProducer(): RaribleKafkaProducer<EthActivityEventDto> {
        return RaribleKafkaProducer(
            clientId = "$clientId.item-activity",
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = EthActivityEventDto::class.java,
            defaultTopic = ActivityTopicProvider.getActivityTopic(environment, blockchain.value),
            bootstrapServers = kafkaReplicaSet,
            compression = compression,
        )
    }

    fun createItemActionEventProducer(): RaribleKafkaProducer<ActionEvent> {
        return RaribleKafkaProducer(
            clientId = "$clientId.item.internal.action",
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = ActionEvent::class.java,
            defaultTopic = InternalTopicProvider.getItemActionTopic(environment, blockchain.value),
            bootstrapServers = kafkaReplicaSet,
            compression = compression,
        )
    }
}
