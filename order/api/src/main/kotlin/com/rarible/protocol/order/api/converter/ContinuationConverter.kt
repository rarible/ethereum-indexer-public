package com.rarible.protocol.order.api.converter

import com.rarible.protocol.dto.ActivityContinuationDto
import com.rarible.protocol.order.core.model.Continuation
import org.springframework.core.convert.converter.Converter

object ContinuationConverter : Converter<ActivityContinuationDto, Continuation> {
    override fun convert(source: ActivityContinuationDto): Continuation {
        return Continuation(source.afterDate, source.afterId)
    }
}
