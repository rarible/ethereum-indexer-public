package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.dto.ActivitySortDto
import com.rarible.protocol.dto.SyncSortDto
import com.rarible.protocol.order.core.model.ActivitySort
import org.springframework.core.convert.converter.Converter

object ActivitySortConverter : Converter<ActivitySortDto, ActivitySort> {
    override fun convert(source: ActivitySortDto): ActivitySort {
        return when (source) {
            ActivitySortDto.EARLIEST_FIRST -> ActivitySort.EARLIEST_FIRST
            ActivitySortDto.LATEST_FIRST -> ActivitySort.LATEST_FIRST
        }
    }
}

object ActivitySyncSortConverter : Converter<SyncSortDto, ActivitySort> {
    override fun convert(source: SyncSortDto): ActivitySort {
        return when (source) {
            SyncSortDto.DB_UPDATE_ASC -> ActivitySort.SYNC_EARLIEST_FIRST
            SyncSortDto.DB_UPDATE_DESC -> ActivitySort.SYNC_LATEST_FIRST
        }
    }
}
