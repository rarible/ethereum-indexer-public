package com.rarible.protocol.order.listener.service.descriptors.exchange.zero.ex

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.SimpleTraceResult
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.trace.TraceCallService
import com.rarible.protocol.order.core.trace.TransactionTraceProvider
import com.rarible.protocol.order.listener.service.zero.ex.ZeroExOrderEventConverter
import com.rarible.protocol.order.listener.service.zero.ex.ZeroExOrderParser
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import scalether.domain.Address
import scalether.domain.AddressFactory
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import scalether.java.Lists
import java.math.BigInteger
import java.time.Instant

@ExtendWith(MockKExtension::class)
class ZeroExExchangeOrderMatchDescriptorComplexTest {

    private lateinit var zeroExExchangeOrderMatchDescriptor: ZeroExExchangeOrderMatchDescriptor

    @MockK
    private lateinit var exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses

    @MockK
    private lateinit var priceUpdateService: PriceUpdateService

    @MockK
    private lateinit var priceNormalizer: PriceNormalizer

    @Test
    fun `convert buying by sell order`() = runBlocking<Unit> {
        every {
            runBlocking {
                priceUpdateService.getAssetsUsdValue(any(), any(), any())
            }
        } returns null
        every {
            runBlocking {
                priceNormalizer.normalize(any())
            }
        } returns 1.toBigDecimal()
        zeroExExchangeOrderMatchDescriptor = ZeroExExchangeOrderMatchDescriptor(
            exchangeContractAddresses = exchangeContractAddresses,
            zeroExOrderEventConverter = ZeroExOrderEventConverter(
                priceUpdateService = priceUpdateService,
                priceNormalizer = priceNormalizer,
                exchangeDomainHash = "0x17068c8fc502c4938835d37c402e7c17f51ec6895246726893d5fe3198085a67"
            ),
            zeroExOrderParser = ZeroExOrderParser(TraceCallService(TestTransactionTraceProvider()))
        )

        // https://polygonscan.com/tx/0x08ef7447c15669631b1b54bf6b035df31c0b3b19720a760b2093db8b1eeb14cb
        val log = Log(
            BigInteger.ONE, // logIndex
            BigInteger.TEN, // transactionIndex
            Word.apply(ByteArray(32)), // transactionHash
            Word.apply(ByteArray(32)), // blockHash
            BigInteger.ZERO, // blockNumber
            Address.ZERO(), // address
            Binary.apply( // data
                "0x0000000000000000000000000000000000000000000000000000000000000160" +
                    "00000000000000000000000000000000000000000000000000000000000001c0" +
                    "0000000000000000000000000000000000000000000000000000000000000240" +
                    "0000000000000000000000000000000000000000000000000000000000000260" +
                    "000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d" +
                    "000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d" +
                    "0000000000000000000000000000000000000000000000000005666e940f0000" +
                    "0000000000000000000000000000000000000000000000000000000000000001" +
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "0000000000000000000000000000000000000000000000000029911687df4000" +
                    "0000000000000000000000000000000000000000000000000000000000000024" +
                    "f47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cf" +
                    "f1b9f61900000000000000000000000000000000000000000000000000000000" +
                    "0000000000000000000000000000000000000000000000000000000000000044" +
                    "025717920000000000000000000000002b4a66557a79263275826ad31a4cddc2" +
                    "789334bd00000000000000000000000000000000000000000000000000000000" +
                    "0000762b00000000000000000000000000000000000000000000000000000000" +
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "0000000000000000000000000000000000000000000000000000000000000000"
            ),
            false, // removed
            Lists.toScala( // topics
                listOf(
                    Word.apply("0x6869791f0a34781b29882982cc39e882768cf2c96995c2a110c577c53bc932d5"),
                    Word.apply("0x0000000000000000000000004d3b39791d9bfe56304b32c35fe8f3d411d85a02"),
                    Word.apply("0x000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d"),
                    Word.apply("0x935b13465952ccbf981a264761b2edf0c0edf46f2e62a6a57ab37a32b224d6b0"),
                )
            ),
            "" // type
        )

        val transaction = Transaction(
            Word.apply(ByteArray(32)), // hash
            11.toBigInteger(), // nonce
            Word.apply(ByteArray(32)), // blockHash
            222.toBigInteger(), // blockNumber
            AddressFactory.create(), // creates
            333.toBigInteger(), // transactionIndex
            AddressFactory.create(), // from
            AddressFactory.create(), // to
            444.toBigInteger(), // value
            555.toBigInteger(), // gasPrice
            777.toBigInteger(), // gas
            Binary.apply( // input
                transactionInput
            )
        )
        val date = Instant.now().epochSecond
        val index = 0
        val totalLogs = 2
        val result = zeroExExchangeOrderMatchDescriptor.convert(
            log = log,
            transaction = transaction,
            timestamp = date,
            index = index,
            totalLogs = totalLogs
        ).awaitSingle()

        val leftOrderHash: Word = Word.apply("0x935b13465952ccbf981a264761b2edf0c0edf46f2e62a6a57ab37a32b224d6b0")
        val rightOrderHash: Word = Word.apply("0xdcbcc2880a85e7b7e1b80c2fd714dcd6f275ee203298a52a0702b75691aec628")

        val leftMakeErc20Asset = Asset(
            type = Erc20AssetType(token = Address.apply("0x7ceb23fd6bc0add59e62ac25578270cff1b9f619")),
            value = EthUInt256.of(1520000000000000.toBigInteger())
        )
        val leftTakeErc721Asset = Asset(
            type = Erc721AssetType(
                token = Address.apply("0x2b4a66557a79263275826ad31a4cddc2789334bd"),
                tokenId = EthUInt256.of("0x000000000000000000000000000000000000000000000000000000000000762b")
            ),
            value = EthUInt256.ONE
        )
        val expOrderSideMatch =
            OrderSideMatch(
                hash = leftOrderHash,
                counterHash = rightOrderHash,
                side = OrderSide.RIGHT,
                fill = EthUInt256(1.toBigInteger()),
                make = leftMakeErc20Asset,
                take = leftTakeErc721Asset,
                maker = Address.apply("0x4d3b39791d9bfe56304b32c35fe8f3d411d85a02"),
                taker = Address.apply("0xdfa346c49c159c58d8316978b0f721ecebd10a3c"),
                makeUsd = null,
                takeUsd = null,
                makePriceUsd = null,
                takePriceUsd = null,
                makeValue = 1.toBigDecimal(),
                takeValue = 1.toBigDecimal(),
                date = Instant.ofEpochSecond(date),
                source = HistorySource.OPEN_SEA,
                externalOrderExecutedOnRarible = false,
                adhoc = false,
                counterAdhoc = false,
            )
        assertThat(result).isEqualTo(expOrderSideMatch)
    }

