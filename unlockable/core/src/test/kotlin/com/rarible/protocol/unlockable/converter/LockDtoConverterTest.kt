package com.rarible.protocol.unlockable.converter

import com.rarible.core.common.nowMillis
import com.rarible.protocol.unlockable.domain.Lock
import io.daonomic.rpc.domain.Binary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.util.*

// TODO remake with mapstruct after Java 11 migration finished
class LockDtoConverterTest {

    @Test
    fun `map lock`() {
        val lock = Lock(
            "itemId",
            "content",
            Address.THREE(),
            Binary.apply(ByteArray(4) { it.toByte() }),
            nowMillis().minusSeconds(120),
            UUID.randomUUID().toString(),
            432
        )

        val mapper = LockDtoConverter
        val dto = mapper.convert(lock)

        assertEquals(lock.itemId, dto.itemId)
        assertEquals(lock.content, dto.content)
        assertEquals(lock.author, dto.author)
        assertEquals(lock.signature, dto.signature)
        assertEquals(lock.unlockDate, dto.unlockDate)
        assertEquals(lock.version, dto.version)
        assertEquals(lock.id, dto.id)
    }


}
