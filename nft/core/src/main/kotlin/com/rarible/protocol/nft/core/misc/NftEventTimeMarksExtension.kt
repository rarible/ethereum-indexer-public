package com.rarible.protocol.nft.core.misc

import com.rarible.core.common.EventTimeMarks
import com.rarible.protocol.dto.EventTimeMarkDto
import com.rarible.protocol.dto.EventTimeMarksDto
import java.time.Instant

private const val stage = "indexer"
private const val postfix = "nft"

fun EventTimeMarks.addIndexerIn(date: Instant? = null) = this.addIn(stage, postfix, date)
fun EventTimeMarks.addIndexerOut(date: Instant? = null) = this.addOut(stage, postfix, date)

fun EventTimeMarks.toDto(): EventTimeMarksDto {
    return EventTimeMarksDto(this.source, this.marks.map { EventTimeMarkDto(it.name, it.date) })
}

fun nftOffchainEventMarks() = EventTimeMarks("offchain").add("source").addIndexerIn()
fun nftTaskEventMarks() = EventTimeMarks("task").add("source").addIndexerIn()

// Just for tests
fun nftStubEventMarks() = EventTimeMarks("stub").add("source").addIndexerIn()



