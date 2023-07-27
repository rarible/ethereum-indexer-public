package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.dto.ActivitySortDto
import com.rarible.protocol.dto.SyncSortDto
import com.rarible.protocol.order.core.model.AuctionActivitySort
import org.springframework.core.convert.converter.Converter

object AuctionActivitySortConverter : Converter<ActivitySortDto, AuctionActivitySort> {
    override fun convert(source: ActivitySortDto): AuctionActivitySort {
        return when (source) {
            ActivitySortDto.EARLIEST_FIRST -> AuctionActivitySort.EARLIEST_FIRST
            ActivitySortDto.LATEST_FIRST -> AuctionActivitySort.LATEST_FIRST
        }
    }
}

object AuctionActivitySyncSortConverter : Converter<SyncSortDto, AuctionActivitySort> {
    override fun convert(source: SyncSortDto): AuctionActivitySort {
        return when (source) {
            SyncSortDto.DB_UPDATE_ASC -> AuctionActivitySort.SYNC_EARLIEST_FIRST
            SyncSortDto.DB_UPDATE_DESC -> AuctionActivitySort.SYNC_LATEST_FIRST
        }
    }
}
