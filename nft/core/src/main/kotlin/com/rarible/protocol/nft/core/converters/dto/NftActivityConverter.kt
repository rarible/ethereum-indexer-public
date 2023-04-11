package com.rarible.protocol.nft.core.converters.dto

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.dto.BurnDto
import com.rarible.protocol.dto.MintDto
import com.rarible.protocol.dto.NftActivityDto
import com.rarible.protocol.dto.TransferDto
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemTransfer
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigInteger

@Component
class NftActivityConverter(
    addresses: NftIndexerProperties.ContractAddresses
) {

    private val marketAddresses = addresses.marketAddresses

    fun convert(logEvent: ReversedEthereumLogRecord, reverted: Boolean): NftActivityDto? {
        val transactionHash = logEvent.transactionHash
        val blockHash = logEvent.blockHash ?: DEFAULT_BLOCK_HASH
        val blockNumber = logEvent.blockNumber ?: DEFAULT_BLOCK_NUMBER
        val logIndex = logEvent.logIndex ?: DEFAULT_LOG_INDEX

        return when (val eventData = logEvent.data) {
            is ItemTransfer -> {
                toDto(
                    eventData,
                    logEvent.id,
                    Word.apply(transactionHash),
                    blockHash,
                    blockNumber,
                    logIndex,
                    reverted,
                    logEvent.to,
                    logEvent.updatedAt
                )
            }
            else -> null
        }
    }

    fun convert(logEvent: LogEvent, reverted: Boolean = false): NftActivityDto? {
        val transactionHash = logEvent.transactionHash
        val blockHash = logEvent.blockHash ?: DEFAULT_BLOCK_HASH
        val blockNumber = logEvent.blockNumber ?: DEFAULT_BLOCK_NUMBER
        val logIndex = logEvent.logIndex ?: DEFAULT_LOG_INDEX

        return when (val eventData = logEvent.data) {
            is ItemTransfer -> {
                toDto(
                    eventData,
                    logEvent.id.toString(),
                    transactionHash,
                    blockHash,
                    blockNumber,
                    logIndex,
                    reverted,
                    logEvent.to,
                    logEvent.updatedAt
                )
            }
            else -> null
        }
    }

    private fun toDto(
        itemTransfer: ItemTransfer,
        id: String,
        transactionHash: Word,
        blockHash: Word,
        blockNumber: Long,
        logIndex: Int,
        reverted: Boolean,
        to: Address? = null,
        updatedAt: java.time.Instant
    ): NftActivityDto {
        return when {
            itemTransfer.isMintTransfer() -> {
                MintDto(
                    id = id,
                    owner = itemTransfer.owner,
                    contract = itemTransfer.token,
                    tokenId = itemTransfer.tokenId.value,
                    value = itemTransfer.value.value,
                    mintPrice = itemTransfer.mintPrice?.let(::normalize),
                    date = itemTransfer.date,
                    transactionHash = transactionHash,
                    blockHash = blockHash,
                    blockNumber = blockNumber,
                    logIndex = logIndex,
                    reverted = reverted,
                    lastUpdatedAt = updatedAt
                )
            }

            itemTransfer.isBurnTransfer() -> {
                BurnDto(
                    id = id,
                    owner = itemTransfer.from,
                    contract = itemTransfer.token,
                    tokenId = itemTransfer.tokenId.value,
                    value = itemTransfer.value.value,
                    date = itemTransfer.date,
                    transactionHash = transactionHash,
                    blockHash = blockHash,
                    blockNumber = blockNumber,
                    logIndex = logIndex,
                    reverted = reverted,
                    lastUpdatedAt = updatedAt
                )
            }
            else -> {
                TransferDto(
                    id = id,
                    owner = itemTransfer.owner,
                    contract = itemTransfer.token,
                    tokenId = itemTransfer.tokenId.value,
                    value = itemTransfer.value.value,
                    date = itemTransfer.date,
                    purchase = to?.let { marketAddresses.contains(to) },
                    from = itemTransfer.from,
                    transactionHash = transactionHash,
                    blockHash = blockHash,
                    blockNumber = blockNumber,
                    logIndex = logIndex,
                    reverted = reverted,
                    lastUpdatedAt = updatedAt
                )
            }
        }
    }

    private fun normalize(value: BigInteger) = value.toBigDecimal(18)

    private val DEFAULT_BLOCK_HASH: Word = Word.apply(ByteArray(32))
    private val DEFAULT_BLOCK_NUMBER: Long = 0
    private val DEFAULT_LOG_INDEX: Int = 0
}
