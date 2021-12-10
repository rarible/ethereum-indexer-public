package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.core.cache.Cache
import com.rarible.core.common.nowMillis
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.service.token.meta.TokenPropertiesService
import com.rarible.protocol.nft.core.service.token.meta.TokenPropertiesService.CachedTokenProperties
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import java.util.*

@ChangeLog(order = "00018")
class ChangeLog00019CryptopunkCollectionMeta {
    @ChangeSet(
        id = "ChangeLog00019CryptopunkCollectionMeta.save",
        order = "1",
        author = "protocol"
    )
    fun save(
        @NonLockGuarded nftIndexerProperties: NftIndexerProperties,
        @NonLockGuarded mongo: ReactiveMongoOperations
    ) = runBlocking {
        logger.info("Started saving cryptopunk collection meta")
        val properties = TokenProperties(
            name = "CryptoPunks",
            description = "CryptoPunks launched as a fixed set of 10,000 items in mid-2017 and became one of the inspirations for the ERC-721 standard. They have been featured in places like The New York Times, Christie’s of London, Art|Basel Miami, and The PBS NewsHour.",
            image = "https://ipfs.io/ipfs/QmPnbisaFugMs6LP9usYvepoUAuqUqhBmTseYCyEVsoWsL",
            externalLink = "https://www.larvalabs.com/cryptopunks",
            feeRecipient = null,
            sellerFeeBasisPoints = null
        )
        val entity = Cache(
            id = nftIndexerProperties.cryptoPunksContractAddress,
            data = CachedTokenProperties(properties, nowMillis()),
            updateDate = Date()
        )
        mongo.save(entity, TokenPropertiesService.TOKEN_METADATA_COLLECTION).awaitFirstOrNull()
        logger.info("Finished saving cryptopunk meta")
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ChangeLog00019CryptopunkCollectionMeta::class.java)
    }
}
