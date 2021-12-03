package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.CryptoPunksMeta
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import scalether.domain.Address
import java.math.BigInteger

@Component
@CaptureSpan(type = SpanType.EXT, subtype = "meta")
class CryptoPunksPropertiesResolver(
    val cryptoPunksRepository: CryptoPunksRepository,
    nftIndexerProperties: NftIndexerProperties
) : ItemPropertiesResolver {

    private val cryptoPunksAddress = Address.apply(nftIndexerProperties.cryptoPunksContractAddress)

    override val name get() = "CryptoPunks"

    fun get(punkId: BigInteger): Mono<CryptoPunksMeta> =
        cryptoPunksRepository.findById(punkId)

    suspend fun save(punk: CryptoPunksMeta): CryptoPunksMeta? =
        cryptoPunksRepository.save(punk).awaitFirstOrNull()

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token != cryptoPunksAddress) {
            return null
        }
        return get(itemId.tokenId.value).map {
            ItemProperties(
                name = "CryptoPunk #${it.id}",
                description = null,
                image = it.image,
                imagePreview = null,
                imageBig = null,
                animationUrl = null,
                attributes = it.attributes,
                rawJsonContent = null
            )
        }.awaitFirstOrNull()
    }
}

@Component
@CaptureSpan(type = META_CAPTURE_SPAN_TYPE)
class CryptoPunksRepository(private val mongo: ReactiveMongoOperations) {

    fun findById(id: BigInteger): Mono<CryptoPunksMeta> {
        return mongo.findById(id)
    }

    fun save(punk: CryptoPunksMeta): Mono<CryptoPunksMeta> {
        return mongo.save(punk)
    }
}
