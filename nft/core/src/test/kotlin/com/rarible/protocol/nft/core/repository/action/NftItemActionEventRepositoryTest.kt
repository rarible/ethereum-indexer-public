package com.rarible.protocol.nft.core.repository.action

import com.rarible.core.common.nowMillis
import com.rarible.protocol.nft.core.data.createRandomBurnAction
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

@IntegrationTest
internal class NftItemActionEventRepositoryIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var repository: NftItemActionEventRepository

    @Test
    fun `should save and get action`() = runBlocking<Unit> {
        val action = createRandomBurnAction()
        repository.save(action).awaitFirst()
        val actions = repository.findByItemIdAndType(action.itemId(), action.type)
        assertThat(actions).hasSize(1)
        assertThat(actions.single().id).isEqualTo(action.id)
    }

    @Test
    fun `should get all pending actions ready to be executed`() = runBlocking<Unit> {
        val now = nowMillis()
        val action1 = createRandomBurnAction().copy(actionAt = now - Duration.ofDays(2))
        val action2 = createRandomBurnAction().copy(actionAt = now - Duration.ofDays(1))
        val action3 = createRandomBurnAction().copy(actionAt = now)
        val action4 = createRandomBurnAction().copy(actionAt = now + Duration.ofDays(1))

        listOf(action1, action2, action3, action4).forEach {
            repository.save(it).awaitFirst()
        }
        val actions = repository.findPendingActions(now).toList()
        assertThat(actions).hasSize(3)
        assertThat(actions.map { it.id }).containsExactlyInAnyOrder(action1.id, action2.id, action3.id)
    }
}
