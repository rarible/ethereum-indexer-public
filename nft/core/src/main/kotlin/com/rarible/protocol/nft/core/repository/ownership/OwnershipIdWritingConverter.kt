package com.rarible.protocol.nft.core.repository.ownership

import com.rarible.protocol.nft.core.model.OwnershipId
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter
import org.springframework.stereotype.Component

@Component
@WritingConverter
object OwnershipIdWritingConverter : Converter<OwnershipId, String> {
    override fun convert(source: OwnershipId): String = source.stringValue
}
