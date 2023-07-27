package com.rarible.protocol.nft.core.repository.ownership

import com.rarible.protocol.nft.core.model.OwnershipId
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.stereotype.Component

@Component
@ReadingConverter
object OwnershipIdReadingConverter : Converter<String, OwnershipId> {
    override fun convert(source: String): OwnershipId = OwnershipId.parseId(source)
}
