package com.rarible.protocol.order.listener.service.descriptors.exchange.v2

import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import scalether.domain.Address
import java.math.BigInteger

@FlowPreview
@IntegrationTest
class ExchangeV2MatchLegacyDescriptorTest : ExchangeV2BaseMatchDescriptorTests() {
    override fun hashToSign(structHash: Word): Word = legacyEip712Domain.hashToSign(structHash)
    override fun fills(hash: ByteArray): Mono<BigInteger> = legacyExchange.fills(hash).call()
    override fun exchangeAddress(): Address = legacyExchange.address()

    @BeforeEach
    fun setupAddresses() {
        orderIndexerProperties.exchangeContractAddresses.v2 = legacyExchange.address()
        prepareTxService.eip712Domain = legacyEip712Domain
    }

    @Test
    fun `partially match make-fill sell order - data V2`() = runBlocking<Unit> {
        `test partially match make-fill sell order - data V2`()
    }

    @Test
    fun `fully match make-fill sell order - data V2`() = runBlocking<Unit> {
        `test fully match make-fill sell order - data V2`()
    }

    @Test
    fun `partially match take-fill bid order - data V2`() = runBlocking<Unit> {
        `test partially match take-fill bid order - data V2`()
    }

    @Test
    fun `fully match take-fill bid order - data V1`() = runBlocking<Unit> {
        `test fully match take-fill bid order - data V1`()
    }

    @Test
    fun `fully match take-fill bid order with payout - data V1`() = runBlocking<Unit> {
        `test fully match take-fill bid order with payout - data V1`()
    }
}
