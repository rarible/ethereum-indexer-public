package com.rarible.protocol.nft.migration.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.protocol.nft.api.service.item.meta.ItemPropertiesService
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.repository.item.ItemPropertyRepository
import com.rarible.protocol.nft.migration.configuration.IpfsProperties
import com.rarible.protocol.nft.migration.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.migration.integration.IntegrationTest
import com.rarible.protocol.nft.migration.mongock.mongo.ChangeLog00013InsertAttributesForCryptoPunks
import com.rarible.protocol.nft.migration.mongock.mongo.ChangeLog00014UploadSvgsForCryptoPunks
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import java.math.BigInteger

@IntegrationTest
class CryptoPunkSvgMigrationTest : AbstractIntegrationTest() {

    private val insertAttributes = ChangeLog00013InsertAttributesForCryptoPunks()
    private val uploaderSvg = ChangeLog00014UploadSvgsForCryptoPunks()

    @Autowired
    private lateinit var itemPropertyRepository: ItemPropertyRepository

    @Autowired
    private lateinit var mapper: ObjectMapper

    @Autowired
    private lateinit var itemPropertiesService: ItemPropertiesService

    @Autowired
    private lateinit var ipfsProperties: IpfsProperties

    @Test
    fun `should save svg image`() = runBlocking {
        val token = Address.apply(nftIndexerProperties.cryptoPunksContractAddress)
        val tokenId = BigInteger.valueOf(2L)

        val punk = "2, Human, Female, Light, 1, Wild Hair"
        insertAttributes.savePunk(punk, itemPropertyRepository, mapper, nftIndexerProperties)
        uploaderSvg.upload("2.svg", itemPropertyRepository, mapper, nftIndexerProperties, ipfsProperties)

        assertEquals(1, itemPropertyRepository.count().awaitSingle())

        val itemProps = itemPropertiesService.getProperties(token, tokenId).awaitFirstOrNull()
        assertEquals("CryptoPunk #2", itemProps?.name)
        assertEquals("https://rarible.mypinata.cloud/ipfs/QmezgTfa7f3x4BkcSbDqUa8iuX5AFK1oiBnpFrJD5MwzE8", itemProps?.image)
        assertThat(itemProps?.attributes).contains(ItemAttribute("type", "Human"))
        assertThat(itemProps?.attributes).contains(ItemAttribute("gender", "Female"))
        assertThat(itemProps?.attributes).contains(ItemAttribute("skin tone", "Light"))
        assertThat(itemProps?.attributes).contains(ItemAttribute("count", "1"))
        assertThat(itemProps?.attributes).contains(ItemAttribute("accessory", "Wild Hair"))
        assertEquals(5, itemProps?.attributes?.size)
    }

    // It's a very slow test
    @Disabled
    @Test
    fun `should upload all svg images`() = runBlocking {
        insertAttributes.create(itemPropertyRepository, mapper, nftIndexerProperties)
        uploaderSvg.create(itemPropertyRepository, mapper, nftIndexerProperties, ipfsProperties)
        assertEquals(10000, itemPropertyRepository.count().awaitSingle())
    }
}
