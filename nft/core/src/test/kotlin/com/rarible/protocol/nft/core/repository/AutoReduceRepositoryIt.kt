package com.rarible.protocol.nft.core.repository

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.nft.core.data.createRandomItemId
import com.rarible.protocol.nft.core.model.AutoReduce
import com.rarible.protocol.nft.core.test.AbstractIntegrationTest
import com.rarible.protocol.nft.core.test.IntegrationTest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class AutoReduceRepositoryIt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var autoReduceRepository: AutoReduceRepository

    @Test
    fun crud() = runBlocking<Unit> {
        val item1 = AutoReduce(createRandomItemId().toString())
        val item2 = AutoReduce(createRandomItemId().toString())
        autoReduceRepository.saveItems(listOf(item1))
        autoReduceRepository.saveItems(listOf(item1, item2))
        val token1 = AutoReduce(randomAddress().toString())
        val token2 = AutoReduce(randomAddress().toString())
        autoReduceRepository.saveTokens(listOf(token1, token2))

        assertThat(autoReduceRepository.findItems().toList()).containsExactlyInAnyOrder(item1, item2)
        assertThat(autoReduceRepository.findTokens().toList()).containsExactlyInAnyOrder(token1, token2)

        autoReduceRepository.removeItem(item1)
        autoReduceRepository.removeToken(token1)

        assertThat(autoReduceRepository.findItems().toList()).containsExactlyInAnyOrder(item2)
        assertThat(autoReduceRepository.findTokens().toList()).containsExactlyInAnyOrder(token2)
    }
}
