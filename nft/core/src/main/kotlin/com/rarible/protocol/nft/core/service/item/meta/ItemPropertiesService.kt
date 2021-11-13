package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.cache.CacheDescriptor
import com.rarible.core.cache.CacheService
import com.rarible.core.cache.get
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.TemporaryItemProperties
import com.rarible.protocol.nft.core.repository.TemporaryItemPropertiesRepository
import com.rarible.protocol.nft.core.service.CryptoPunksMetaService
import com.rarible.protocol.nft.core.service.item.meta.descriptors.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.whenComplete
import scalether.domain.Address
import java.math.BigInteger

@Service
@CaptureSpan(type = SpanType.APP, subtype = "meta")
class ItemPropertiesService(
    private val propertiesCacheDescriptor: PropertiesCacheDescriptor,
    private val kittiesCacheDescriptor: KittiesCacheDescriptor,
    private val lootCacheDescriptor: LootCacheDescriptor,
    private val yInsureCacheDescriptor: YInsureCacheDescriptor,
    private val hegicCacheDescriptor: HegicCacheDescriptor,
    private val hashmasksCacheDescriptor: HashmasksCacheDescriptor,
    private val waifusionCacheDescriptor: WaifusionCacheDescriptor,
    private val cryptoPunksMetaService: CryptoPunksMetaService,
    private val openSeaCacheDescriptor: OpenSeaCacheDescriptor,
    private val ipfsService: IpfsService,
    private val temporaryItemPropertiesRepository: TemporaryItemPropertiesRepository,
    properties: NftIndexerProperties,
    @Value("\${api.yinsure.address}") yInsureAddress: String,
    @Value("\${api.hegic.address}") hegicAddress: String,
    @Value("\${api.hashmasks.address}") hashmasksAddress: String,
    @Value("\${api.waifusion.address}") waifusionAddress: String,
    @Autowired(required = false) private val cacheService: CacheService?
) {
    private val yInsureAddress = Address.apply(yInsureAddress)
    private val hegicAddress = Address.apply(hegicAddress)
    private val hashmasksAddress = Address.apply(hashmasksAddress)
    private val waifusionAddress = Address.apply(waifusionAddress)
    private val cryptoPunksAddress = Address.apply(properties.cryptoPunksContractAddress)

    //todo нужно принимать не строки
    fun saveTemporaryProperties(id: String, uri: String): Mono<Void> {
        return propertiesCacheDescriptor.getByUri(uri)
            .map { getItemProperties(it) }
            .flatMap { temporaryItemPropertiesRepository.save(TemporaryItemProperties(id, it)) }
            .then()
            .onErrorResume {
                logger.warn("Unable to save temporary item properties", it)
                Mono.empty()
            }
    }

    @Suppress("ReactiveStreamsUnusedPublisher")
    fun getProperties(token: Address, tokenId: BigInteger): Mono<ItemProperties> {
        return when (token) {
            yInsureAddress -> {
                getExternalItemProperties(tokenId, yInsureCacheDescriptor)
            }
            hegicAddress -> {
                getExternalItemProperties(tokenId, hegicCacheDescriptor)
            }
            hashmasksAddress -> {
                getExternalItemProperties(tokenId, hashmasksCacheDescriptor)
                    .flatMap { itemProperties ->
                        openSeaCacheDescriptor.fetchAsset(token, tokenId)
                            .map {
                                itemProperties.copy(
                                    image = it.image?.let { url -> ipfsService.resolveIpfsUrl(url) },
                                    imagePreview = it.imagePreview,
                                    imageBig = it.imageBig
                                )
                            }
                    }
            }
            waifusionAddress -> {
                getExternalItemProperties(tokenId, waifusionCacheDescriptor)
                    .map {
                        it.copy(image = it.image?.let { url -> ipfsService.resolveIpfsUrl(url) })
                    }
            }
            cryptoPunksAddress -> cryptoPunkProps(tokenId)
            else -> {
                getPropertiesFromOpensea(token, tokenId)
                    .switchIfEmpty {
                        when (token) {
                            CRYPTO_KITTIES -> getCryptoKittiesProperties(tokenId)
                            LOOT_ADDRESS -> getLootProperties(tokenId)
                            else -> getStandardProperties(token, tokenId)
                        }
                    }
            }
        }.switchIfEmpty {
            temporaryItemPropertiesRepository.findById("$token:$tokenId")
                .map { it.value }
                .map {
                    it.copy(
                        image = it.image?.let { url -> ipfsService.resolveIpfsUrl(url) },
                        animationUrl = it.animationUrl?.let { url -> ipfsService.resolveIpfsUrl(url) }
                    )
                }
        }
    }

    fun cryptoPunkProps(tokenId: BigInteger): Mono<ItemProperties?> {
        return cryptoPunksMetaService.get(tokenId).map {
            ItemProperties(
                name = "CryptoPunk #$tokenId",
                description = null,
                image = it.image,
                imagePreview = null,
                imageBig = null,
                attributes = it.attributes
            )
        }
    }

    @Suppress("ReactiveStreamsUnusedPublisher")
    fun getLootProperties(tokenId: BigInteger): Mono<ItemProperties> {
        return cacheService.get("$LOOT_ADDRESS:$tokenId", lootCacheDescriptor, true)
    }

    fun resetLootProperties(tokenId: BigInteger): Mono<Void> =
        cacheService?.reset("$LOOT_ADDRESS:$tokenId", lootCacheDescriptor) ?: Mono.empty()

    fun resetProperties(token: Address, tokenId: BigInteger): Mono<Void> =
        listOf(
            when (token) {
                yInsureAddress -> resetExternalItemProperties(tokenId, yInsureCacheDescriptor)
                hegicAddress -> resetExternalItemProperties(tokenId, hegicCacheDescriptor)
                hashmasksAddress -> resetExternalItemProperties(tokenId, hashmasksCacheDescriptor)
                waifusionAddress -> resetExternalItemProperties(tokenId, waifusionCacheDescriptor)
                CRYPTO_KITTIES -> resetCryptoKittiesProperties(tokenId)
                LOOT_ADDRESS -> resetLootProperties(tokenId)
                else -> Mono.empty<Void>()
            },
            openSeaCacheDescriptor.resetAsset(token, tokenId),
            temporaryItemPropertiesRepository.deleteById("$token:$tokenId"),
            resetStandardProperties(token, tokenId)
        ).map { it.onErrorResume { Mono.empty() } }.whenComplete()

    private fun getPropertiesFromOpensea(token: Address, tokenId: BigInteger): Mono<ItemProperties> {
        return openSeaCacheDescriptor.fetchAsset(token, tokenId)
            .map {
                it.copy(
                    image = it.image?.let { url -> ipfsService.resolveIpfsUrl(url) },
                    animationUrl = it.animationUrl?.let { url -> ipfsService.resolveIpfsUrl(url) }
                )
            }
    }

    private fun getStandardProperties(token: Address, tokenId: BigInteger): Mono<ItemProperties> {
        return cacheService
            .get("$token:$tokenId", propertiesCacheDescriptor, true)
            .map { getItemProperties(it) }
    }

    private fun resetStandardProperties(token: Address, tokenId: BigInteger): Mono<Void> =
        cacheService?.reset("$token:$tokenId", propertiesCacheDescriptor) ?: Mono.empty()

    private fun getItemProperties(itemProperties: ItemProperties): ItemProperties {
        return itemProperties.copy(
            image = itemProperties.image?.let { ipfsService.resolveIpfsUrl(it) },
            animationUrl = itemProperties.animationUrl?.let { ipfsService.resolveIpfsUrl(it) }
        )
    }

    private fun getCryptoKittiesProperties(tokenId: BigInteger): Mono<ItemProperties> {
        return cacheService.get("$CRYPTO_KITTIES:$tokenId", kittiesCacheDescriptor, true)
    }

    private fun resetCryptoKittiesProperties(tokenId: BigInteger): Mono<Void> =
        cacheService?.reset("$CRYPTO_KITTIES:$tokenId", kittiesCacheDescriptor) ?: Mono.empty()

    private fun getExternalItemProperties(tokenId: BigInteger, descriptor: CacheDescriptor<ItemProperties>): Mono<ItemProperties> {
        return cacheService.get("$tokenId", descriptor, true)
    }

    private fun resetExternalItemProperties(tokenId: BigInteger, descriptor: CacheDescriptor<ItemProperties>): Mono<Void> =
        cacheService?.reset("$tokenId", descriptor) ?: Mono.empty()

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ItemPropertiesService::class.java)
        val CRYPTO_KITTIES: Address = Address.apply("0x06012c8cf97bead5deae237070f9587f8e7a266d")
        val LOOT_ADDRESS: Address = Address.apply("0xff9c1b15b16263c61d017ee9f65c50e4ae0113d7")
    }
}
