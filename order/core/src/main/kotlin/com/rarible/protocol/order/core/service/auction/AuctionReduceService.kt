package com.rarible.protocol.order.core.service.auction

import com.rarible.core.reduce.service.ReduceService
import com.rarible.protocol.order.core.model.Auction
import com.rarible.protocol.order.core.model.AuctionReduceEvent
import com.rarible.protocol.order.core.model.AuctionReduceSnapshot
import io.daonomic.rpc.domain.Word

typealias AuctionReduceService = ReduceService<AuctionReduceEvent, AuctionReduceSnapshot, Long, Auction, Word>
