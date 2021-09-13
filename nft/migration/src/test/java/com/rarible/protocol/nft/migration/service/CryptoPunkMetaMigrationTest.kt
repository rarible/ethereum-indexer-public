package com.rarible.protocol.nft.migration.service

import com.rarible.protocol.nft.api.service.item.meta.ItemPropertiesService
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.repository.CryptoPunksRepository
import com.rarible.protocol.nft.core.service.CryptoPunksMetaService
import com.rarible.protocol.nft.migration.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.migration.integration.IntegrationTest
import com.rarible.protocol.nft.migration.mongock.mongo.ChangeLog00013InsertAttributesForCryptoPunks
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Query
import scalether.domain.Address
import java.math.BigInteger

@IntegrationTest
class CryptoPunkMetaMigrationTest : AbstractIntegrationTest() {

    private val insertAttributes = ChangeLog00013InsertAttributesForCryptoPunks()

    @Autowired
    private lateinit var itemPropertiesService: ItemPropertiesService

    @Autowired
    private lateinit var cryptoPunksRepository: CryptoPunksRepository

    @Autowired
    private lateinit var cryptoPunksMetaService: CryptoPunksMetaService

    @Test
    fun `should get attributes after migration`() = runBlocking {
        val token = Address.apply(nftIndexerProperties.cryptoPunksContractAddress)
        val tokenId = BigInteger.valueOf(2L)
        insertAttributes.create(cryptoPunksMetaService)

        assertEquals(10000, mongo.count(Query(), "cryptopunks_meta").awaitSingle())
        val props = cryptoPunksRepository.findById(tokenId).awaitSingle()
        assertEquals("Human", props.attributes.first { it.key.equals("type") }.value)

        val itemProps = itemPropertiesService.getProperties(token, tokenId).awaitFirstOrNull()
        assertThat(itemProps?.attributes).contains(ItemAttribute("type", "Human"))
        assertThat(itemProps?.attributes).contains(ItemAttribute("gender", "Female"))
        assertThat(itemProps?.attributes).contains(ItemAttribute("skin tone", "Light"))
        assertThat(itemProps?.attributes).contains(ItemAttribute("count", "1"))
        assertThat(itemProps?.attributes).contains(ItemAttribute("accessory", "Wild Hair"))
        assertEquals(5, itemProps?.attributes?.size)
    }
}
