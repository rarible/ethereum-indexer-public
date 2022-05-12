package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.misc.detector.Base64Detector.Companion.MIME_TYPE_PREFIX
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.IpfsService
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class EmblemVaultV2Resolver(
    private val raribleResolver: RariblePropertiesResolver,
    private val propertiesHttpLoader: PropertiesHttpLoader,
    private val ipfsService: IpfsService
) : ItemPropertiesResolver {

    override val name get() = "EmblemVaultV2"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token != EMBLEM_VAULT_V2_ADDRESS) {
            return null
        }
        logMetaLoading(itemId, "Resolving $name Nft properties")
        val properties = raribleResolver.resolve(itemId) ?: return null

        if (properties.image != null) {
            val resolvedUrl = ipfsService.resolvePublicHttpUrl(properties.image)
            val imageContent = propertiesHttpLoader.getByUrl(itemId, resolvedUrl)

            if (imageContent != null && imageContent.startsWith(MIME_TYPE_PREFIX)) {
                return properties.copy(image = imageContent)
            }
        }

        return properties
    }

    companion object {
        val EMBLEM_VAULT_V2_ADDRESS: Address = Address.apply("0x82c7a8f707110f5fbb16184a5933e9f78a34c6ab")
    }
}
