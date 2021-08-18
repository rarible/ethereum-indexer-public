package com.rarible.protocol.gateway.service.transaction

import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.CreateTransactionRequestDto
import com.rarible.protocol.dto.LogEventDto
import com.rarible.protocol.gateway.service.cluster.IndexerApiClientProvider
import com.rarible.protocol.nft.api.client.NftTransactionControllerApi
import com.rarible.protocol.order.api.client.OrderTransactionControllerApi
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import reactor.core.publisher.Flux
import java.util.stream.Stream

internal class TransactionServiceTest {
    private val indexerApiClientProvider = mockk<IndexerApiClientProvider>()
    private val transactionService = TransactionService(indexerApiClientProvider)

    private val nftTransactionApi = mockk<NftTransactionControllerApi>()
    private val orderTransactionApi = mockk<OrderTransactionControllerApi>()

    @BeforeEach
    fun setup() {
        clearMocks(indexerApiClientProvider, nftTransactionApi, orderTransactionApi)
    }

    companion object {
        @JvmStatic
        fun getBlockchains(): Stream<Blockchain> {
            return Blockchain.values().asList().stream()
        }
    }

    @ParameterizedTest
    @MethodSource("getBlockchains")
    fun `should aggregate transactions from nft and orders`(blockchain: Blockchain) = runBlocking<Unit> {
        every { indexerApiClientProvider.getNftTransactionApiClient(eq(blockchain)) } returns nftTransactionApi
        every { indexerApiClientProvider.getOrderTransactionApiClient(eq(blockchain)) } returns orderTransactionApi

        val transactionRequest = mockk<CreateTransactionRequestDto>()
        val nftLogEvents = listOf<LogEventDto>(mockk(), mockk())
        val orderLogEvents = listOf<LogEventDto>(mockk(), mockk())

        coEvery { nftTransactionApi.createNftPendingTransaction(eq(transactionRequest)) } returns Flux.fromIterable(
            nftLogEvents
        )
        coEvery { orderTransactionApi.createOrderPendingTransaction(eq(transactionRequest)) } returns Flux.fromIterable(
            orderLogEvents
        )

        val result = transactionService.createPendingTransaction(blockchain, transactionRequest)
        Assertions.assertThat(result).hasSize(4)
        Assertions.assertThat(result).containsExactlyInAnyOrderElementsOf(nftLogEvents + orderLogEvents)
    }
}
