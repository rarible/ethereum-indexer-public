package com.rarible.protocol.order.api.exceptions

import com.rarible.protocol.dto.OrderIndexerApiErrorDto
import com.rarible.protocol.order.core.model.AssetType
import io.daonomic.rpc.domain.Word
import org.springframework.http.HttpStatus
import scalether.domain.Address

sealed class IndexerApiException(
    message: String,
    val status: HttpStatus,
    val code: OrderIndexerApiErrorDto.Code
) : Exception(message)

class InvalidParameterException(message: String) : IndexerApiException(
    message = message,
    status = HttpStatus.BAD_REQUEST,
    code = OrderIndexerApiErrorDto.Code.INVALID_ARGUMENT
)

class OrderNotFoundException(hash: Word) : IndexerApiException(
    message = "Order $hash not found",
    status = HttpStatus.NOT_FOUND,
    code = OrderIndexerApiErrorDto.Code.ORDER_NOT_FOUND
)

class BalanceNotFoundException(token: Address, owner: Address) : IndexerApiException(
    message = "Balance of $token fow owner $owner not found",
    status = HttpStatus.BAD_REQUEST,
    code = OrderIndexerApiErrorDto.Code.BALANCE_NOT_FOUND
)

class OwnershipNotFoundException(ownershipId: String) : IndexerApiException(
    message = "Ownership $ownershipId not found",
    status = HttpStatus.BAD_REQUEST,
    code = OrderIndexerApiErrorDto.Code.OWNERSHIP_NOT_FOUND
)

class LazyItemNotFoundException(itemId: String) : IndexerApiException(
    message = "Lazy item $itemId not found",
    status = HttpStatus.BAD_REQUEST,
    code = OrderIndexerApiErrorDto.Code.LAZY_ITEM_NOT_FOUND
)

class AssetBalanceNotFoundException(owner: Address, assetType: AssetType) : IndexerApiException(
    message = "Can't get asset balance for owner=$owner and asset type ${assetType.javaClass.simpleName}",
    status = HttpStatus.BAD_REQUEST,
    code = OrderIndexerApiErrorDto.Code.ASSET_BALANCE_NOT_FOUND
)

class IncorrectOrderDataException(message: String) : IndexerApiException(
    message = message,
    status = HttpStatus.BAD_REQUEST,
    code = OrderIndexerApiErrorDto.Code.INCORRECT_ORDER_DATA
)

class InvalidLazyAssetException(message: String) : IndexerApiException(
    message = message,
    status = HttpStatus.BAD_REQUEST,
    code = OrderIndexerApiErrorDto.Code.INCORRECT_LAZY_ASSET
)

class IncorrectSignatureException(message: String = "Incorrect signature"): IndexerApiException(
    message = message,
    status = HttpStatus.BAD_REQUEST,
    code = OrderIndexerApiErrorDto.Code.INCORRECT_SIGNATURE
)

enum class OrderUpdateErrorReason(val error: OrderIndexerApiErrorDto.Code) {
    CANCELLED(OrderIndexerApiErrorDto.Code.ORDER_CANCELED),
    INVALID_UPDATE(OrderIndexerApiErrorDto.Code.ORDER_INVALID_UPDATE),
    MAKE_VALUE_ERROR(OrderIndexerApiErrorDto.Code.ORDER_INVALID_UPDATE),
    TAKE_VALUE_ERROR(OrderIndexerApiErrorDto.Code.ORDER_INVALID_UPDATE)
}

class OrderUpdateError(reason: OrderUpdateErrorReason) : IndexerApiException(
    message = "Order can't be updated: $reason",
    status = HttpStatus.BAD_REQUEST,
    code = reason.error
)
