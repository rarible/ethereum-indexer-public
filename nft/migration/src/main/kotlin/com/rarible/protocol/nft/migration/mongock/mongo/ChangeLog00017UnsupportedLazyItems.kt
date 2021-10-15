package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.nft.core.converters.dto.DeletedItemDtoConverter
import com.rarible.protocol.nft.core.converters.dto.DeletedOwnershipDtoConverter
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactive.collect
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.nin
import java.util.*

@ChangeLog(order = "00017")
class ChangeLog00017UnsupportedLazyItems {
    @ChangeSet(
        id = "ChangeLog00017RemoveLazyItems.remove",
        order = "1",
        author = "protocol"
    )
    fun remove(
        @NonLockGuarded mongo: ReactiveMongoOperations,
        @NonLockGuarded publisher: ProtocolNftEventPublisher
    ) = runBlocking {
        logger.info("Started removing lazy mint items from unsupported collections")
        val query = Query.query(Token::features nin listOf(TokenFeature.MINT_AND_TRANSFER))
        mongo.find(query, Token::class.java).collect { token ->
            mongo.find(
                Query(
                    Criteria().andOperator(
                        Item::token isEqualTo token.id,
                        Item::lazySupply gt EthUInt256.ZERO,
                        Item::deleted isEqualTo false
                    )
                ), Item::class.java
            ).collect { item ->
                deleteItem(item, mongo, publisher)
                deleteOwnerShips(item, mongo, publisher)
                deleteLazyHistory(item, mongo)
            }
        }
        logger.info("Finished removing lazy mint items from unsupported collections")
    }

    private suspend fun deleteItem(
        item: Item,
        mongo: ReactiveMongoOperations,
        publisher: ProtocolNftEventPublisher
    ) {
        mongo.save(item.copy(deleted = true)).awaitSingle()
        logger.info("Deleted item=${item.id}")
        val msg = NftItemDeleteEventDto(
            UUID.randomUUID().toString(),
            item.id.decimalStringValue,
            DeletedItemDtoConverter.convert(item.id)
        )
        publisher.publish(msg)
    }

    private suspend fun deleteOwnerShips(
        item: Item,
        mongo: ReactiveMongoOperations,
        publisher: ProtocolNftEventPublisher
    ) {
        mongo.find(
            Query(
                Criteria().andOperator(
                    Ownership::token isEqualTo item.token,
                    Ownership::tokenId isEqualTo item.tokenId
                )
            ), Ownership::class.java
        ).collect { ownership ->
            mongo.remove(ownership).awaitFirstOrNull()
            logger.info("Deleted ownership=${ownership.id}")
            val msg = NftOwnershipDeleteEventDto(
                UUID.randomUUID().toString(),
                ownership.id.decimalStringValue,
                DeletedOwnershipDtoConverter.convert(ownership.id)
            )
            publisher.publish(msg)
        }
    }

    private suspend fun deleteLazyHistory(item: Item, mongo: ReactiveMongoOperations) {
        mongo.find(
            Query(
                Criteria().andOperator(
                    ItemLazyMint::token isEqualTo item.token,
                    ItemLazyMint::tokenId isEqualTo item.tokenId
                )
            ), ItemLazyMint::class.java, LazyNftItemHistoryRepository.COLLECTION
        ).collect { history ->
            mongo.remove(history, LazyNftItemHistoryRepository.COLLECTION).awaitFirstOrNull()
            logger.info("Deleted history=${history.id}")
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ChangeLog00013InsertAttributesForCryptoPunks::class.java)
    }
}
