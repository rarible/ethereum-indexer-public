package com.rarible.protocol.nft.core.service.token

import com.google.common.cache.CacheBuilder
import com.rarible.protocol.nft.core.misc.LogUtils
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.TokenByteCode
import com.rarible.protocol.nft.core.repository.token.TokenByteCodeRepository
import io.daonomic.rpc.domain.Word
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.util.Hash
import java.time.Duration

@Component
class TokenByteCodeService(
    private val byteCodeProvider: TokenByteCodeProvider,
    private val tokenByteCodeRepository: TokenByteCodeRepository,
    private val featureFlags: FeatureFlags,
) {
    private val wasSaved = CacheBuilder.newBuilder()
        .maximumSize(CACHE_SIZE)
        .expireAfterWrite(CACHE_EXPIRE_AFTER)
        .build<Word, Boolean>()

    suspend fun getByteCode(token: Address): TokenByteCode? {
        val code = byteCodeProvider.fetchByteCode(token)
        if (code == null || code.length() == 0) {
            logger.info("Can't get token byte code for $token")
            return null
        }
        val hash = Word.apply(Hash.sha3(code.bytes()))
        return TokenByteCode(hash, code)
            .also { save(it) }
            .also {
                LogUtils.addToMdc("tokenByteCodeHash" to it.hash.prefixed()) {
                   logger.info("Get token $token byte code (size=${it.code.length()})")
                }
            }
    }

    private suspend fun save(tokenByteCode: TokenByteCode) {
        if (featureFlags.saveTokenByteCode &&
            !recordExist(tokenByteCode.hash)
        ) {
            tokenByteCodeRepository.save(tokenByteCode)
            wasSaved.put(tokenByteCode.hash, true)
        }
    }

    private suspend fun recordExist(hash: Word): Boolean {
        return wasSaved.getIfPresent(hash) == true || tokenByteCodeRepository.exist(hash)
    }

    private val logger = LoggerFactory.getLogger(TokenByteCodeService::class.java)

    private companion object {
        const val CACHE_SIZE = 1000L
        val CACHE_EXPIRE_AFTER: Duration = Duration.ofDays(1)
    }
}
