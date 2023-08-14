package com.rarible.protocol.nft.core.repository.action

import com.rarible.core.common.nowMillis
import com.rarible.protocol.nft.core.data.createRandomBurnAction
import com.rarible.protocol.nft.core.model.ActionState
import com.rarible.protocol.nft.core.test.AbstractIntegrationTest
import com.rarible.protocol.nft.core.test.IntegrationTest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

@IntegrationTest
internal class NftItemActionEventRepositoryIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var repository: NftItemActionEventRepository

    @BeforeEach
    fun setupIndexes() = runBlocking<Unit> {
        repository.createIndexes()
    }

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
        val action1 = createRandomBurnAction().copy(actionAt = now - Duration.ofDays(2), state = ActionState.PENDING)
        val action2 = createRandomBurnAction().copy(actionAt = now - Duration.ofDays(2), state = ActionState.EXECUTED)
        val action3 = createRandomBurnAction().copy(actionAt = now - Duration.ofDays(1), state = ActionState.PENDING)
        val action4 = createRandomBurnAction().copy(actionAt = now, state = ActionState.PENDING)
        val action5 = createRandomBurnAction().copy(actionAt = now, state = ActionState.EXECUTED)
        val action6 = createRandomBurnAction().copy(actionAt = now + Duration.ofDays(1), state = ActionState.PENDING)

        listOf(action1, action2, action3, action4, action5, action6).forEach {
            repository.save(it).awaitFirst()
        }
        val actions = repository.findPendingActions(now).toList()
        assertThat(actions).hasSize(3)
        assertThat(actions.map { it.id }).containsExactlyInAnyOrder(action1.id, action3.id, action4.id)
    }
}
