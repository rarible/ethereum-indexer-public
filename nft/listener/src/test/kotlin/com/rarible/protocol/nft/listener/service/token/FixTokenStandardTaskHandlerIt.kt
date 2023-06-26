package com.rarible.protocol.nft.listener.service.token

import com.rarible.core.task.Task
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.contracts.erc721.rarible.ERC721Rarible
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.listener.admin.FixTokenStandardTaskHandler
import com.rarible.protocol.nft.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.test.IntegrationTest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.web3jold.utils.Numeric
import reactor.core.publisher.Mono
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger
import java.time.Duration
import java.time.Instant.now

// This test could be flaky because of listening registration of token
@Disabled
@IntegrationTest
class FixTokenStandardTaskHandlerIt : AbstractIntegrationTest() {

    @Test
    fun `find tokens with NONE standard`() = runBlocking<Unit> {
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
            assertThat(token).isNotNull
            // set NONE standard
            token?.let {
                tokenRepository.save(
                    it.copy(
                        standard = TokenStandard.NONE,
                        dbUpdatedAt = now().minusSeconds(60),
                        features = emptySet()
                    )
                ).awaitSingle()
            }
        }

        Wait.waitAssert(timeout = Duration.ofDays(1)) {
            assertThat(tokenRepository.findById(erc721.address()).awaitFirst().standard).isEqualTo(TokenStandard.NONE)
        }

        // run job to fix
        Wait.waitAssert(timeout = Duration.ofDays(1)) {
            val findNonParseableLogEntriesTaskHandler = FixTokenStandardTaskHandler(
                mongo,
                tokenProvider,
                nftHistoryRepository,
                reindexTokenService,
                tokenService,
                tokenReduceService
            )
            val processed = findNonParseableLogEntriesTaskHandler.runLongTask(null, "false").firstOrNull()
            assertThat(processed).isNotNull()
        }

        // check set standard
        Wait.waitAssert(timeout = Duration.ofDays(1)) {
            assertThat(tokenRepository.findById(erc721.address()).awaitFirst().standard).isEqualTo(TokenStandard.ERC721)
        }

        // check created tasks
        Wait.waitAssert(timeout = Duration.ofDays(1)) {
            assertThat(findTask("ADMIN_REINDEX_TOKEN_ITEMS").param).isEqualTo("ERC721:${erc721.address().prefixed()}")
            assertThat(findTask("ADMIN_REDUCE_TOKEN_ITEMS").param).isEqualTo(erc721.address().prefixed())
        }
    }

    private suspend fun findTask(name: String): Task {
        val criteria = (Task::type isEqualTo name)
        return mongo.find<Task>(Query.query(criteria), "task").awaitSingle()
    }
}
