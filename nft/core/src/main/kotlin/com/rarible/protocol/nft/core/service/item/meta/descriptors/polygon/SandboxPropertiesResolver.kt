package com.rarible.protocol.nft.core.service.item.meta.descriptors.polygon

import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.meta.EthMetaContent
import com.rarible.protocol.nft.core.service.item.meta.descriptors.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.RariblePropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class SandboxPropertiesResolver(
    private val raribleResolver: RariblePropertiesResolver
) : ItemPropertiesResolver {

    override val name get() = "TheSandbox"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token != SANDBOX_NFT_ADDRESS) {
            return null
        }
        logMetaLoading(itemId, "Resolving $name Nft properties")
        val properties = raribleResolver.resolve(itemId) ?: return null

        val content = properties.content

        return properties.copy(
            content = content.copy(
                imagePreview = fixUrl(content.imagePreview),
                imageBig = fixUrl(content.imageBig),
                imageOriginal = fixUrl(content.imageOriginal),
                videoOriginal = fixUrl(content.videoOriginal),
            )
        )
    }

    private fun fixUrl(content: EthMetaContent?): EthMetaContent? {
        content ?: return null

        return if (content.url.endsWith(WEBP)) {
            val startFrom = API_PATH.length
            val guid = content.url.substring(startFrom, startFrom + GUID_LENGTH)
            content.copy(url = API_PATH + guid + PREVIEW_PATH)
        } else content
    }

    companion object {
        val SANDBOX_NFT_ADDRESS: Address = Address.apply("0x9d305a42a3975ee4c1c57555bed5919889dce63f")
        const val WEBP = ".webp"
        const val PREVIEW_PATH = "/preview"
        const val API_PATH = "https://api.sandbox.game/lands/"
        const val GUID_LENGTH = 36
    }
}
