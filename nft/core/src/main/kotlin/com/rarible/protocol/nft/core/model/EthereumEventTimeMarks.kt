package com.rarible.protocol.nft.core.model

import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import com.rarible.protocol.dto.EventTimeMarkDto
import com.rarible.protocol.dto.EventTimeMarksDto
import java.time.Instant

data class EthereumEventTimeMarks(
    val source: String, val marks: List<EthereumSourceEventTimeMark> = emptyList()
) {
    fun add(name: String, date: Instant? = null): EthereumEventTimeMarks {
        val mark = EthereumSourceEventTimeMark(name, date ?: Instant.now())
        val marks = this.marks + mark
        return this.copy(marks = marks)
    }

    fun addOut(suffix: String) = add("indexer-out_$suffix")
    fun toDto() = EventTimeMarksDto(source, marks.map { EventTimeMarkDto(it.name, it.date) })
}

data class EthereumSourceEventTimeMark(
    val name: String,
    val date: Instant = Instant.now(),
)

fun indexerInNftBlockchainTimeMark() = EthereumEventTimeMarks(
    source = "blockchain",
    marks = listOf(
        EthereumSourceEventTimeMark(name = "indexer-in_nft")
    )
)

fun indexerInNftBlockchainTimeMark(log: EthereumLog) = EthereumEventTimeMarks(
    source = "blockchain",
    marks = listOfNotNull(
        log.blockTimestamp?.let {
            EthereumSourceEventTimeMark(
                name = "source",
                date = Instant.ofEpochSecond(it)
            )
        },
        EthereumSourceEventTimeMark(name = "indexer-in_nft"),
    )
)