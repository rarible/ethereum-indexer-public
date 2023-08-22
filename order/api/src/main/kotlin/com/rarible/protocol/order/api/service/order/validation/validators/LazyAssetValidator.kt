package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.ethereum.nft.model.LazyNft
import com.rarible.ethereum.nft.validation.LazyNftValidator
import com.rarible.ethereum.nft.validation.ValidationResult
import com.rarible.protocol.dto.EthereumOrderUpdateApiErrorDto
import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.order.core.validator.OrderValidator
import com.rarible.protocol.order.core.converters.model.LazyAssetTypeToLazyNftConverter
import com.rarible.protocol.order.core.exception.OrderUpdateException
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.AssetType.Companion.isLazy
import com.rarible.protocol.order.core.model.Order
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Service
import scalether.abi.Uint256Type
import java.util.Arrays

@Service
class LazyAssetValidator(
    private val delegate: LazyNftValidator,
    private val nftCollectionClient: NftCollectionControllerApi
) : OrderValidator {

    override val type = "lazy"

    override suspend fun validate(order: Order) {
        order.make.type.takeIf { it.isLazy }
            ?.let { validate(it, "make") }

        order.take.type.takeIf { it.isLazy }
            ?.let { validate(it, "take") }
    }

    override fun supportsValidation(order: Order): Boolean = order.make.type.isLazy || order.take.type.isLazy

    private suspend fun validate(lazyAssetType: AssetType, side: String) {
        val lazyNft = LazyAssetTypeToLazyNftConverter.convert(lazyAssetType)
        validate(lazyNft, side)
    }

    private suspend fun validate(lazyNft: LazyNft, side: String) {
        val collection = nftCollectionClient.getNftCollectionById(lazyNft.token.hex()).awaitFirstOrNull()
            ?: throw OrderUpdateException(
                "Token ${lazyNft.token} was not found",
                EthereumOrderUpdateApiErrorDto.Code.INCORRECT_LAZY_ASSET
            )

        if (collection.features.contains(NftCollectionDto.Features.MINT_AND_TRANSFER)) {
            val tokenId = Uint256Type.encode(lazyNft.tokenId).bytes()
            val firstCreator = lazyNft.creators.first().account.bytes()

            if (tokenId.size < firstCreator.size) {
                throw OrderUpdateException(
                    "TokenId has invalid hex size",
                    EthereumOrderUpdateApiErrorDto.Code.INCORRECT_LAZY_ASSET
                )
            }
            if (Arrays.equals(firstCreator, tokenId.sliceArray(firstCreator.indices)).not()) {
                throw OrderUpdateException(
                    "TokenId must start with first creator address",
                    EthereumOrderUpdateApiErrorDto.Code.INCORRECT_LAZY_ASSET
                )
            }
        }
        val errorMessage = when (val result = delegate.validate(lazyNft)) {
            ValidationResult.Valid -> return
            ValidationResult.InvalidCreatorAndSignatureSize -> "Invalid creator and signature size for lazy asset on $side side"
            ValidationResult.NotUniqCreators -> "All creators must be uniq for lazy asset on $side side"
            is ValidationResult.InvalidCreatorSignature -> "Invalid signatures for creators ${result.creators} for lazy asset on $side side"
        }
        throw OrderUpdateException(
            errorMessage,
            EthereumOrderUpdateApiErrorDto.Code.INCORRECT_LAZY_ASSET
        )
    }
}
