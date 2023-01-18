package com.rarible.protocol.nft.listener.service.descriptors.erc721

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.blockchain.scanner.ethereum.model.EthereumLogRecord
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.FullBlock
import com.rarible.protocol.nft.core.data.randomEthereumLogEvent
import com.rarible.protocol.nft.core.data.randomReversedLogRecord
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.repository.data.createItemHistory
import io.daonomic.rpc.domain.Word
import io.mockk.every
import io.mockk.mockk
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.response.Transaction
import java.math.BigInteger
import scalether.domain.Address

class TransferLogsPostProcessorTest {

    private val postProcessor = TransferLogsPostProcessor()

    @Test
    fun `should calculate mintPrice`() {
        val tx1 = txWithValue(BigInteger.ONE)
        val tx2 = txWithValue(BigInteger.ZERO)
        val transactions = listOf(tx1, tx2)

        val transfer1 = createItemHistory().copy(from = Address.ZERO(), mintPrice = null)
        val transfer2 = createItemHistory().copy(from = Address.ZERO(), mintPrice = null)

        val record1 = randomReversedLogRecord(transfer1, randomEthereumLogEvent().copy(transactionHash = tx1.hash().toString()))
        val record2 = randomReversedLogRecord(transfer2, randomEthereumLogEvent().copy(transactionHash = tx2.hash().toString()))
        val records = listOf(record1, record2)

        val block = FullBlock<EthereumBlockchainBlock, EthereumBlockchainLog>(
            block = mockk(),
            logs = transactions.map {
                val log = mockk<EthereumBlockchainLog>()
                every { log.ethTransaction } returns it
                log
            }
        )

        val events = postProcessor.process(block, records)
        checkMintPrice(events[0], BigInteger.ONE)
        checkMintPrice(events[1], null)
    }

    private fun checkMintPrice(record: EthereumLogRecord, price: BigInteger?) {
        val log = record as ReversedEthereumLogRecord
        val data = log.data as ItemTransfer
        assertThat(data.mintPrice).isEqualTo(price)
    }

    private fun txWithValue(value: BigInteger) : Transaction {
        val transaction = mockk<Transaction>()
        every { transaction.value() } returns value
        every { transaction.hash() } returns Word(RandomUtils.nextBytes(32))
        return transaction
    }

}
