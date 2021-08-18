package com.rarible.protocol.unlockable.api.exception

import com.rarible.protocol.dto.UnlockableApiErrorDto

class LockOwnershipException : LockOperationException(
    "Item is unlockable already",
    UnlockableApiErrorDto.Code.OWNERHIP_ERROR
)