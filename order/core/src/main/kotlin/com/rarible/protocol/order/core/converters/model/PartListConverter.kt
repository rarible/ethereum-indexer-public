package com.rarible.protocol.order.core.converters.model

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.order.core.model.Part
import org.springframework.core.convert.converter.Converter

object PartListConverter : Converter<List<PartDto>, List<Part>> {
    override fun convert(source: List<PartDto>): List<Part> {
        return source.map { Part(it.account, EthUInt256.of(it.value.toLong())) }
    }
}
