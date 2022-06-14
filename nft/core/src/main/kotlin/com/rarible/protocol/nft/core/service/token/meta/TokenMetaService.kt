package com.rarible.protocol.nft.core.service.token.meta

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.dto.MetaContentDto
import com.rarible.protocol.nft.core.model.TokenMeta
import com.rarible.protocol.nft.core.model.meta.EthImageProperties
import com.rarible.protocol.nft.core.model.meta.EthMetaContent
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.meta.ItemMetaService
import com.rarible.protocol.nft.core.service.item.meta.MediaMetaService
import com.rarible.protocol.nft.core.service.item.meta.TOKEN_META_CAPTURE_SPAN_TYPE
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
@CaptureSpan(type = TOKEN_META_CAPTURE_SPAN_TYPE)
class TokenMetaService(
    private val tokenPropertiesService: TokenPropertiesService,
    private val itemRepository: ItemRepository,
    private val itemMetaService: ItemMetaService,
    private val mediaMetaService: MediaMetaService
) {

    suspend fun get(id: Address): TokenMeta {
        val properties = tokenPropertiesService.resolve(id)

        // TODO originally, it should be done in resolve procedure
        val withContent = properties?.image?.let {
            properties.copy(
                content = listOf(
                    EthMetaContent(it, MetaContentDto.Representation.ORIGINAL, null, EthImageProperties())
                )
            )
        } ?: properties

        return if (withContent == null) TokenMeta.EMPTY else {
            TokenMeta(
                properties = withContent,
                contentMeta = withContent.image?.let {
                    mediaMetaService.getMediaMetaFromCache(it, id.prefixed())
                }
            )
        }
    }

    suspend fun reset(id: Address) {
        tokenPropertiesService.reset(id)
    }

    suspend fun refreshMetadataForCollectionItems(token: Address) {
        itemRepository.findTokenItems(token, null).map { it }.onEach {
            itemMetaService.scheduleMetaUpdate(it.id, "refresh the whole collection $token")
        }.collect()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(TokenMetaService::class.java)
    }
}
