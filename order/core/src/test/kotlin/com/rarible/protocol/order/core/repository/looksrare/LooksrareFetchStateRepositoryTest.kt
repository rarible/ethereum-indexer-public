package com.rarible.protocol.order.core.repository.looksrare

import com.rarible.core.test.ext.MongoTest
import com.rarible.protocol.order.core.TestPropertiesConfiguration
import com.rarible.protocol.order.core.configuration.RepositoryConfiguration
import com.rarible.protocol.order.core.model.LooksrareFetchState
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@MongoTest
@DataMongoTest
@ContextConfiguration(classes = [RepositoryConfiguration::class, TestPropertiesConfiguration::class])
@EnableAutoConfiguration
@ActiveProfiles("integration")
internal class LooksrareFetchStateRepositoryTest {

    @Autowired
    private lateinit var openSeaFetchStateRepository: LooksrareFetchStateRepository

    @BeforeEach
    fun setup() = runBlocking {
        openSeaFetchStateRepository.delete()
    }

    @Test
    fun `should save and get fetch state`() = runBlocking<Unit> {
        val noState = openSeaFetchStateRepository.get(LooksrareFetchState.ID)
        assertThat(noState).isNull()

        val initState = LooksrareFetchState("1")
        openSeaFetchStateRepository.save(initState)

        var currentState = openSeaFetchStateRepository.get(LooksrareFetchState.ID)
        assertThat(currentState).isEqualTo(initState)

        val newState = LooksrareFetchState("2")
        openSeaFetchStateRepository.save(newState)

        currentState = openSeaFetchStateRepository.get(LooksrareFetchState.ID)
        assertThat(currentState).isEqualTo(newState)
    }
}
