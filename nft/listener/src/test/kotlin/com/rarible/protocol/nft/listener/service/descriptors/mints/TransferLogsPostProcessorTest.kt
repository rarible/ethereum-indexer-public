package com.rarible.protocol.nft.listener.service.descriptors.mints

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.FullBlock
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.randomEthereumLogEvent
import com.rarible.protocol.nft.core.data.randomReversedLogRecord
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.repository.data.createItemHistory
import io.daonomic.rpc.domain.Word
import io.mockk.every
import io.mockk.mockk
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import scalether.domain.Address
import scalether.domain.AddressFactory
import scalether.domain.response.Transaction
import java.math.BigInteger
import java.util.stream.Stream

class TransferLogsPostProcessorTest {

    private val postProcessor = TransferLogsPostProcessor()

    companion object {
        @JvmStatic
        fun data() = Stream.of(
            // simple mint
            Arguments.of(
                listOf(txWithValue(BigInteger.ONE), txWithValue(BigInteger.ZERO)),
                listOf(mint(EthUInt256.ONE), mint(EthUInt256.ONE)),
                listOf(BigInteger.ONE, null)
            ),
            // multiple mint
            Arguments.of(
                listOf(txWithValue(20.toBigInteger()), txWithValue(BigInteger.ZERO)),
                listOf(mint(EthUInt256.TEN), mint(EthUInt256.ONE)),
                listOf(2.toBigInteger(), null)
            ),
            // round case
            Arguments.of(
                listOf(txWithValue(4.toBigInteger()), txWithValue(BigInteger.ZERO)),
                listOf(mint(EthUInt256.of(3)), mint(EthUInt256.ONE)),
                listOf(1.toBigInteger(), null)
            ),
            // tx value = 0
            Arguments.of(
                listOf(txWithValue(BigInteger.ZERO)),
                listOf(mint(EthUInt256.ONE)),
                listOf(null)
            ),
            // mint = 0
            Arguments.of(
                listOf(txWithValue(BigInteger.ONE)),
                listOf(createItemHistory().copy(from = AddressFactory.create())),
                listOf(null)
            )
        )

        fun txWithValue(value: BigInteger) : Transaction {
            val transaction = mockk<Transaction>()
            every { transaction.value() } returns value
            every { transaction.hash() } returns Word(RandomUtils.nextBytes(32))
            return transaction
        }

        private fun mint(value: EthUInt256) = createItemHistory().copy(from = Address.ZERO(), mintPrice = null, value = value)
    }

    @ParameterizedTest
    @MethodSource("data")
    fun test(
        transactions: List<Transaction>,
        transfers: List<ItemTransfer>,
        prices: List<BigInteger>
    ) {
        val block = FullBlock<EthereumBlockchainBlock, EthereumBlockchainLog>(
            block = mockk(),
            logs = transactions.map {
                val log = mockk<EthereumBlockchainLog>()
                every { log.ethTransaction } returns it
                log
            }
        )

        val records = transfers.zip(transactions).map { (transfer, tx) ->
            randomReversedLogRecord(transfer, randomEthereumLogEvent().copy(transactionHash = tx.hash().toString()))
        }

        val events = postProcessor.process(block, records)

        events.zip(prices).forEach { (event, price) ->
            val log = event as ReversedEthereumLogRecord
            val data = log.data as ItemTransfer
            assertThat(data.mintPrice).isEqualTo(price)
        }
    }

}
