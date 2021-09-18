package com.rarible.protocol.nft.api.service.mint

import com.rarible.core.common.convert
import com.rarible.ethereum.nft.model.LazyNft
import com.rarible.ethereum.nft.validation.ValidationResult
import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.nft.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.nft.api.exceptions.ValidationApiException
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.repository.TokenRepository
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component
import scalether.abi.Uint256Type
import java.util.*
import com.rarible.ethereum.nft.validation.LazyNftValidator as DaonomicLazyNftValidator

@Component
class LazyNftValidator(
    private val delegate: DaonomicLazyNftValidator,
    private val conversionService: ConversionService,
    private val tokenRepository: TokenRepository
) {
    suspend fun validate(lazyNftDto: LazyNftDto) {
        val token = tokenRepository.findById(lazyNftDto.contract).awaitFirstOrNull()
            ?: throw EntityNotFoundApiException("LazyNft", lazyNftDto.contract)

        if (token.features.contains(TokenFeature.MINT_AND_TRANSFER)) {
            val tokenId = Uint256Type.encode(lazyNftDto.tokenId).bytes()
            val firstCreator = lazyNftDto.creators.first().account.bytes()

            if (tokenId.size < firstCreator.size) {
                throw ValidationApiException("TokenId $token has invalid hex size")
            }
            if (Arrays.equals(firstCreator, tokenId.sliceArray(firstCreator.indices)).not()) {
                throw ValidationApiException("TokenId $token must start with first creator address")
            }
        }

        val lazyNft = conversionService.convert<LazyNft>(lazyNftDto)

        val errorMessage = when (val result = delegate.validate(lazyNft)) {
            ValidationResult.Valid -> return
            ValidationResult.InvalidCreatorAndSignatureSize -> "Invalid creator and signature size"
            ValidationResult.NotUniqCreators -> "All creators must be uniq"
            is ValidationResult.InvalidCreatorSignature -> "Invalid signatures for creators ${result.creators}"
        }
        throw ValidationApiException(errorMessage)
    }
}
