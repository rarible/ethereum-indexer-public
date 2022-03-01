package com.rarible.protocol.nft.core.converters.dto

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.dto.BurnDto
import com.rarible.protocol.dto.MintDto
import com.rarible.protocol.dto.NftActivityDto
import com.rarible.protocol.dto.TransferDto
import com.rarible.protocol.nft.core.model.ItemTransfer
import io.daonomic.rpc.domain.Word
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
object NftActivityConverter: Converter<LogEvent, NftActivityDto> {
    fun convert(logEvent: ReversedEthereumLogRecord): NftActivityDto? {
        val transactionHash = logEvent.transactionHash
        val blockHash = logEvent.blockHash ?: DEFAULT_BLOCK_HASH
        val blockNumber = logEvent.blockNumber ?: DEFAULT_BLOCK_NUMBER
        val logIndex = logEvent.logIndex ?: DEFAULT_LOG_INDEX

        return when(val eventData = logEvent.data) {
            is ItemTransfer -> {
                toDto(eventData, logEvent.id, Word.apply(transactionHash), blockHash, blockNumber, logIndex)
            }
            else -> null
        }
    }

    override fun convert(logEvent: LogEvent): NftActivityDto? {
        val transactionHash = logEvent.transactionHash
        val blockHash = logEvent.blockHash ?: DEFAULT_BLOCK_HASH
        val blockNumber = logEvent.blockNumber ?: DEFAULT_BLOCK_NUMBER
        val logIndex = logEvent.logIndex ?: DEFAULT_LOG_INDEX

            return when(val eventData = logEvent.data) {
            is ItemTransfer -> {
                toDto(eventData, logEvent.id.toString(), transactionHash, blockHash, blockNumber, logIndex)
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
        logIndex: Int
    ): NftActivityDto {
        return when {
            itemTransfer.from == Address.ZERO() -> {
                MintDto(
                    id = id,
                    owner = itemTransfer.owner,
                    contract = itemTransfer.token,
                    tokenId = itemTransfer.tokenId.value,
                    value = itemTransfer.value.value,
                    date = itemTransfer.date,
                    transactionHash = transactionHash,
                    blockHash = blockHash,
                    blockNumber = blockNumber,
                    logIndex = logIndex,
                    reverted = false
                )
            }
            itemTransfer.owner == Address.ZERO() -> {
                BurnDto(
                    id = id.toString(),
                    owner = itemTransfer.from,
                    contract = itemTransfer.token,
                    tokenId = itemTransfer.tokenId.value,
                    value = itemTransfer.value.value,
                    date = itemTransfer.date,
                    transactionHash = transactionHash,
                    blockHash = blockHash,
                    blockNumber = blockNumber,
                    logIndex = logIndex,
                    reverted = false
                )
            }
            else -> {
                TransferDto(
                    id = id.toString(),
                    owner = itemTransfer.owner,
                    contract = itemTransfer.token,
                    tokenId = itemTransfer.tokenId.value,
                    value = itemTransfer.value.value,
                    date = itemTransfer.date,
                    from = itemTransfer.from,
                    transactionHash = transactionHash,
                    blockHash = blockHash,
                    blockNumber = blockNumber,
                    logIndex = logIndex,
                    reverted = false
                )
            }
        }
    }

    private val DEFAULT_BLOCK_HASH: Word = Word.apply(ByteArray(32))
    private const val DEFAULT_BLOCK_NUMBER: Long = 0
    private const val DEFAULT_LOG_INDEX: Int = 0
}
