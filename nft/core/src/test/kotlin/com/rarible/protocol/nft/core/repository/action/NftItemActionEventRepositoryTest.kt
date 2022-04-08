package com.rarible.protocol.nft.core.repository.action

import com.rarible.protocol.nft.core.data.createRandomBurnAction
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class NftItemActionEventRepositoryIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var repository: NftItemActionEventRepository

    @Test
    fun `should save and get ownership`() = runBlocking<Unit> {
        val action = createRandomBurnAction()
        repository.save(action).awaitFirst()
        val actions = repository.findByItemIdAndType(action.itemId(), action.type)
        assertThat(actions).hasSize(1)
        assertThat(actions.single().id).isEqualTo(action.id)
    }
}
