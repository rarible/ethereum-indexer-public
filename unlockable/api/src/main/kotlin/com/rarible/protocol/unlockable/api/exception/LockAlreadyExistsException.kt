package com.rarible.protocol.unlockable.api.exception

import com.rarible.protocol.dto.UnlockableApiErrorDto

class LockAlreadyExistsException : LockOperationException(
    "Item is unlockable already",
    UnlockableApiErrorDto.Code.LOCK_EXISTS
)