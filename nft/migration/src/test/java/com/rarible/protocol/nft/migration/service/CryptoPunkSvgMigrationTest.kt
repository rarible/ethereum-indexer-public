package com.rarible.protocol.nft.migration.service

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.CryptoPunksMeta
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.IpfsService
import com.rarible.protocol.nft.core.service.item.meta.descriptors.CryptoPunksPropertiesResolver
import com.rarible.protocol.nft.migration.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.migration.integration.IntegrationTest
import com.rarible.protocol.nft.migration.mongock.mongo.ChangeLog00013InsertAttributesForCryptoPunks
import com.rarible.protocol.nft.migration.mongock.mongo.ChangeLog00014UploadSvgsForCryptoPunks
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.exists
import scalether.domain.Address
import java.math.BigInteger

@IntegrationTest
@Disabled
class CryptoPunkSvgMigrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var cryptoPunksPropertiesResolver: CryptoPunksPropertiesResolver

    @Autowired
    private lateinit var ipfsService: IpfsService

    @Test
    fun `should save svg image`() = runBlocking<Unit> {
        val token = Address.apply(nftIndexerProperties.cryptoPunksContractAddress)
        val tokenId = BigInteger.valueOf(2L)

        val punk = "2, Human, Female, Light, 1, Wild Hair"
        ChangeLog00013InsertAttributesForCryptoPunks().savePunk(punk, cryptoPunksPropertiesResolver)

        val testPunkSvg = javaClass.getResourceAsStream("/data/cryptopunks/2.svg")!!.readBytes()
        ChangeLog00014UploadSvgsForCryptoPunks().upload("2.svg", testPunkSvg, cryptoPunksPropertiesResolver, ipfsService)

        assertEquals(1, mongo.count(Query(), "cryptopunks_meta").awaitSingle())

        val itemProps = cryptoPunksPropertiesResolver.resolve(ItemId(token, EthUInt256(tokenId)))
        assertThat(itemProps).isEqualTo(
            ItemProperties(
                name = "CryptoPunk #2",
                image = "${IpfsService.RARIBLE_IPFS}/ipfs/QmWMVUQ4QidzC2rg6hBEJMgihizraW29hStyVLNPfmU4WS",
                description = null,
                imagePreview = null,
                imageBig = null,
                animationUrl = null,
                attributes = listOf(
                    ItemAttribute("type", "Human"),
                    ItemAttribute("gender", "Female"),
                    ItemAttribute("skin tone", "Light"),
                    ItemAttribute("count", "1"),
                    ItemAttribute("accessory", "Wild Hair")
                ),
                rawJsonContent = null
            )
        )
    }

    @Test
    fun `should upload all svg images`() = runBlocking {
        ChangeLog00013InsertAttributesForCryptoPunks().insertCryptoPunksAttributes(cryptoPunksPropertiesResolver)
        ChangeLog00014UploadSvgsForCryptoPunks().uploadCryptoPunksSvgs(cryptoPunksPropertiesResolver, ipfsService)
        assertEquals(10000, mongo.count(Query(CryptoPunksMeta::image exists true), "cryptopunks_meta").awaitSingle())
    }
}
