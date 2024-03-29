package com.rarible.protocol.nft.listener.service.descriptors.mints

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.blockchain.scanner.ethereum.model.EthereumLogRecord
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.FullBlock
import com.rarible.core.common.optimisticLock
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.service.token.TokenService
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
class TransferLogsPostProcessor(
    private val tokenService: TokenService,
    private val featureFlags: FeatureFlags,
) {
    suspend fun process(
        block: FullBlock<EthereumBlockchainBlock, EthereumBlockchainLog>,
        logs: List<EthereumLogRecord>
    ): List<EthereumLogRecord> {
        val ethereumLogs = logs as List<ReversedEthereumLogRecord>
        if (featureFlags.detectScamToken) {
            detectScam(ethereumLogs)
        }
        return withMintPrices(block, ethereumLogs)
    }

    private suspend fun detectScam(logs: List<ReversedEthereumLogRecord>) {
        val scamTokens = logs.filter { it.data is ItemTransfer }
            .groupBy { Pair(it.transactionHash, (it.data as ItemTransfer).token) }
            .filterValues { events ->
                events.map { (it.data as ItemTransfer).owner }.toSet().size >= featureFlags.detectScamTokenThreshold
            }
            .keys
            .map { it.second }
        if (scamTokens.isNotEmpty()) {
            scamTokens.chunked(10).flatMap { tokenAddresses ->
                optimisticLock {
                    tokenService.getTokens(tokenAddresses)
                        .filterNot { token -> token.scam }
                        .map { token ->
                            tokenService.saveToken(token.copy(scam = true))
                        }
                }
            }
        }
    }

    private fun withMintPrices(
        block: FullBlock<EthereumBlockchainBlock, EthereumBlockchainLog>,
        logs: List<ReversedEthereumLogRecord>
    ): List<ReversedEthereumLogRecord> {
        // tx hash -> value of transaction
        val valueByHash: Map<String, BigInteger> = block.logs.map { it.ethTransaction }
            .filter { it.value() > BigInteger.ZERO }
            .associateBy({ tx -> tx.hash().toString() }, { tx -> tx.value() })

        val priceByHash: Map<String, BigInteger?> = logs
            .filter { it.transactionHash in valueByHash.keys }
            .filter { it.data is ItemTransfer } // Actually data is always ItemTransfer
            .filter { (it.data as ItemTransfer).isMintTransfer() }
            // tx hash -> mint counts
            .groupBy({ it.transactionHash }, { (it.data as ItemTransfer).value.value })
            // tx hash -> mintPrice
            .entries.associate { (hash, counts) ->
                val value = valueByHash[hash]
                val count = counts.reduce { acc, bigInteger -> acc + bigInteger }
                if (count > BigInteger.ZERO) {
                    hash to value?.divide(count)
                } else {
                    hash to null // this could be happened only in testnet
                }
            }

        return logs
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
