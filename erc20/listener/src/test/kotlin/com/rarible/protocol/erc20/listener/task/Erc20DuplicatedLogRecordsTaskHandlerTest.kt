package com.rarible.protocol.erc20.listener.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.task.Task
import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.core.admin.Erc20TaskService
import com.rarible.protocol.erc20.core.admin.model.Erc20DuplicatedLogRecordsTaskParam
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.repository.Erc20TransferHistoryRepository
import com.rarible.protocol.erc20.core.repository.data.randomErc20OutcomeTransfer
import com.rarible.protocol.erc20.core.repository.data.randomLogEvent
import com.rarible.protocol.erc20.core.service.Erc20BalanceService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import scalether.domain.Address
import java.math.BigInteger
import java.time.Instant

@ExtendWith(MockKExtension::class)
internal class Erc20DuplicatedLogRecordsTaskHandlerTest {
    @InjectMockKs
    private lateinit var erc20DuplicatedLogRecordsTaskHandler: Erc20DuplicatedLogRecordsTaskHandler

    @SpyK
    private var objectMapper: ObjectMapper = jacksonObjectMapper()

    @MockK
    private lateinit var erc20TransferHistoryRepository: Erc20TransferHistoryRepository

    @MockK
    private lateinit var erc20TaskService: Erc20TaskService

    @MockK
    private lateinit var erc20BalanceService: Erc20BalanceService

    private lateinit var token: Address
    private lateinit var owner: Address
    private lateinit var log: ReversedEthereumLogRecord
    private lateinit var param: String

    @BeforeEach
    fun before() {
        token = randomAddress()
        owner = randomAddress()
        log = randomLogEvent(
            randomErc20OutcomeTransfer(token, owner),
            blockNumber = 1
        ).copy(status = EthereumBlockStatus.CONFIRMED)
        param = objectMapper.writeValueAsString(Erc20DuplicatedLogRecordsTaskParam(update = true))
        coEvery { erc20TransferHistoryRepository.findAll("from") } returns flowOf(log)
    }

    @Test
    fun deduplicate() = runBlocking<Unit> {
        val duplicate1 = log.copy(
            id = ObjectId().toHexString(),
            minorLogIndex = log.minorLogIndex + 1,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val duplicate2 = log.copy(
            id = ObjectId().toHexString(),
            minorLogIndex = log.minorLogIndex + 2,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        coEvery { erc20TransferHistoryRepository.findPossibleDuplicates(log) } returns listOf(duplicate1, duplicate2)
        coEvery { erc20BalanceService.getBlockchainBalance(BalanceId(token, owner)) } returns EthUInt256(BigInteger.ONE)
        coEvery { erc20BalanceService.get(BalanceId(token, owner)) } returns Erc20Balance(
            owner = owner,
            token = token,
            balance = EthUInt256(BigInteger.TEN),
            blockNumber = null,
            createdAt = null,
            lastUpdatedAt = null,
        )
        coEvery { erc20TransferHistoryRepository.removeAll(listOf(duplicate1, duplicate2)) } returns Unit
        coEvery { erc20TaskService.createReduceTask(token = token, owner = owner, force = true) } returns Task(
            param = "",
            running = false,
            type = "type"
        )

        val result = erc20DuplicatedLogRecordsTaskHandler.runLongTask("from", param = param).toList()

        assertThat(result).containsExactly(log.id)
        coVerify {
            erc20TransferHistoryRepository.removeAll(listOf(duplicate1, duplicate2))
            erc20TaskService.createReduceTask(token = token, owner = owner, force = true)
        }
    }

    @Test
    fun `possible duplicates not found`() = runBlocking<Unit> {
        coEvery { erc20TransferHistoryRepository.findPossibleDuplicates(log) } returns emptyList()

        val result = erc20DuplicatedLogRecordsTaskHandler.runLongTask("from", param = param).toList()

        assertThat(result).containsExactly(log.id)

        coVerify(exactly = 0) {
            erc20TransferHistoryRepository.removeAll(any())
            erc20TaskService.createReduceTask(token = token, owner = owner, force = true)
        }
    }

    @Test
    fun `possible duplicates found but not duplicates`() = runBlocking<Unit> {
        val duplicate1 = log.copy(
            id = ObjectId().toHexString(),
            minorLogIndex = log.minorLogIndex + 1,
            blockNumber = (log.blockNumber ?: 0) + 1,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        coEvery { erc20TransferHistoryRepository.findPossibleDuplicates(log) } returns listOf(duplicate1)

        val result = erc20DuplicatedLogRecordsTaskHandler.runLongTask("from", param = param).toList()

        assertThat(result).containsExactly(log.id)

        coVerify(exactly = 0) {
            erc20TransferHistoryRepository.removeAll(any())
            erc20TaskService.createReduceTask(token = token, owner = owner, force = true)
        }
    }

    @Test
    fun `possible duplicates found but balances equal`() = runBlocking<Unit> {
        val duplicate1 = log.copy(
            id = ObjectId().toHexString(),
            minorLogIndex = log.minorLogIndex + 1,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        coEvery { erc20TransferHistoryRepository.findPossibleDuplicates(log) } returns listOf(duplicate1)
        coEvery { erc20BalanceService.getBlockchainBalance(BalanceId(token, owner)) } returns EthUInt256(BigInteger.ONE)
        coEvery { erc20BalanceService.get(BalanceId(token, owner)) } returns Erc20Balance(
            owner = owner,
            token = token,
            balance = EthUInt256(BigInteger.ONE),
            blockNumber = null,
            createdAt = null,
            lastUpdatedAt = null,
        )

        val result = erc20DuplicatedLogRecordsTaskHandler.runLongTask("from", param = param).toList()

        assertThat(result).containsExactly(log.id)

        coVerify(exactly = 0) {
            erc20TransferHistoryRepository.removeAll(any())
            erc20TaskService.createReduceTask(token = token, owner = owner, force = true)
        }
    }

    @Test
    fun `possible duplicates found but update disabled`() = runBlocking<Unit> {
        val duplicate1 = log.copy(
            id = ObjectId().toHexString(),
            minorLogIndex = log.minorLogIndex + 1,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        coEvery { erc20TransferHistoryRepository.findPossibleDuplicates(log) } returns listOf(duplicate1)
        coEvery { erc20BalanceService.getBlockchainBalance(BalanceId(token, owner)) } returns EthUInt256(BigInteger.ONE)
        coEvery { erc20BalanceService.get(BalanceId(token, owner)) } returns Erc20Balance(
            owner = owner,
            token = token,
            balance = EthUInt256(BigInteger.TEN),
            blockNumber = null,
            createdAt = null,
            lastUpdatedAt = null,
        )

        val result = erc20DuplicatedLogRecordsTaskHandler.runLongTask(
            "from",
            param = objectMapper.writeValueAsString(Erc20DuplicatedLogRecordsTaskParam(update = false))
        ).toList()

        assertThat(result).containsExactly(log.id)

        coVerify(exactly = 0) {
            erc20TransferHistoryRepository.removeAll(any())
            erc20TaskService.createReduceTask(token = token, owner = owner, force = true)
        }
    }
}
