package com.rarible.protocol.unlockable.converter

import com.rarible.protocol.dto.UnlockableEventDto
import com.rarible.protocol.unlockable.event.LockEvent
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object LockEventDtoConverter : Converter<LockEvent, UnlockableEventDto> {

    override fun convert(event: LockEvent): UnlockableEventDto {
        return UnlockableEventDto(
            event.id,
            event.itemId,
            UnlockableEventDto.Type.valueOf(event.type.name)
        )
    }


}