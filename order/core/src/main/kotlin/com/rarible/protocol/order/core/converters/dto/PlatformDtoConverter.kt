package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.core.model.Platform
import org.springframework.core.convert.converter.Converter

object PlatformDtoConverter : Converter<Platform, PlatformDto> {
    override fun convert(source: Platform): PlatformDto {
        return when (source) {
            Platform.RARIBLE -> PlatformDto.RARIBLE
            Platform.OPEN_SEA -> PlatformDto.OPEN_SEA
            Platform.CRYPTO_PUNKS -> PlatformDto.CRYPTO_PUNKS
        }
    }
}
