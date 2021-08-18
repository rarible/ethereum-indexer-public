package com.rarible.protocol.unlockable.converter

import com.rarible.protocol.dto.LockDto
import com.rarible.protocol.unlockable.domain.Lock
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

// TODO remake with mapstruct after Java 11 migration finished
@Component
object LockDtoConverter : Converter<Lock, LockDto> {
    override fun convert(source: Lock): LockDto {
        return source.run {
            LockDto(
                id,
                itemId,
                content,
                author,
                signature,
                unlockDate,
                version
            )
        }
    }
}
