package com.rarible.protocol.nft.core.converters.dto

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.dto.BurnDto
import com.rarible.protocol.dto.MintDto
import com.rarible.protocol.dto.TransferDto
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.data.randomReversedLogRecord
import com.rarible.protocol.nft.core.repository.data.createItemHistory
import io.daonomic.rpc.domain.Word
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address

class NftActivityConverterTest {

    private val marketContract = randomAddress()
    private val converter = NftActivityConverter(
        NftIndexerProperties.ContractAddresses(
            marketContract.prefixed()
        )
    )

    @Test
    fun `convert - transfer, not reverted`() {
        val transfer = createItemHistory()
        val record = randomReversedLogRecord(transfer)

        val converted = converter.convert(record, false) as TransferDto

        assertThat(converted.owner).isEqualTo(transfer.owner)
        assertThat(converted.tokenId).isEqualTo(transfer.tokenId.value)
        assertThat(converted.value).isEqualTo(transfer.value.value)
        assertThat(converted.date).isEqualTo(transfer.date)
        assertThat(converted.contract).isEqualTo(transfer.token)
        assertThat(converted.from).isEqualTo(transfer.from)
        assertThat(converted.transactionHash.prefixed()).isEqualTo(record.transactionHash)
        assertThat(converted.blockHash).isEqualTo(record.blockHash)
        assertThat(converted.blockNumber).isEqualTo(record.blockNumber)
        assertThat(converted.logIndex).isEqualTo(record.logIndex)

        assertThat(converted.reverted).isFalse()
        assertThat(converted.purchase).isFalse()
    }

    @Test
    fun `convert - transfer with default block values`() {
        val transfer = createItemHistory()
        val record = randomReversedLogRecord(transfer).copy(
            blockHash = null,
            blockNumber = null,
            logIndex = null
        )

        val converted = converter.convert(record, true) as TransferDto

        assertThat(converted.blockHash).isEqualTo(Word.apply(ByteArray(32))) // zero word
        assertThat(converted.blockNumber).isEqualTo(0)
        assertThat(converted.logIndex).isEqualTo(0)
    }

    @Test
    fun `convert - purchase, reverted`() {
        val transfer = createItemHistory()
        val record = randomReversedLogRecord(transfer).copy(to = marketContract)

        val converted = converter.convert(record, true) as TransferDto

        assertThat(converted.reverted).isTrue()
        assertThat(converted.purchase).isTrue()
    }

    @Test
    fun `convert - burn, reverted`() {
        val transfer = createItemHistory().copy(owner = Address.ZERO())
        val record = randomReversedLogRecord(transfer)

        val converted = converter.convert(record, true) as BurnDto

        assertThat(converted.owner).isEqualTo(transfer.from)
        assertThat(converted.tokenId).isEqualTo(transfer.tokenId.value)
        assertThat(converted.value).isEqualTo(transfer.value.value)
        assertThat(converted.date).isEqualTo(transfer.date)
        assertThat(converted.contract).isEqualTo(transfer.token)
        assertThat(converted.transactionHash.prefixed()).isEqualTo(record.transactionHash)
        assertThat(converted.blockHash).isEqualTo(record.blockHash)
        assertThat(converted.blockNumber).isEqualTo(record.blockNumber)
        assertThat(converted.logIndex).isEqualTo(record.logIndex)

        assertThat(converted.reverted).isTrue()
    }

    @Test
    fun `convert - mint, not reverted`() {
        val transfer = createItemHistory().copy(from = Address.ZERO())
        val record = randomReversedLogRecord(transfer)

        val converted = converter.convert(record, false) as MintDto

        assertThat(converted.owner).isEqualTo(transfer.owner)
        assertThat(converted.tokenId).isEqualTo(transfer.tokenId.value)
        assertThat(converted.value).isEqualTo(transfer.value.value)
        assertThat(converted.date).isEqualTo(transfer.date)
        assertThat(converted.contract).isEqualTo(transfer.token)
        assertThat(converted.transactionHash.prefixed()).isEqualTo(record.transactionHash)
        assertThat(converted.blockHash).isEqualTo(record.blockHash)
        assertThat(converted.blockNumber).isEqualTo(record.blockNumber)
        assertThat(converted.logIndex).isEqualTo(record.logIndex)

        assertThat(converted.reverted).isFalse()
    }
}