package com.rarible.protocol.nft.api.e2e.collection

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.core.task.Task
import com.rarible.core.task.TaskStatus
import com.rarible.core.test.data.randomBigInt
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ReduceTokenItemsTaskParams
import com.rarible.protocol.nft.core.model.ReduceTokenRangeTaskParams
import com.rarible.protocol.nft.core.model.ReindexTokenItemRoyaltiesTaskParam
import com.rarible.protocol.nft.core.model.ReindexTokenItemsTaskParams
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TempTaskRepository
import com.rarible.protocol.nft.core.service.ReindexTokenService
import com.rarible.protocol.nft.core.service.TaskSchedulingService
import com.rarible.protocol.nft.core.service.token.TokenService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigInteger

class ReindexTokenServiceTest {

    private val tokenService = mockk<TokenService>()
    private val taskRepository = mockk<TempTaskRepository>() {
        coEvery { save(any()) } coAnswers { firstArg() }
        coEvery { delete(any()) } returns Unit
    }
    private val taskSchedulingService = TaskSchedulingService(taskRepository)

    private val service = ReindexTokenService(
        tokenService,
        taskRepository,
        taskSchedulingService,
        mockk(),
        ObjectMapper().registerKotlinModule()
    )

    @Test
    fun `should create token reindex task`() = runBlocking<Unit> {
        val token1 = AddressFactory.create()
        val token2 = AddressFactory.create()

        mockGetTokenStandard(token1, TokenStandard.ERC721)
        mockGetTokenStandard(token2, TokenStandard.ERC721)
        mockTaskRepositoryFindNothingByType(ReindexTokenItemsTaskParams.ADMIN_REINDEX_TOKEN_ITEMS)

        val task = service.createReindexTokenItemsTask(listOf(token1, token2), 100, false)
        assertThat(task.lastStatus).isEqualTo(TaskStatus.NONE)
        assertThat(task.running).isEqualTo(false)
        assertThat(task.state).isEqualTo(100L)

        with(ReindexTokenItemsTaskParams.fromParamString(task.param)) {
            assertThat(tokens).isEqualTo(listOf(token1, token2))
            assertThat(standard).isEqualTo(TokenStandard.ERC721)
        }
    }

    @Test
    fun `should create token reduce task`() = runBlocking<Unit> {
        val targetToken = AddressFactory.create()

        mockTaskRepositoryFindNothingByType(ReduceTokenItemsTaskParams.ADMIN_REDUCE_TOKEN_ITEMS)

        val task = service.createReduceTokenItemsTask(targetToken, false)
        assertThat(task.lastStatus).isEqualTo(TaskStatus.NONE)
        assertThat(task.running).isEqualTo(false)
        assertThat(task.state).isNull()

        with(ReduceTokenItemsTaskParams.fromParamString(task.param)) {
            assertThat(tokens).isEqualTo(tokens)
        }
    }

    @Test
    fun `should create token reduce task forcibly`() = runBlocking<Unit> {
        val targetToken = AddressFactory.create()

        mockTaskRepositoryFindTask(
            ReduceTokenItemsTaskParams.ADMIN_REDUCE_TOKEN_ITEMS,
            ReduceTokenItemsTaskParams(targetToken).toParamString(),
            running = false,
            lastStatus = TaskStatus.COMPLETED
        )

        val task = service.createReduceTokenItemsTask(targetToken, true)
        assertThat(task.lastStatus).isEqualTo(TaskStatus.NONE)
        assertThat(task.running).isEqualTo(false)
        assertThat(task.state).isNull()

        with(ReduceTokenItemsTaskParams.fromParamString(task.param)) {
            assertThat(oneToken).isEqualTo(targetToken)
        }
    }

    @Test
    fun `should create token items royalties reindex task`() = runBlocking<Unit> {
        val targetToken = AddressFactory.create()

        mockTaskRepositoryFindNothingByType(ReindexTokenItemRoyaltiesTaskParam.ADMIN_REINDEX_TOKEN_ITEM_ROYALTIES)

        val task = service.createReindexTokenItemRoyaltiesTask(targetToken, false)
        assertThat(task.lastStatus).isEqualTo(TaskStatus.NONE)
        assertThat(task.running).isEqualTo(false)
        assertThat(task.state).isNull()

        with(ReindexTokenItemRoyaltiesTaskParam.fromParamString(task.param)) {
            assertThat(tokens).isEqualTo(tokens)
        }
    }

    @Test
    fun `should throw exception if reduce token task exist`() = runBlocking<Unit> {
        val targetToken = AddressFactory.create()

        mockTaskRepositoryFindTask(
            ReduceTokenItemsTaskParams.ADMIN_REDUCE_TOKEN_ITEMS,
            ReduceTokenItemsTaskParams(targetToken).toParamString()
        )
        assertThrows<IllegalArgumentException> {
            runBlocking {
                service.createReduceTokenItemsTask(targetToken, false)
            }
        }
    }

    @Test
    fun `should throw exception if reindex royalties token task exist`() = runBlocking<Unit> {
        val targetToken = AddressFactory.create()

        mockTaskRepositoryFindTask(
            ReindexTokenItemRoyaltiesTaskParam.ADMIN_REINDEX_TOKEN_ITEM_ROYALTIES,
            ReindexTokenItemRoyaltiesTaskParam(targetToken).toParamString()
        )
        assertThrows<IllegalArgumentException> {
            runBlocking {
                service.createReindexTokenItemRoyaltiesTask(targetToken, false)
            }
        }
    }

