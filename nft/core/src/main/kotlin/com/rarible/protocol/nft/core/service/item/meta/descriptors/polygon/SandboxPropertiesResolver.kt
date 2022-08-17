package com.rarible.protocol.nft.core.service.item.meta.descriptors.polygon

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.meta.EthMetaContent
import com.rarible.protocol.nft.core.service.item.meta.ITEM_META_CAPTURE_SPAN_TYPE
import com.rarible.protocol.nft.core.service.item.meta.descriptors.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.RariblePropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.net.URI
import kotlin.io.path.toPath

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
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

        return if(content.url.endsWith(WEBP)) {
            val res = URI(content.url).toPath().parent.parent.resolve(PREVIEW_PATH)
            content.copy(url = res.toString())
        } else content
    }

    companion object {
        val SANDBOX_NFT_ADDRESS: Address = Address.apply("0x9d305a42a3975ee4c1c57555bed5919889dce63f")
        const val WEBP = ".webp"
        const val PREVIEW_PATH = "preview"
    }
}
