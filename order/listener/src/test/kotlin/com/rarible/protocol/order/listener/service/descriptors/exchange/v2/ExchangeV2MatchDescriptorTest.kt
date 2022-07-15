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
class ExchangeV2MatchDescriptorTest : ExchangeV2BaseMatchDescriptorTests() {
    override fun hashToSign(structHash: Word): Word = eip712Domain.hashToSign(structHash)
    override fun fills(hash: ByteArray): Mono<BigInteger> = exchange.fills(hash)
    override fun exchangeAddress(): Address = exchange.address()

    @BeforeEach
    fun setupAddresses() {
        orderIndexerProperties.exchangeContractAddresses.v2 = exchange.address()
        prepareTxService.eip712Domain = eip712Domain
    }

    @Test
    fun `partially match order - data V3 sell`() = runBlocking<Unit> {
        `test partially match order - data V3 sell`()
    }

    @Test
    fun `fully match order sell order - data V3`() = runBlocking<Unit> {
        `test fully match order sell order - data V3`()
    }

    @Test
    fun `partially match bid order - data V3`() = runBlocking<Unit> {
        `test partially match bid order - data V3`()
    }

    @Test
    fun `fully match bid order - data V3`() = runBlocking<Unit> {
        `test fully match bid order - data V3`()
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