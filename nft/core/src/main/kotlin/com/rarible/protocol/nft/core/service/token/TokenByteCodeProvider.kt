package com.rarible.protocol.nft.core.service.token

import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender

@Component
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class TokenByteCodeProvider(
    private val sender: MonoTransactionSender,
    @Value("\${nft.token.registration.retryCount:3}") private val retryCount: Int,
    @Value("\${nft.token.registration.retryDelay:5000}") private val retryDelay: Long,
) {
    suspend fun fetchByteCode(address: Address): Binary? {
        var retriesLeft = retryCount
        var bytecode: Binary?

        while (retriesLeft > 0) {
            bytecode = try {
                sender.ethereum()
                    .ethGetCode(address, "latest")
                    .awaitFirstOrNull()
            } catch (e: Exception) {
                null
            }
            if (bytecode?.hex().isNullOrEmpty()) {
                delay(retryDelay)
                retriesLeft--
            } else {
                return bytecode
            }
        }
        return null
    }
}