    private companion object {
        const val transactionInput =
            "0xbbbfa60c00000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000000000000003e0000000000000000000000000000000000000000000000000000000000000072000000000000000000000000000000000000000000000000000000000000007a000000000000000000000000000000000000000000000000000000000000008200000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f6190000000000000000000000004d3b39791d9bfe56304b32c35fe8f3d411d85a020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d0000000000000000000000000000000000000000000000000005666e940f000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000625935e7000000000000000000000000000000000000000000000000015909b08de1da0100000000000000000000000000000000000000000000000000000000000001c0000000000000000000000000000000000000000000000000000000000000022000000000000000000000000000000000000000000000000000000000000002a000000000000000000000000000000000000000000000000000000000000002e00000000000000000000000000000000000000000000000000000000000000024f47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000044025717920000000000000000000000002b4a66557a79263275826ad31a4cddc2789334bd000000000000000000000000000000000000000000000000000000000000762b000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000dfa346c49c159c58d8316978b0f721ecebd10a3c0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d00000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000005666e940f00000000000000000000000000000000000000000000000000000000accdd281e000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000625935e700000000000000000000000000000000000000000000000000fef66f825d18ca00000000000000000000000000000000000000000000000000000000000001c0000000000000000000000000000000000000000000000000000000000000024000000000000000000000000000000000000000000000000000000000000002a000000000000000000000000000000000000000000000000000000000000003000000000000000000000000000000000000000000000000000000000000000044025717920000000000000000000000002b4a66557a79263275826ad31a4cddc2789334bd000000000000000000000000000000000000000000000000000000000000762b000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000024f47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000024f47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000421c7763a38bdc4a8cc3bfdab4302a54c5c76f0aafb5ff54fafb4a809d34d84277dd3a940201f11fd8bf824c2732af189ef6f922ef351157ea0d3a3c13e6c4543db30300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000421c40e0c8008601b2df04d165574e899598e63caa7f7a68f2c2ba274bd088c56e8162513d0f1a69a31770981117d8533133295cef7fa4ec5978376caa8e7ff8c0d10300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000020000000000000000000000005b3256965e7c3cf26e11fcaf296dfc8807c010730000000000000000000000000000000000000000000000000000228f908060000000000000000000000000000bbdd174198c3bafff09f58d62119e680141ab4400000000000000000000000000000000000000000000000000008a3e42018000"
    }

    private class TestTransactionTraceProvider : TransactionTraceProvider {
        override suspend fun traceAndFindFirstCallTo(
            transactionHash: Word,
            to: Address,
            id: Binary
        ): SimpleTraceResult? = null

        override suspend fun traceAndFindAllCallsTo(
            transactionHash: Word,
            to: Address,
            id: Binary
        ): List<SimpleTraceResult> = listOf()
    }
}