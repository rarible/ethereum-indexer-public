package com.rarible.protocol.nft.migration.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.api.service.item.meta.ItemPropertiesService
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.repository.item.ItemPropertyRepository
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
import scalether.domain.Address
import java.math.BigInteger

@IntegrationTest
class CryptoPunkMetaMigrationTest : AbstractIntegrationTest() {

    private val insertAttributes = ChangeLog00013InsertAttributesForCryptoPunks()

    @Autowired
    private lateinit var itemPropertyRepository: ItemPropertyRepository

    @Autowired
    private lateinit var mapper: ObjectMapper

    @Autowired
    private lateinit var itemPropertiesService: ItemPropertiesService

    @Test
    fun `should get attributes after migration`() = runBlocking {
        val token = Address.apply(nftIndexerProperties.cryptoPunksContractAddress)
        val tokenId = BigInteger.valueOf(2L)
        insertAttributes.create(itemPropertyRepository, mapper, nftIndexerProperties)

        val itemId = ItemId(token, EthUInt256.of(tokenId))
        assertEquals(10000, itemPropertyRepository.count().awaitSingle())
        val props = mapper.readValue(itemPropertyRepository.get(itemId).awaitFirstOrNull(), Map::class.java)
        assertEquals("Human", props.get("type"))

        val itemProps = itemPropertiesService.getProperties(token, tokenId).awaitFirstOrNull()
        assertThat(itemProps?.attributes).contains(ItemAttribute("type", "Human"))
        assertThat(itemProps?.attributes).contains(ItemAttribute("gender", "Female"))
        assertThat(itemProps?.attributes).contains(ItemAttribute("skin tone", "Light"))
        assertThat(itemProps?.attributes).contains(ItemAttribute("count", "1"))
        assertThat(itemProps?.attributes).contains(ItemAttribute("accessory", "Wild Hair"))
        assertEquals(5, itemProps?.attributes?.size)
    }
}
