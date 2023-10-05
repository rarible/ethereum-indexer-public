package com.rarible.protocol.nft.listener.service.descriptors.mints

import com.rarible.contracts.test.erc1155.TestERC1155
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.nft.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.test.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.web3jold.crypto.Keys
import org.web3jold.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger

@IntegrationTest
class TransferLogsPostProcessorIt : AbstractIntegrationTest() {
    @Test
    fun `detect scam token`(): Unit = runBlocking {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        Address.apply(Keys.getAddressFromPrivateKey(privateKey))

        val userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000)
        ) { Mono.just(BigInteger.ZERO) }

        val contract = TestERC1155.deployAndWait(userSender, poller).awaitFirst()
        val minter = userSender.from()

        // normal transfers
        contract.mint(minter, BigInteger.ONE, BigInteger("10")).execute().verifySuccess()
        batchTransferRandom(10, minter, contract)
        Wait.waitAssert {
            val token = tokenService.getToken(contract.address())
            assertThat(token).isNotNull
            assertThat(token!!.scam).isFalse()
        }

        // scam transfers
        contract.mint(minter, BigInteger.ONE, BigInteger("100")).execute().verifySuccess()
        batchTransferRandom(100, minter, contract)
        Wait.waitAssert {
            val token = tokenService.getToken(contract.address())
            assertThat(token).isNotNull
            assertThat(token!!.scam).isTrue()
        }
    }

    private suspend fun batchTransferRandom(
        recipientsAmount: Int,
        minter: Address,
        contract: TestERC1155
    ) {
        val froms = (1..recipientsAmount).map { minter }.toTypedArray()
        val tos = (1..recipientsAmount).map { randomAddress() }.toTypedArray()
        val ids = (1..recipientsAmount).map { BigInteger.ONE }.toTypedArray()
        val amounts = (1..recipientsAmount).map { BigInteger.ONE }.toTypedArray()

        contract.batchSafeTransferFrom(froms, tos, ids, amounts).execute().verifySuccess()
    }
}
