package com.rarible.protocol.nft.listener.job

import com.mongodb.assertions.Assertions.assertFalse
import com.rarible.core.task.Task
import com.rarible.core.task.TaskStatus
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.contracts.erc721.rarible.ERC721Rarible
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.listener.admin.ReduceTokenItemsDependentTaskHandler
import com.rarible.protocol.nft.listener.configuration.FixStandardJobProperties
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import com.rarible.protocol.nft.listener.metrics.NftListenerMetricsFactory
import com.rarible.protocol.nft.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.test.IntegrationTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.web3jold.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.AddressFactory
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger
import java.time.Instant

@IntegrationTest
internal class FixStandardJobIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var metricsFactory: NftListenerMetricsFactory

    @Autowired
    private lateinit var reduceTokenItemsDependentTaskHandler: ReduceTokenItemsDependentTaskHandler

    private lateinit var job: FixStandardJob

    @BeforeEach
    fun setUpJob() {
        val props: NftListenerProperties = mockk() {
            every { fixStandardJob } returns FixStandardJobProperties(
                enabled = true,
                batchSize = 10,
                retries = 5
            )
        }
        job = FixStandardJob(
            listenerProps = props,
            tokenRepository = tokenRepository,
            tokenService = tokenService,
            reindexTokenService = reindexTokenService,
            metricsFactory = metricsFactory
        )
    }

    @Test
    fun `fix missed standard - ok`() = runBlocking<Unit> {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))

        val userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000)
        ) { Mono.just(BigInteger.ZERO) }

        val erc721 = ERC721Rarible.deployAndWait(userSender, poller).awaitFirst()
        erc721.__ERC721Rarible_init("Test", "TestSymbol", "BASE", "URI").execute().verifySuccess()

        Wait.waitAssert {
            val token = tokenRepository.findById(erc721.address()).awaitFirstOrNull()
            println("VERSION: " + token?.version)
            assertThat(token).isNotNull
            assertThat(token?.version).isGreaterThanOrEqualTo(1)
        }

        // set NONE standard
        val token = tokenRepository.findById(erc721.address()).awaitFirst()
        tokenRepository.save(
            token.copy(
                standard = TokenStandard.NONE,
                standardRetries = null,
                dbUpdatedAt = Instant.now().minusSeconds(60),
                features = emptySet()
            )
        ).awaitSingle()

        // fire fixing job
        job.execute()

        // check standard
        Wait.waitAssert {
            val updated = tokenRepository.findById(erc721.address()).awaitFirst()
            assertThat(updated.standard).isEqualTo(TokenStandard.ERC721)
            assertThat(updated.standardRetries).isEqualTo(1)
        }

        // check created tasks
        Wait.waitAssert {
            assertThat(findTask("BLOCK_SCANNER_REINDEX_TASK").param).isNotNull()
            val param = findTask("REDUCE_TOKEN_ITEMS_DEPENDENT").param
            assertThat(reduceTokenItemsDependentTaskHandler.isAbleToRun(param)).isFalse()
        }
        finishIndexing()

        // check that reduce can start
        Wait.waitAssert {
            val param = findTask("REDUCE_TOKEN_ITEMS_DEPENDENT").param
            assertThat(reduceTokenItemsDependentTaskHandler.isAbleToRun(param)).isTrue()
        }
    }

    @Test
    fun `increment retry - ok`() = runBlocking<Unit> {
        val tokenAddress = AddressFactory.create()

        tokenRepository.save(
            Token(
                id = tokenAddress,
                name = tokenAddress.toString(),
                standard = TokenStandard.NONE,
                standardRetries = null
            )
        ).awaitSingle()

        job.execute()

        Wait.waitAssert {
            val token = tokenRepository.findById(tokenAddress).awaitFirst()
            assertThat(token.standardRetries).isEqualTo(1)
            assertFalse(existTask("BLOCK_SCANNER_REINDEX_TASK"))
        }
    }

    @Test
    fun `do not start after retry - ok`() = runBlocking<Unit> {
        val tokenAddress = AddressFactory.create()

        tokenRepository.save(
            Token(
                id = tokenAddress,
                name = tokenAddress.toString(),
                standard = TokenStandard.NONE,
                standardRetries = 5
            )
        ).awaitSingle()

        job.execute()

        Wait.waitAssert {
            assertFalse(existTask("BLOCK_SCANNER_REINDEX_TASK"))
        }
    }

    private suspend fun findTask(name: String): Task {
        val criteria = (Task::type isEqualTo name)
        return mongo.find<Task>(Query.query(criteria), "task").awaitSingle()
    }

    private suspend fun existTask(name: String): Boolean {
        val criteria = (Task::type isEqualTo name)
        return mongo.find<Task>(Query.query(criteria), "task").awaitFirstOrNull() != null
    }

    private suspend fun finishIndexing() {
        mongo.findAndModify(
            Query(Task::type isEqualTo "BLOCK_SCANNER_REINDEX_TASK"),
            Update().set(Task::lastStatus.name, TaskStatus.COMPLETED.name),
            Task::class.java
        ).awaitLast()
    }
}
