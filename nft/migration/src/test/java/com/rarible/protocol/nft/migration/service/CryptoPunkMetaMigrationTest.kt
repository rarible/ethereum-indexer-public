package com.rarible.protocol.nft.migration.service

import com.rarible.protocol.nft.core.model.CryptoPunksMeta
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.service.item.meta.descriptors.CryptoPunksPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.descriptors.CryptoPunksRepository
import com.rarible.protocol.nft.migration.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.migration.integration.IntegrationTest
import com.rarible.protocol.nft.migration.mongock.mongo.ChangeLog00013InsertAttributesForCryptoPunks
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Query
import java.math.BigInteger

@IntegrationTest
class CryptoPunkMetaMigrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var cryptoPunksRepository: CryptoPunksRepository

    @Autowired
    private lateinit var cryptoPunksPropertiesResolver: CryptoPunksPropertiesResolver

    @Test
    fun `should get attributes after migration`() = runBlocking<Unit> {
        val tokenId = BigInteger.valueOf(2L)
        ChangeLog00013InsertAttributesForCryptoPunks().insertCryptoPunksAttributes(cryptoPunksPropertiesResolver)

        val count = mongo.count(Query(), "cryptopunks_meta").awaitSingle()
        assertThat(count).isEqualTo(10000)

        val props = cryptoPunksRepository.findById(tokenId).awaitSingle()
        assertThat(props).isEqualTo(
            CryptoPunksMeta(
                id = tokenId,
                image = null,
                attributes = listOf(
                    ItemAttribute("type", "Human"),
                    ItemAttribute("gender", "Female"),
                    ItemAttribute("skin tone", "Light"),
                    ItemAttribute("count", "1"),
                    ItemAttribute("accessory", "Wild Hair")
                )
            )
        )
    }
}
