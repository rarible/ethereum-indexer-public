package com.rarible.protocol.order.api.service.order.validation

import com.rarible.core.common.convert
import com.rarible.ethereum.nft.model.LazyNft
import com.rarible.ethereum.nft.validation.LazyNftValidator
import com.rarible.ethereum.nft.validation.ValidationResult
import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.order.api.exceptions.InvalidLazyAssetException
import com.rarible.protocol.order.core.model.AssetType
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Service
import scalether.abi.Uint256Type
import java.util.*

@Service
class LazyAssetValidator(
    private val delegate: LazyNftValidator,
    private val nftCollectionClient: NftCollectionControllerApi,
    private val conversionService: ConversionService
) {
    suspend fun validate(lazyAssetType: AssetType, side: String) {
        val lazyNft = conversionService.convert<LazyNft>(lazyAssetType)

        val collection = nftCollectionClient.getNftCollectionById(lazyNft.token.hex()).awaitFirstOrNull()
            ?: throw InvalidLazyAssetException("Token ${lazyNft.token} was not found")

        if (collection.features.contains(NftCollectionDto.Features.MINT_AND_TRANSFER)) {
            val tokenId = Uint256Type.encode(lazyNft.tokenId).bytes()
            val firstCreator = lazyNft.creators.first().account.bytes()

            if (tokenId.size < firstCreator.size) {
                throw InvalidLazyAssetException("TokenId has invalid hex size")
            }
            if (Arrays.equals(firstCreator, tokenId.sliceArray(firstCreator.indices)).not()) {
                throw InvalidLazyAssetException("TokenId must start with first creator address")
            }
        }
        val errorMessage = when (val result = delegate.validate(lazyNft)) {
            ValidationResult.Valid -> return
            ValidationResult.InvalidCreatorAndSignatureSize -> "Invalid creator and signature size for lazy asset on $side side"
            ValidationResult.NotUniqCreators -> "All creators must be uniq for lazy asset on $side side"
            is ValidationResult.InvalidCreatorSignature -> "Invalid signatures for creators ${result.creators} for lazy asset on $side side"
        }
        throw InvalidLazyAssetException(errorMessage)
    }
}
