package com.rarible.protocol.order.core.model

import com.rarible.core.reduce.model.ReduceSnapshot
import io.daonomic.rpc.domain.Word
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("auction_snapshot")
data class AuctionReduceSnapshot(
    @Id
    override val id: Word,
    override val data: Auction,
    override val mark: Long
) : ReduceSnapshot<Auction, Long, Word>()
