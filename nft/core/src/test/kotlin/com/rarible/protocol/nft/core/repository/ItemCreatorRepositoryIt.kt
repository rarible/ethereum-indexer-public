package com.rarible.protocol.nft.core.repository

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.ItemCreator
import com.rarible.protocol.nft.core.model.ItemId
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.AddressFactory

@IntegrationTest
class ItemCreatorRepositoryIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var itemCreatorRepository: ItemCreatorRepository

    @Test
    fun saveFind() = runBlocking<Unit> {
        val creator = AddressFactory.create()
        val saved = itemCreatorRepository.save(ItemCreator(ItemId(AddressFactory.create(), EthUInt256.TEN), creator))
            .awaitFirst()
        assertThat(itemCreatorRepository.findById(saved.id).awaitFirst())
            .hasFieldOrPropertyWithValue(ItemCreator::creator.name, creator)
    }
}