    @Test
    fun `should throw exception if reindex token task exist`() = runBlocking<Unit> {
        val targetToken = AddressFactory.create()
        val existToken = AddressFactory.create()

        mockGetTokenStandard(targetToken, TokenStandard.ERC1155)
        mockGetTokenStandard(existToken, TokenStandard.ERC1155)

        mockTaskRepositoryFindTask(
            ReindexTokenItemsTaskParams.ADMIN_REINDEX_TOKEN_ITEMS,
            ReindexTokenItemsTaskParams(TokenStandard.ERC1155, listOf(existToken)).toParamString()
        )
        assertThrows<IllegalArgumentException> {
            runBlocking {
                service.createReindexTokenItemsTask(listOf(targetToken, existToken), 1, false)
            }
        }
    }

    @Test
    fun `reduce in range - tasks created`() = runBlocking<Unit> {
        val from = Address.ZERO()
        val fromTokenId = randomBigInt()

        val to = Address.TWO()
        val toTokenId = randomBigInt()

        val fromItemId = ItemId(from, EthUInt256(fromTokenId))
        val toItemId = ItemId(to, EthUInt256(toTokenId))
        val midItemId = ItemId(Address.ONE(), EthUInt256(BigInteger.ZERO))

        coEvery { taskRepository.findByType(ReduceTokenRangeTaskParams.ADMIN_REDUCE_TOKEN_RANGE) } returns emptyFlow()

        service.createReduceTokenRangeTask(fromItemId, toItemId, 2)

        coVerify(exactly = 2) { taskRepository.save(any()) }

        coVerify(exactly = 1) {
            taskRepository.save(match {
                val params = ReduceTokenRangeTaskParams.parse(it.param)
                params.from == fromItemId.stringValue && params.to == midItemId.stringValue
            })
        }

        coVerify(exactly = 1) {
            taskRepository.save(match {
                val params = ReduceTokenRangeTaskParams.parse(it.param)
                params.from == midItemId.stringValue && params.to == toItemId.stringValue
            })
        }
    }

    @Test
    fun `reduce in range - already in progress`() = runBlocking<Unit> {
        val fromItemId = ItemId(Address.ZERO(), EthUInt256(randomBigInt()))
        val toItemId = ItemId(Address.TWO(), EthUInt256(randomBigInt()))
        val parent = "${fromItemId.stringValue}..${toItemId.stringValue}"

        val exists = Task(
            type = ReduceTokenRangeTaskParams.ADMIN_REDUCE_TOKEN_RANGE,
            param = ReduceTokenRangeTaskParams(parent, fromItemId.stringValue, toItemId.stringValue).toParamString(),
            state = "",
            running = false,
            lastStatus = TaskStatus.NONE
        )

        coEvery {
            taskRepository.findByType(ReduceTokenRangeTaskParams.ADMIN_REDUCE_TOKEN_RANGE)
        } returns flowOf(exists)

        assertThrows<java.lang.IllegalArgumentException> {
            service.createReduceTokenRangeTask(fromItemId, toItemId, 2)
        }
    }

    @Test
    fun `reduce in range - remove completed on restart`() = runBlocking<Unit> {
        val fromItemId = ItemId(Address.ZERO(), EthUInt256(randomBigInt()))
        val toItemId = ItemId(Address.TWO(), EthUInt256(randomBigInt()))
        val parent = "${fromItemId.stringValue}..${toItemId.stringValue}"

        val exists = Task(
            type = ReduceTokenRangeTaskParams.ADMIN_REDUCE_TOKEN_RANGE,
            param = ReduceTokenRangeTaskParams(parent, fromItemId.stringValue, toItemId.stringValue).toParamString(),
            state = "",
            running = false,
            lastStatus = TaskStatus.COMPLETED
        )

        coEvery {
            taskRepository.findByType(ReduceTokenRangeTaskParams.ADMIN_REDUCE_TOKEN_RANGE)
        } returns flowOf(exists)

        service.createReduceTokenRangeTask(fromItemId, toItemId, 2)

        coVerify(exactly = 2) { taskRepository.save(any()) }
        coVerify(exactly = 1) { taskRepository.delete(exists.id) }
    }

    private fun mockGetTokenStandard(token: Address, standard: TokenStandard) {
        coEvery { tokenService.getTokenStandard(eq(token)) } returns standard
    }

    private fun mockTaskRepositoryFindNothingByType(type: String) {
        coEvery { taskRepository.findByType(eq(type)) } returns flow { }
    }

    private fun mockTaskRepositoryFindTask(
        type: String,
        param: String,
        running: Boolean = true,
        lastStatus: TaskStatus = TaskStatus.NONE
    ) {
        val flow = flow {
            emit(
                Task(
                    type = type,
                    running = running,
                    lastStatus = lastStatus,
                    param = param
                )
            )
        }
        coEvery { taskRepository.findByType(eq(type), eq(param)) } returns flow
        coEvery { taskRepository.findByType(eq(type)) } returns flow
    }
}
