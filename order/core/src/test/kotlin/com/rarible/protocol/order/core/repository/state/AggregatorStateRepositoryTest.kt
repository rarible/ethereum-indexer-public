package com.rarible.protocol.order.core.repository.state

import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.protocol.order.core.TestPropertiesConfiguration
import com.rarible.protocol.order.core.configuration.RepositoryConfiguration
import com.rarible.protocol.order.core.model.LooksrareFetchState
import com.rarible.protocol.order.core.model.SeaportFetchState
import com.rarible.protocol.order.core.model.X2Y2FetchState
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@MongoTest
@MongoCleanup
@DataMongoTest
@ContextConfiguration(classes = [RepositoryConfiguration::class, TestPropertiesConfiguration::class])
@EnableAutoConfiguration
@ActiveProfiles("integration")
internal class AggregatorStateRepositoryTest {

    @Autowired
    private lateinit var repository: AggregatorStateRepository

    @Test
    fun `should save and get seaport state`() = runBlocking<Unit> {
        val noState = repository.getSeaportState()
        Assertions.assertThat(noState).isNull()

        val initState = SeaportFetchState("1")
        repository.save(initState)

        var currentState = repository.getSeaportState()
        Assertions.assertThat(currentState).isEqualTo(initState)

        val newState = SeaportFetchState("2")
        repository.save(newState)

        currentState = repository.getSeaportState()
        Assertions.assertThat(currentState).isEqualTo(newState)
    }

    @Test
    fun `should save and get x2y2 state`() = runBlocking<Unit> {
        val noState = repository.getX2Y2State()
        Assertions.assertThat(noState).isNull()

        val initState = X2Y2FetchState("1")
        repository.save(initState)

        var currentState = repository.getX2Y2State()
        Assertions.assertThat(currentState).isEqualTo(initState)

        val newState = X2Y2FetchState("2")
        repository.save(newState)

        currentState = repository.getX2Y2State()
        Assertions.assertThat(currentState).isEqualTo(newState)
    }

    @Test
    fun `should save and get looksrare state`() = runBlocking<Unit> {
        val noState = repository.getLooksrareState()
        Assertions.assertThat(noState).isNull()

        val initState = LooksrareFetchState("1")
        repository.save(initState)

        var currentState = repository.getLooksrareState()
        Assertions.assertThat(currentState).isEqualTo(initState)

        val newState = LooksrareFetchState("2")
        repository.save(newState)

        currentState = repository.getLooksrareState()
        Assertions.assertThat(currentState).isEqualTo(newState)
    }
}
