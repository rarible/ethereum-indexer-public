package com.rarible.protocol.nft.api.service.colllection

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.api.configuration.NftIndexerApiProperties.OperatorProperties
import com.rarible.protocol.nft.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.repository.TokenIdRepository
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import scalether.abi.Uint256Type
import scalether.domain.Address
import scalether.util.Hex
import java.math.BigInteger

@Component
class CollectionService(
    operator: OperatorProperties,
    private val tokenRegistrationService: TokenRegistrationService,
    private val tokenRepository: TokenRepository,
    private val tokenIdRepository: TokenIdRepository
) {
    private val operatorPrivateKey = Numeric.toBigInt(Hex.toBytes(operator.privateKey))
    private val operatorPublicKey = Sign.publicKeyFromPrivate(operatorPrivateKey)

    suspend fun get(collectionId: Address): Token {
        return tokenRepository.findById(collectionId).awaitFirstOrNull()
            ?.takeIf { it.standard != TokenStandard.NONE }
            ?: throw EntityNotFoundApiException("Collection", collectionId)
    }

    suspend fun search(filter: TokenFilter): List<Token> {
        return tokenRepository.search(filter).collectList().awaitFirst()
    }

    suspend fun generateId(collectionId: Address, minter: Address): SignedTokenId {
        val token = tokenRegistrationService
            .register(collectionId).awaitFirstOrNull()
            ?: throw EntityNotFoundApiException("Collection", collectionId)

        val hasMintAndTransferFeature = token.features.contains(TokenFeature.MINT_AND_TRANSFER)
        val tokenIdKey = if (hasMintAndTransferFeature) "$collectionId:$minter" else "$collectionId"

        val nextTokenId = tokenIdRepository.generateTokenId(tokenIdKey)
            .toBigInteger()
            .generateUint256(minter.takeIf { hasMintAndTransferFeature })

        val sign = sign(token, nextTokenId)
        return SignedTokenId(nextTokenId, sign)
    }

    private fun BigInteger.generateUint256(minter: Address?): EthUInt256 {
        assert(this < BigInteger.valueOf(2).pow(96)) { "tokenId size error" }
        val encoded = Uint256Type.encode(this).slice(20, 32)

        val binary = if (minter != null) {
            minter.add(encoded)
        } else {
            encoded
        }
        return EthUInt256.of(Uint256Type.decode(binary, 0).value())
    }

    private fun sign(token: Token, nextTokenId: EthUInt256): Sign.SignatureData {
        val address = if (token.features.contains(TokenFeature.MINT_WITH_ADDRESS)) token.id else null
        return sign(nextTokenId.value, address)
    }

    fun sign(value: BigInteger, address: Address? = null): Sign.SignatureData {
        val toSign = if (address != null) {
            address.add(Uint256Type.encode(value))
        } else {
            Uint256Type.encode(value)
        }
        return Sign.signMessage(toSign.bytes(), operatorPublicKey, operatorPrivateKey)
    }
}