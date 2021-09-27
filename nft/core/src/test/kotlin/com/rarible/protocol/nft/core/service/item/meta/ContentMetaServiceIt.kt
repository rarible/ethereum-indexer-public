package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.*
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.findById
import scalether.domain.Address
import java.time.Instant

@Tag("manual")
@Disabled
@IntegrationTest
class ContentMetaServiceIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var service: ContentMetaService

    @Autowired
    private lateinit var itemMetaService: ItemMetaService

    @Test
    fun testFetchCachedMeta() = runBlocking<Unit> {
        val itemId = ItemId(
            Address.apply("0x9b1aa69fe9fca10aa41250bba054aabd92aba5b6"),
            EthUInt256.of(116L)
        )
        val meta = itemMetaService.getItemMetadata(itemId)
        val cachedMeta = itemMetaService.getItemMetadata(itemId)

        assertThat(meta.meta.imageMeta).isNotNull
        assertThat(cachedMeta.meta.imageMeta).isNotNull
    }

    @Test
    fun testVideoWithPreview() {
        val meta = service.getByProperties(ItemProperties(
            name = "Test",
            description = "",
            imageBig = null,
            imagePreview = null,
            image = "ipfs://ipfs/QmSNhGhcBynr1s9QgPnon8HaiPzE5dKgmqSDNsNXCfDHGs/image.gif",
            animationUrl = "ipfs://ipfs/QmSNhGhcBynr1s9QgPnon8HaiPzE5dKgmqSDNsNXCfDHGs/video.mp4",
            attributes = listOf()
        )
        ).block()!!
        assertEquals(600, meta.imageMeta?.width)
    }

    @Test
    fun testEmpty() {
        val meta = service.getByProperties(ItemProperties(
            name = "Test",
            description = "",
            imageBig = null,
            imagePreview = null,
            image = null,
            animationUrl = null,
            attributes = listOf()
        )).block()!!
        assertEquals(null, meta.imageMeta)
        assertEquals(null, meta.animationMeta)
    }

    @Test
    fun testResetCachedMeta() = runBlocking {
        val openSeaItem = ItemId(Address.apply("0x9b1aa69fe9fca10aa41250bba054aabd92aba5b6"), EthUInt256.of(116L))
        println("Testing OpenSea item $openSeaItem")
        testResetCachedMeta(
            openSeaItem,
            "cache_opensea",
            "https://lh3.googleusercontent.com/_koUdMM9s66qz9yyNmoCiFiaeroNN6ZH0vvZXYPaajnTWQsPhhUAXrE_YG-kk4UvELgccw96hJrBQmdzwq_fHSc=s250"
        )

        run {
            val standardItem = ItemId(Address.apply("0x6ebeaf8e8e946f0716e6533a6f2cefc83f60e8ab"), EthUInt256.of(142708))
            println("Testing standard item (not available in OpenSea). $openSeaItem")
            val uri = "https://api.godsunchained.com/card/142708"
            lazyNftItemHistoryRepository.save(ItemLazyMint(
                token = standardItem.token,
                tokenId = standardItem.tokenId,
                value = EthUInt256.ONE,
                date = Instant.now(),
                uri = uri,
                standard = TokenStandard.ERC721,
                creators = listOf(Part(Address.ONE(), 1)),
                royalties = emptyList(),
                signatures = emptyList()
            )).awaitFirstOrNull()

            testResetCachedMeta(
                standardItem,
                "cache_properties",
                "https://images.godsunchained.com/cards/250/43.png"
            )
        }

        run {
            println("Testing saving to the Temporary item properties")
            val imageUrl = "https://cryptodozer.io/static/images/dozer/meta/dolls/100.png"
            val itemProperties = ItemProperties(
                name = "ItemMetaName",
                image = imageUrl,
                description = null,
                imagePreview = null,
                imageBig = null,
                animationUrl = null,
                attributes = emptyList()
            )

            val itemId = ItemId(Address.ONE(), EthUInt256.TEN)
            val tempItemProperties = TemporaryItemProperties(id = itemId.decimalStringValue, value = itemProperties)
            testResetCachedMeta(itemId, "temporary_item_properties", imageUrl) {
                temporaryItemPropertiesRepository.save(tempItemProperties).awaitFirstOrNull()
            }
        }
    }

    private suspend fun testResetCachedMeta(itemId: ItemId, metaCollection: String, metadataKeyToTrack: String, preInsert: suspend () -> Unit = {}) {
        preInsert()
        val meta = itemMetaService.getItemMetadata(itemId)
        assertNotNull(meta.meta.imageMeta)
        assertMetaFoundInMongo(true, metaCollection, itemId)
        assertMongoEntryFound(true, metadataKeyToTrack, "cache_meta")

        itemMetaService.resetMetadata(itemId)
        assertMetaFoundInMongo(false, metaCollection, itemId)
        assertMongoEntryFound(false, metadataKeyToTrack, "cache_meta")

        preInsert()
        val updated = itemMetaService.getItemMetadata(itemId)
        assertEquals(meta, updated)
        assertMetaFoundInMongo(true, metaCollection, itemId)
        assertMongoEntryFound(true, metadataKeyToTrack, "cache_meta")
    }

    private suspend fun assertMetaFoundInMongo(shouldBeFound: Boolean, collectionName: String, itemId: ItemId) {
        val id = "${itemId.token}:${itemId.tokenId.value}"
        assertMongoEntryFound(shouldBeFound, id, collectionName)
    }

    private suspend fun assertMongoEntryFound(shouldBeFound: Boolean, id: String, collectionName: String) {
        val cache = mongo.findById<Any>(id, collectionName).awaitFirstOrNull()
        if (shouldBeFound) {
            assertNotNull(cache)
        } else {
            assertNull(cache)
        }
    }
}
