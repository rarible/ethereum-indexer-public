package com.rarible.protocol.order.core.converters.model

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.order.core.model.Part
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object PartConverter : Converter<PartDto, Part> {
    override fun convert(source: PartDto): Part {
        return Part(source.account, EthUInt256.of(source.value.toLong()))
    }
}
