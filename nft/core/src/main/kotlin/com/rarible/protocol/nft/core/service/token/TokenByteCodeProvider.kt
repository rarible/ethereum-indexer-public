package com.rarible.protocol.nft.core.service.token

import com.google.common.cache.CacheBuilder
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender
import java.time.Duration

@Component
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class TokenByteCodeProvider(
    private val sender: MonoTransactionSender,
    @Value("\${nft.token.registration.retryCount:3}") private val retryCount: Int,
    @Value("\${nft.token.registration.retryDelay:5000}") private val retryDelay: Long,
    @Value("\${nft.token.registration.codeCacheSize:100}") private val codeCacheSize: Long,
    @Value("\${nft.token.registration.codeCacheExpireAfter:10s}") private val codeCacheExpireAfter: Duration,
) {
    private val cache = CacheBuilder.newBuilder()
        .maximumSize(codeCacheSize)
        .expireAfterWrite(codeCacheExpireAfter)
        .build<Address, Binary>()

    suspend fun fetchByteCode(address: Address): Binary? {
        var retriesLeft = retryCount
        var bytecode: Binary?
        val cached = cache.getIfPresent(address)
        if (cached != null) return cached

        while (retriesLeft > 0) {
            bytecode = try {
                sender.ethereum()
                    .ethGetCode(address, "latest")
                    .awaitFirstOrNull()
            } catch (e: Exception) {
                null
            }
            if (bytecode == null || bytecode.hex().isNullOrEmpty()) {
                delay(retryDelay)
                retriesLeft--
            } else {
                cache.put(address, bytecode)
                return bytecode
            }
        }
        return null
    }
}
