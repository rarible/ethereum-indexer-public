package com.rarible.protocol.dto.mapper

import com.rarible.protocol.dto.ActivityContinuationDto
import com.rarible.protocol.dto.ActivityDto
import org.apache.commons.lang3.StringUtils
import java.time.Instant

class ContinuationMapper {

    companion object {
        private const val SEPARATOR = "_"

        fun toActivityContinuationDto(str: String?): ActivityContinuationDto? {
            val dateStr = StringUtils.substringBefore(str, SEPARATOR)
            val idStr = StringUtils.substringAfter(str, SEPARATOR)
            return if (dateStr != null && idStr != null) {
                ActivityContinuationDto(Instant.ofEpochMilli(dateStr.toLong()), idStr)
            } else {
                null
            }
        }

        fun toString(dto: ActivityContinuationDto?): String? {
            if (dto == null) {
                return null
            }
            return toString(dto.afterDate.toEpochMilli(), dto.afterId)
        }

        fun toString(dto: ActivityDto?): String? {
            if (dto == null) {
                return null
            }
            return toString(dto.date.toEpochMilli(), dto.id)
        }

        fun toString(time: Long, id: String): String {
            return "${time}_${id}"
        }
    }

}
