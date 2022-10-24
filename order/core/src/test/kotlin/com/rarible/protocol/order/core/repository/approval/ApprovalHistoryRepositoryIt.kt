package com.rarible.protocol.order.core.repository.approval

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.ext.MongoTest
import com.rarible.protocol.order.core.TestPropertiesConfiguration
import com.rarible.protocol.order.core.configuration.RepositoryConfiguration
import com.rarible.protocol.order.core.data.createLogEvent
import com.rarible.protocol.order.core.data.randomApproveHistory
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@MongoTest
@DataMongoTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [RepositoryConfiguration::class, TestPropertiesConfiguration::class])
@ActiveProfiles("integration")
internal class ApprovalHistoryRepositoryIt {

    @Autowired
    private lateinit var approvalRepository: ApprovalHistoryRepository

    @Test
    fun `should get latest approval`() = runBlocking<Unit> {
        val owner = randomAddress()
        val collection = randomAddress()
        val operator = randomAddress()

        val approval1 = randomApproveHistory(owner = owner, collection = collection, operator = operator)
        val event1 = createLogEvent(approval1).copy(blockNumber = 1, logIndex = 1, minorLogIndex = 1)

        val approval2 = randomApproveHistory(owner = owner, collection = collection, operator = operator)
        val event2 = createLogEvent(approval2).copy(blockNumber = 2, logIndex = 1, minorLogIndex = 1)

        val approval3 = randomApproveHistory(owner = owner, collection = collection, operator = operator)
        val event3 = createLogEvent(approval3).copy(blockNumber = 2, logIndex = 2, minorLogIndex = 1)

        listOf(
            event1,
            createLogEvent(randomApproveHistory()),
            event2,
            createLogEvent(randomApproveHistory()),
            event3,
            createLogEvent(randomApproveHistory()),
        ).shuffled().forEach { approvalRepository.save(it) }

        val result = approvalRepository.lastApprovalLogEvent(collection = collection, owner = owner, operator = operator)
        assertThat(result?.id).isEqualTo(event3.id)
    }
}
