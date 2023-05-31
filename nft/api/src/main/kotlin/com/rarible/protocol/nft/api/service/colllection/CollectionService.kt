package com.rarible.protocol.nft.api.service.colllection

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.nft.api.configuration.NftIndexerApiProperties
import com.rarible.protocol.nft.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.nft.core.converters.dto.CollectionDtoConverter
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.SignedTokenId
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.model.TokenFilter
import com.rarible.protocol.nft.core.model.TokenMeta
import com.rarible.protocol.nft.core.repository.TokenIdRepository
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import com.rarible.protocol.nft.core.service.item.meta.MetaException
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import com.rarible.protocol.nft.core.service.token.TokenService
import com.rarible.protocol.nft.core.service.token.meta.TokenMetaService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.time.withTimeout
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import scalether.abi.Uint256Type
import scalether.domain.Address
import scalether.util.Hex
import java.math.BigInteger
import java.time.Duration

@Component
class CollectionService(
    private val nftIndexerApiProperties: NftIndexerApiProperties,
    private val tokenService: TokenService,
    private val tokenRepository: TokenRepository,
    private val tokenIdRepository: TokenIdRepository,
    private val tokenMetaService: TokenMetaService
) {
    private val logger = LoggerFactory.getLogger(CollectionService::class.java)
    private val operatorPrivateKey = Numeric.toBigInt(Hex.toBytes(nftIndexerApiProperties.operator.privateKey))
    private val operatorPublicKey = Sign.publicKeyFromPrivate(operatorPrivateKey)

    suspend fun get(collectionId: Address): NftCollectionDto {
        val token = tokenRepository.findById(collectionId).awaitFirstOrNull()
            ?.takeIf { it.standard.isNotIgnorable() && it.status != ContractStatus.ERROR }
            ?: throw EntityNotFoundApiException("Collection", collectionId)
        return CollectionDtoConverter.convert(token)
    }

    suspend fun get(ids: List<Address>): List<NftCollectionDto> {
        val tokens = tokenRepository.findByIds(ids).toList()
        return tokens.map { CollectionDtoConverter.convert(it) }
    }

    suspend fun getMetaWithTimeout(
        address: Address,
        timeout: Duration
    ): TokenMeta {
        return try {
            withTimeout(timeout) {
                tokenMetaService.get(address)
            }
        } catch (e: CancellationException) {
            val message = "Collection meta load timeout (${timeout.toMillis()}ms) - ${e.message}"
            logMetaLoading(address, message, warn = true)
            throw MetaException(message, status = MetaException.Status.Timeout)
        } catch (e: MetaException) {
            val message = "Collection meta load failed (${e.status}) - ${e.message}"
            logMetaLoading(address, message, warn = true)
            throw e
        } catch (e: Exception) {
            val message = "Collection meta load failed with unexpected error - ${e.message}"
            logMetaLoading(address, message, warn = true)
            throw MetaException(message, status = MetaException.Status.Unknown)
        }
    }

    suspend fun search(filter: TokenFilter): List<Token> {
        return tokenRepository.search(filter).collectList().awaitFirst()
    }

    suspend fun generateId(collectionId: Address, minter: Address): SignedTokenId {
        val token = tokenService
            .register(collectionId)
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
