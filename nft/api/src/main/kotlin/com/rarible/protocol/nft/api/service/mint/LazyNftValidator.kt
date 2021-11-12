package com.rarible.protocol.nft.api.service.mint

import com.rarible.contracts.interfaces.erc1155.lazymint.IERC1155LazyMint
import com.rarible.core.apm.CaptureSpan
import com.rarible.contracts.interfaces.erc721.lazymint.IERC721LazyMint
import com.rarible.ethereum.nft.validation.ValidationResult
import com.rarible.ethereum.sign.service.InvalidSignatureException
import com.rarible.protocol.dto.LazyErc1155Dto
import com.rarible.protocol.dto.LazyErc721Dto
import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.nft.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.nft.api.exceptions.ValidationApiException
import com.rarible.protocol.nft.core.converters.model.LazyNftDtoToDaonomicLazyNftConverter
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.span.SpanType
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import scala.Tuple2
import scala.Tuple5
import scala.Tuple6
import scalether.abi.Uint256Type
import scalether.transaction.MonoTransactionSender
import java.util.*
import com.rarible.ethereum.nft.validation.LazyNftValidator as DaonomicLazyNftValidator

@Component
@CaptureSpan(type = SpanType.SERVICE, subtype = "lazy-nft-validator")
class LazyNftValidator(
    private val delegate: DaonomicLazyNftValidator,
    private val tokenRepository: TokenRepository,
    private val sender: MonoTransactionSender
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
        } else {
            throw ValidationApiException("This collection (${token.id}) doesn't support lazy mint")
        }

        val lazyNft = LazyNftDtoToDaonomicLazyNftConverter.convert(lazyNftDto)

        val result = try {
            delegate.validate(lazyNft)
        } catch (e: InvalidSignatureException) {
            throw ValidationApiException(e.message ?: "Invalid structure of signature")
        }

        try {
            checkOwner(lazyNftDto)
        } catch (e: InvalidSignatureException) {
            throw ValidationApiException("It isn't allowed to lazy mint")
        }

        val errorMessage = when (result) {
            ValidationResult.Valid -> return
            ValidationResult.InvalidCreatorAndSignatureSize -> "Invalid creator and signature size"
            ValidationResult.NotUniqCreators -> "All creators must be uniq"
            is ValidationResult.InvalidCreatorSignature -> "Invalid signatures for creators ${result.creators}"
        }
        throw ValidationApiException(errorMessage)
    }

    private suspend fun checkOwner(lazyNftDto: LazyNftDto) = when (lazyNftDto) {
        is LazyErc721Dto -> checkOwnerERC721(lazyNftDto)
        is LazyErc1155Dto -> checkOwnerERC1155(lazyNftDto)
        else -> throw ValidationApiException("Standard doesn't support yet")
    }

    private suspend fun checkOwnerERC721(lazyNftDto: LazyErc721Dto) {
        val contract = IERC721LazyMint(lazyNftDto.contract, sender)
        val creators = lazyNftDto.creators.map { Tuple2(it.account, it.value.toBigInteger()) }
        val royalties = lazyNftDto.royalties.map { Tuple2(it.account, it.value.toBigInteger()) }
        val signatures = lazyNftDto.signatures.map { it.bytes() }
        val mintData = Tuple5(
            lazyNftDto.tokenId,
            lazyNftDto.uri,
            creators.toTypedArray(),
            royalties.toTypedArray(),
            signatures.toTypedArray()
        )
        contract.mintAndTransfer(mintData, lazyNftDto.creators.first().account)
            .withFrom(lazyNftDto.creators.first().account)
            .call().awaitFirstOrNull()
    }

    private suspend fun checkOwnerERC1155(lazyNftDto: LazyErc1155Dto) {
        val contract = IERC1155LazyMint(lazyNftDto.contract, sender)
        val creators = lazyNftDto.creators.map { Tuple2(it.account, it.value.toBigInteger()) }
        val royalties = lazyNftDto.royalties.map { Tuple2(it.account, it.value.toBigInteger()) }
        val signatures = lazyNftDto.signatures.map { it.bytes() }
        val mintData = Tuple6(
            lazyNftDto.tokenId,
            lazyNftDto.uri,
            lazyNftDto.supply,
            creators.toTypedArray(),
            royalties.toTypedArray(),
            signatures.toTypedArray()
        )
        contract.mintAndTransfer(mintData, lazyNftDto.creators.first().account, lazyNftDto.supply)
            .withFrom(lazyNftDto.creators.first().account)
            .call().awaitFirstOrNull()
    }
}
