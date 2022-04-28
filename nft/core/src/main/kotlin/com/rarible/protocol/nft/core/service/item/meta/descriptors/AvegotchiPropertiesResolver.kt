package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.service.IpfsService
import com.rarible.protocol.nft.core.service.item.meta.ExternalHttpClient
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesWrapper
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesMapper
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesParser
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
@Qualifier("AvegotchiPropertiesResolver")
class AvegotchiPropertiesResolver(
    private val openSeaPropertiesResolver: OpenSeaPropertiesResolver,
    ipfsService: IpfsService,
    propertiesHttpLoader: AvegotchiPropertiesHttpLoader,
    tokenUriResolver: BlockchainTokenUriResolver
) : RariblePropertiesResolver(ipfsService, propertiesHttpLoader, tokenUriResolver) {

    override val name get() = "Avegotchi"

    override suspend fun resolve(itemId: ItemId): ItemPropertiesWrapper {
        if (itemId.token != AVEGOTCHI_ADDRESS) {
            return wrapAsUnResolved(null)
        }

        val tokenUri = tokenUriResolver.getUri(itemId)
        if (tokenUri.isNullOrBlank()) {
            logMetaLoading(itemId, "empty token URI", warn = true)
            return wrapAsUnResolved(null)
        }
        logMetaLoading(itemId, "got URI from token contract: $tokenUri")
        val properties = resolveByTokenUri(itemId, tokenUri)
        return if (properties != null) {
            wrapAsResolved(properties)
        } else {
            openSeaPropertiesResolver.resolve(itemId)
        }
    }

    companion object {
        val AVEGOTCHI_ADDRESS: Address = Address.apply("0x1906fd9c4ac440561f7197da0a4bd2e88df5fa70")
    }
}

