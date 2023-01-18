package com.rarible.protocol.nft.listener.service.descriptors.erc721

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.blockchain.scanner.ethereum.model.EthereumLogRecord
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.FullBlock
import com.rarible.protocol.nft.core.model.ItemTransfer
import java.math.BigInteger

class TransferLogsPostProcessor {

    fun process(
        block: FullBlock<EthereumBlockchainBlock, EthereumBlockchainLog>,
        logs: List<EthereumLogRecord>
    ): List<EthereumLogRecord> {

        val ethereumLogs = logs as List<ReversedEthereumLogRecord>

        // tx hash -> value of transaction
        val valueByHash: Map<String, BigInteger> = block.logs.map { it.ethTransaction }
            .filter { it.value() > BigInteger.ZERO }
            .associateBy({ tx -> tx.hash().toString() }, { tx -> tx.value() })

        // tx hash -> mintPrice
        val priceByHash: Map<String, BigInteger?> = ethereumLogs
            .filter { it.transactionHash in valueByHash.keys }
            .filter { (it.data as ItemTransfer).isMintTransfer() }
            .groupingBy { it.transactionHash }
            .eachCount()

            // tx hash -> mint count
            .entries.associate { (hash, count) ->
                val value = valueByHash[hash]
                hash to value?.divide(count.toBigInteger())
            }

        return ethereumLogs
            .map {
                if (it.transactionHash in priceByHash.keys) {
                    val transfer = it.data as ItemTransfer
                    val mintPrice = priceByHash[it.transactionHash]
                    it.copy(data = transfer.copy(mintPrice = mintPrice))
                } else {
                    it
                }
            }
    }
}
