package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ContentMeta
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemMeta
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.MediaMeta
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ItemMetaServiceTest {
    private val itemPropertiesService = mockk<ItemPropertiesService>()
    private val mediaMetaService = mockk<MediaMetaService>()
    private val metaService = ItemMetaServiceImpl(itemPropertiesService, mediaMetaService)

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

        val imageMedia = MediaMeta("imageMeta", width = 1, height = 2)
        val imagePreviewMedia = MediaMeta("imagePreviewMeta", width = 3, height = 4)
        val imageBigMedia = MediaMeta("imageBigMeta", width = 5, height = 6)
        val animationMedia = MediaMeta("animationMeta", width = 7, height = 8)
        coEvery { mediaMetaService.getMediaMeta("imageUrl") } returns imageMedia
        coEvery { mediaMetaService.getMediaMeta("imagePreviewUrl") } returns imagePreviewMedia
        coEvery { mediaMetaService.getMediaMeta("imageBigUrl") } returns imageBigMedia
        coEvery { mediaMetaService.getMediaMeta("animationUrl") } returns animationMedia
        val itemMeta = metaService.getItemMetadata(itemId)
        assertThat(itemMeta).isEqualTo(ItemMeta(itemProperties, ContentMeta(imagePreviewMedia, animationMedia)))
    }

    @Test
    fun `reset properties and URLs of all media`() = runBlocking<Unit> {
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

        val imageMedia = MediaMeta("imageMeta", width = 1, height = 2)
        val imagePreviewMedia = MediaMeta("imagePreviewMeta", width = 3, height = 4)
        val imageBigMedia = MediaMeta("imageBigMeta", width = 5, height = 6)
        val animationMedia = MediaMeta("animationMeta", width = 7, height = 8)
        coEvery { mediaMetaService.getMediaMeta("imageUrl") } returns imageMedia
        coEvery { mediaMetaService.getMediaMeta("imagePreviewUrl") } returns imagePreviewMedia
        coEvery { mediaMetaService.getMediaMeta("imageBigUrl") } returns imageBigMedia
        coEvery { mediaMetaService.getMediaMeta("animationUrl") } returns animationMedia
        val itemMeta = metaService.getItemMetadata(itemId)
        assertThat(itemMeta).isEqualTo(ItemMeta(itemProperties, ContentMeta(imagePreviewMedia, animationMedia)))

        coJustRun { itemPropertiesService.resetProperties(itemId) }
        coJustRun { mediaMetaService.resetMediaMeta(any()) }

        metaService.resetMetadata(itemId)
        coVerify(exactly = 1) { itemPropertiesService.resetProperties(itemId) }
        coVerify(exactly = 1) { mediaMetaService.resetMediaMeta("imageUrl") }
        coVerify(exactly = 1) { mediaMetaService.resetMediaMeta("imagePreviewUrl") }
        coVerify(exactly = 1) { mediaMetaService.resetMediaMeta("imageBigUrl") }
        coVerify(exactly = 1) { mediaMetaService.resetMediaMeta("animationUrl") }
    }
}
