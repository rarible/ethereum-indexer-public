package com.rarible.protocol.nft.core.misc

import com.rarible.core.common.EventTimeMarks
import com.rarible.protocol.dto.EventTimeMarkDto
import com.rarible.protocol.dto.EventTimeMarksDto
import java.time.Instant

private const val stage = "indexer"
private const val postfix = "nft"

fun EventTimeMarks.addIn(date: Instant? = null) = this.addIn(stage, postfix, date)
fun EventTimeMarks.addOut(date: Instant? = null) = this.addOut(stage, postfix, date)

fun EventTimeMarks.toDto(): EventTimeMarksDto {
    return EventTimeMarksDto(this.source, this.marks.map { EventTimeMarkDto(it.name, it.date) })
}

fun nftOffchainEventMarks() = EventTimeMarks("offchain").add("source").addIn()


