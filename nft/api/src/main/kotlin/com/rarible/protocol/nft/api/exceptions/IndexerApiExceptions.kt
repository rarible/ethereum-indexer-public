package com.rarible.protocol.nft.api.exceptions

import com.rarible.protocol.dto.NftIndexerApiErrorDto
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.OwnershipId
import org.springframework.http.HttpStatus
import scalether.domain.Address
import java.math.BigInteger
import java.net.URI

sealed class IndexerApiException(
    message: String,
    val status: HttpStatus,
    val code: NftIndexerApiErrorDto.Code
) : Exception(message)

class IllegalArgumentException(message: String) : IndexerApiException(
    message = message,
    status = HttpStatus.BAD_REQUEST,
    code = NftIndexerApiErrorDto.Code.VALIDATION
)

class InvalidLazyNftException(message: String): IndexerApiException(
    message = message,
    status = HttpStatus.BAD_REQUEST,
    code = NftIndexerApiErrorDto.Code.INCORRECT_LAZY_NFT
)

class TokenNotFoundException(token: Address) : IndexerApiException(
    message = "Token $token not found",
    status = HttpStatus.NOT_FOUND,
    code = NftIndexerApiErrorDto.Code.TOKEN_NOT_FOUND
)

class TokenUrlNotFoundException(token: Address, tokenId: BigInteger) : IndexerApiException(
    message = "Token $token:$tokenId URI not found",
    status = HttpStatus.NOT_FOUND,
    code = NftIndexerApiErrorDto.Code.TOKEN_URI_NOT_FOUND
)

class TokenPropertiesExtractException(tokenURI: URI, cause: String) : IndexerApiException(
    message = "Can't extract token properties from $tokenURI, cause: $cause",
    status = HttpStatus.INTERNAL_SERVER_ERROR,
    code = NftIndexerApiErrorDto.Code.TOKEN_PROPERTIES_EXTRACT
)

class TokenPropertiesInvalidFormatException(properties: String) : IndexerApiException(
    message = "Token properties has no valid json format (value=\"$properties\")",
    status = HttpStatus.INTERNAL_SERVER_ERROR,
    code = NftIndexerApiErrorDto.Code.TOKEN_PROPERTIES_EXTRACT
)

class ItemNotFoundException(id: ItemId) : IndexerApiException(
    message = "Item $id not found",
    status = HttpStatus.NOT_FOUND,
    code = NftIndexerApiErrorDto.Code.ITEM_NOT_FOUND
)

class LazyItemNotFoundException(id: ItemId) : IndexerApiException(
    message = "Lazy item $id not found",
    status = HttpStatus.NOT_FOUND,
    code = NftIndexerApiErrorDto.Code.LAZY_ITEM_NOT_FOUND
)

class OwnershipNotFoundException(id: OwnershipId) : IndexerApiException(
    message = "Ownership $id not found",
    status = HttpStatus.NOT_FOUND,
    code = NftIndexerApiErrorDto.Code.OWNERSHIP_NOT_FOUND
)

class CollectionNotFoundException(id: Address) : IndexerApiException(
    message = "Collection $id not found",
    status = HttpStatus.NOT_FOUND,
    code = NftIndexerApiErrorDto.Code.COLLECTION_NOT_FOUND
)
