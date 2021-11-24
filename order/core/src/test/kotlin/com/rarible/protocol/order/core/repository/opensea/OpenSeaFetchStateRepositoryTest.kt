package com.rarible.protocol.order.core.repository.opensea

import com.rarible.core.test.ext.MongoTest
import com.rarible.protocol.order.core.configuration.RepositoryConfiguration
import com.rarible.protocol.order.core.model.OpenSeaFetchState
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.ContextConfiguration

@MongoTest
@DataMongoTest
@ContextConfiguration(classes = [RepositoryConfiguration::class])
@EnableAutoConfiguration
internal class OpenSeaFetchStateRepositoryTest {

    @Autowired
    private lateinit var openSeaFetchStateRepository: OpenSeaFetchStateRepository

    @BeforeEach
    fun setup() = runBlocking {
        openSeaFetchStateRepository.delete()
    }

    @Test
    fun `should save and get fetch state`() = runBlocking<Unit> {
        val noState = openSeaFetchStateRepository.get()
        assertThat(noState).isNull()

        val initState = OpenSeaFetchState(1)
        openSeaFetchStateRepository.save(initState)

        var currentState = openSeaFetchStateRepository.get()
        assertThat(currentState).isEqualTo(initState)

        val newState = OpenSeaFetchState(2)
        openSeaFetchStateRepository.save(newState)

        currentState = openSeaFetchStateRepository.get()
        assertThat(currentState).isEqualTo(newState)
    }
}
