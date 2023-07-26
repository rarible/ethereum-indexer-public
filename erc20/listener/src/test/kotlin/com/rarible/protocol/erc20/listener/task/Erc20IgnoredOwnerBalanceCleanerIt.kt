package com.rarible.protocol.erc20.listener.task

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.erc20.core.repository.Erc20ApprovalHistoryRepository
import com.rarible.protocol.erc20.core.repository.Erc20BalanceRepository
import com.rarible.protocol.erc20.core.repository.Erc20TransferHistoryRepository
import com.rarible.protocol.erc20.core.repository.data.randomBalance
import com.rarible.protocol.erc20.core.repository.data.randomErc20IncomeTransfer
import com.rarible.protocol.erc20.core.repository.data.randomErc20TokenApproval
import com.rarible.protocol.erc20.core.repository.data.randomLogEvent
import com.rarible.protocol.erc20.listener.service.IgnoredOwnersResolver
import com.rarible.protocol.erc20.listener.test.IntegrationTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class Erc20IgnoredOwnerBalanceCleanerIt {

    private val ignoredOwner = randomAddress()

    @Autowired
    lateinit var erc20BalanceRepository: Erc20BalanceRepository

    @Autowired
    lateinit var erc20ApprovalHistoryRepository: Erc20ApprovalHistoryRepository

    @Autowired
    lateinit var erc20TransferHistoryRepository: Erc20TransferHistoryRepository

    private val ignoredOwnersResolver: IgnoredOwnersResolver = mockk {
        coEvery { resolve() } returns setOf(ignoredOwner)
    }

    lateinit var cleaner: Erc20IgnoredOwnerBalanceCleaner

    @BeforeEach
    fun beforeEach() {
        cleaner = Erc20IgnoredOwnerBalanceCleaner(
            ignoredOwnersResolver,
            erc20BalanceRepository,
            erc20ApprovalHistoryRepository,
            erc20TransferHistoryRepository
        )
    }

    @Test
    fun `cleanup - balances`() = runBlocking<Unit> {
        val other = erc20BalanceRepository.save(randomBalance())
        val ignored = erc20BalanceRepository.save(randomBalance(owner = ignoredOwner))
        cleaner.cleanup()
        assertThat(erc20BalanceRepository.get(other.id)).isNotNull()
        assertThat(erc20BalanceRepository.get(ignored.id)).isNull()
    }

    @Test
    fun `cleanup - history`() = runBlocking<Unit> {
        val other = randomErc20IncomeTransfer()
        val otherLog = erc20TransferHistoryRepository.save(randomLogEvent(other)).awaitFirst()

        val ignored = randomErc20IncomeTransfer(owner = ignoredOwner)
        val ignoredLog = erc20TransferHistoryRepository.save(randomLogEvent(ignored)).awaitFirst()

        cleaner.cleanup()
        val fromDb = erc20TransferHistoryRepository.findAll(null).toList().map { it.id }
        assertThat(fromDb).contains(otherLog.id)
        assertThat(fromDb).doesNotContain(ignoredLog.id)
    }

    @Test
    fun `cleanup - approvals`() = runBlocking<Unit> {
        val other = randomErc20TokenApproval()
        erc20ApprovalHistoryRepository.save(randomLogEvent(other)).awaitFirst()

        val ignored = randomErc20TokenApproval(owner = ignoredOwner)
        erc20ApprovalHistoryRepository.save(randomLogEvent(ignored)).awaitFirst()

        cleaner.cleanup()
        val deleted = erc20ApprovalHistoryRepository.findOwnerLogEvents(null, ignored.owner).awaitFirstOrNull()
        assertThat(deleted).isNull()

        val kept = erc20ApprovalHistoryRepository.findOwnerLogEvents(null, other.owner).awaitFirstOrNull()
        assertThat(kept).isNotNull
    }
}
