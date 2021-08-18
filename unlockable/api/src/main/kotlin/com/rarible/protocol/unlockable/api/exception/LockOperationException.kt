package com.rarible.protocol.unlockable.api.exception

import com.rarible.protocol.dto.UnlockableApiErrorDto

open class LockOperationException(
    message: String,
    val errorCode: UnlockableApiErrorDto.Code
) : RuntimeException(message)