package com.rarible.protocol.nft.migration.service

import com.rarible.core.task.EnableRaribleTask
import com.rarible.core.task.TaskRepository
import com.rarible.protocol.nft.migration.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.migration.integration.IntegrationTest
import com.rarible.protocol.nft.migration.mongock.mongo.ChangeLog00015StartRoyalty
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
@EnableRaribleTask
class StartRoyaltyTest : AbstractIntegrationTest() {

    private val migration = ChangeLog00015StartRoyalty()

    @Autowired
    protected lateinit var taskRepository: TaskRepository

    @Test
    fun `should start royalty`() = runBlocking {
        migration.start(taskRepository, mongo)
        val found = taskRepository.findByTypeAndParam(ChangeLog00015StartRoyalty.TASK_NAME, "").awaitSingle()
        assertNotNull(found.id)
    }
}
