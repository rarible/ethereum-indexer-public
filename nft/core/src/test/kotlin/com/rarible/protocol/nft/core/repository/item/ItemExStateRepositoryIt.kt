package com.rarible.protocol.nft.core.repository.item

import com.rarible.protocol.nft.core.data.randomItemExState
import com.rarible.protocol.nft.core.test.AbstractIntegrationTest
import com.rarible.protocol.nft.core.test.IntegrationTest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class ItemExStateRepositoryIt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var stateRepository: ItemExStateRepository

    @Test
    fun `save and get state`() = runBlocking<Unit> {
        val state = randomItemExState()

        val notExistedState = stateRepository.getById(state.id)
        assertThat(notExistedState).isNull()

        stateRepository.save(state)
        val saved = stateRepository.getById(state.id)
        assertThat(saved).isNotNull
    }

    @Test
    fun `find all`() = runBlocking<Unit> {
        val states = listOf(randomItemExState(), randomItemExState())
        states.forEach { stateRepository.save(it) }

        val saved = stateRepository.getAll(null).toList()
        assertThat(saved.map { it.id }).containsExactlyInAnyOrderElementsOf(states.map { it.id })
    }
}
