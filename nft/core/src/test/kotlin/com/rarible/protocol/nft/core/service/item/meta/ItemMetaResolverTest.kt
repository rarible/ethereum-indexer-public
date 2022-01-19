package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.content.meta.loader.ContentMeta
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemContentMeta
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemMeta
import com.rarible.protocol.nft.core.model.ItemProperties
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ItemMetaResolverTest {
    private val itemPropertiesService = mockk<ItemPropertiesService>()
    private val mediaMetaService = mockk<MediaMetaService>()
    private val itemMetaResolver = ItemMetaResolver(itemPropertiesService, mediaMetaService)

    @Test
    fun `attach media meta to image preview and animation`() = runBlocking<Unit> {
        val itemId = ItemId(randomAddress(), EthUInt256(randomBigInt()))
        val itemProperties = ItemProperties(
            name = "name",
            description = null,
            image = "imageUrl",
            imagePreview = "imagePreviewUrl",
            imageBig = "imageBigUrl",
            animationUrl = "animationUrl",
            attributes = listOf(ItemAttribute(randomString(), randomString(), randomString(), randomString())),
            rawJsonContent = null
        )
        coEvery { itemPropertiesService.resolve(itemId) } returns itemProperties

        val imageMedia = ContentMeta("imageMeta", width = 1, height = 2)
        val imagePreviewMedia =
            ContentMeta("imagePreviewMeta", width = 3, height = 4)
        val imageBigMedia = ContentMeta("imageBigMeta", width = 5, height = 6)
        val animationMedia = ContentMeta("animationMeta", width = 7, height = 8)
        coEvery { mediaMetaService.getMediaMeta("imageUrl") } returns imageMedia
        coEvery { mediaMetaService.getMediaMeta("imagePreviewUrl") } returns imagePreviewMedia
        coEvery { mediaMetaService.getMediaMeta("imageBigUrl") } returns imageBigMedia
        coEvery { mediaMetaService.getMediaMeta("animationUrl") } returns animationMedia
        val itemMeta = itemMetaResolver.resolveItemMeta(itemId)
        assertThat(itemMeta).isEqualTo(ItemMeta(itemProperties, ItemContentMeta(imagePreviewMedia, animationMedia)))
    }
}
