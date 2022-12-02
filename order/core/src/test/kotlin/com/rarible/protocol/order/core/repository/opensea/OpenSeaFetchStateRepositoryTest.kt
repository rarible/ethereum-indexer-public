package com.rarible.protocol.order.core.repository.opensea

import com.rarible.protocol.order.core.integration.AbstractIntegrationTest
import com.rarible.protocol.order.core.integration.IntegrationTest
import com.rarible.protocol.order.core.model.OpenSeaFetchState
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class OpenSeaFetchStateRepositoryTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var openSeaFetchStateRepository: OpenSeaFetchStateRepository

    @BeforeEach
    fun setup() = runBlocking {
        openSeaFetchStateRepository.delete()
    }

    @Test
    fun `should save and get fetch state`() = runBlocking<Unit> {
        val noState = openSeaFetchStateRepository.get(OpenSeaFetchState.ID)
        assertThat(noState).isNull()

        val initState = OpenSeaFetchState(1)
        openSeaFetchStateRepository.save(initState)

        var currentState = openSeaFetchStateRepository.get(OpenSeaFetchState.ID)
        assertThat(currentState).isEqualTo(initState)

        val newState = OpenSeaFetchState(2)
        openSeaFetchStateRepository.save(newState)

        currentState = openSeaFetchStateRepository.get(OpenSeaFetchState.ID)
        assertThat(currentState).isEqualTo(newState)
    }
}
