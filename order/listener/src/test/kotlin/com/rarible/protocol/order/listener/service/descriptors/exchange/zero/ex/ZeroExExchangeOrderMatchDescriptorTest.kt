package com.rarible.protocol.order.listener.service.descriptors.exchange.zero.ex

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.zero.ex.FillEvent
import com.rarible.protocol.order.core.data.randomErc20
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.ZeroExFeeData
import com.rarible.protocol.order.core.model.ZeroExMatchOrdersData
import com.rarible.protocol.order.core.model.ZeroExOrder
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.zero.ex.ZeroExOrderEventConverter
import com.rarible.protocol.order.listener.service.zero.ex.ZeroExOrderParser
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.domain.WordFactory
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import scalether.domain.Address
import scalether.domain.AddressFactory
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import scalether.java.Lists
import java.math.BigInteger
import java.time.Instant

class ZeroExExchangeOrderMatchDescriptorTest {
    private val contractsProvider = mockk<ContractsProvider> {
        every { zeroEx() } returns listOf(randomAddress())
    }
    private val zeroExOrderEventConverter = mockk<ZeroExOrderEventConverter>()
    private val zeroExOrderParser = mockk<ZeroExOrderParser>()
    private val autoReduceService = mockk<AutoReduceService>()

    private val zeroExExchangeOrderMatchDescriptor = ZeroExExchangeOrderMatchDescriptor(
        contractsProvider,
        zeroExOrderEventConverter,
        zeroExOrderParser,
        autoReduceService,
    )

    @BeforeEach
    fun before() {
        every {
            runBlocking {
                zeroExOrderParser.parseMatchOrdersData(any(), any(), any(), any(), any(), any())
            }
        } returns listOf(matchOrdersData)

        every {
            runBlocking {
                zeroExOrderEventConverter.convert(any(), any(), any(), any(), any(), any(), any(), any())
            }
        } returns listOf(orderSideMatchFromConverter)
    }

    @Test
    fun `convert buying by sell order`() = runBlocking<Unit> {
        // https://polygonscan.com/tx/0x08ef7447c15669631b1b54bf6b035df31c0b3b19720a760b2093db8b1eeb14cb
        val log = log(
            data = "0x0000000000000000000000000000000000000000000000000000000000000160" +
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
                "0000000000000000000000000000000000000000000000000000000000000000",
            topics = listOf(
                Word.apply("0x6869791f0a34781b29882982cc39e882768cf2c96995c2a110c577c53bc932d5"),
                Word.apply("0x0000000000000000000000004d3b39791d9bfe56304b32c35fe8f3d411d85a02"),
                Word.apply("0x000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d"),
                Word.apply("0x935b13465952ccbf981a264761b2edf0c0edf46f2e62a6a57ab37a32b224d6b0"),
            )
        )
        val event = FillEvent.apply(log)
        val fillEvent = FillEvent(
            log,
            Address.apply("0x4d3b39791d9bfe56304b32c35fe8f3d411d85a02"), // makerAddress
            Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"), // feeRecipientAddress
            Binary.apply("0x935b13465952ccbf981a264761b2edf0c0edf46f2e62a6a57ab37a32b224d6b0").bytes(), // orderHash
            Binary.apply("0xf47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619")
                .bytes(), // makerAssetData
            Binary.apply("0x025717920000000000000000000000002b4a66557a79263275826ad31a4cddc2789334bd000000000000000000000000000000000000000000000000000000000000762b")
                .bytes(), // takerAssetData
            Binary.apply("0x").bytes(), // makerFeeAssetData
            Binary.apply("0x").bytes(), // takerFeeAssetData
            Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"), // takerAddress
            Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"), // senderAddress
            1520000000000000.toBigInteger(), // makerAssetFilledAmount
            1.toBigInteger(), // takerAssetFilledAmount
            0.toBigInteger(), // makerFeePaid
            0.toBigInteger(), // takerFeePaid
            11700000000000000.toBigInteger() // protocolFeePaid
        )
        assertThat(FillEventSimple(event))
            .isEqualTo(FillEventSimple(fillEvent))

        val date = Instant.now().epochSecond
        val index = 1
        val totalLogs = 21
        val ethBlock = EthereumBlockchainBlock(
            number = 1,
            hash = randomWord(),
            parentHash = randomWord(),
            timestamp = date,
            ethBlock = mockk()
        )
        val ethLog = EthereumBlockchainLog(
            ethLog = log,
            ethTransaction = transaction,
            index = index,
            total = totalLogs,
        )
        val result = zeroExExchangeOrderMatchDescriptor.getEthereumEventRecords(ethBlock, ethLog).single()

        assertThat(result.asEthereumLogRecord().data as OrderSideMatch).isEqualTo(orderSideMatchFromConverter)
        coVerify {
            zeroExOrderParser.parseMatchOrdersData(
                txHash = transaction.hash(),
                txInput = transaction.input(),
                txFrom = transaction.from(),
                event = coMatch { actEvent ->
                    FillEventSimple(actEvent) == FillEventSimple(event)
                },
                index = index,
                totalLogs = totalLogs
            )
        }
        coVerify {
            zeroExOrderEventConverter.convert(
                matchOrdersData,
                from = transaction.from(),
                date = Instant.ofEpochSecond(date),
                orderHash = Word.apply(event.orderHash()),
                makerAddress = event.makerAddress(),
                takerAssetFilledAmount = event.takerAssetFilledAmount(),
                makerAssetFilledAmount = event.makerAssetFilledAmount(),
                input = transaction.input(),
            )
        }

        // it's the second log by these orders only for information. test for it is the same and is omitted
        val log2 = log(
            data = "0000000000000000000000000000000000000000000000000000000000000160" +
                "00000000000000000000000000000000000000000000000000000000000001e0" +
                "0000000000000000000000000000000000000000000000000000000000000240" +
                "00000000000000000000000000000000000000000000000000000000000002a0" +
                "000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d" +
                "000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "0000000000000000000000000000000000000000000000000005666e940f0000" +
                "0000000000000000000000000000000000000000000000000000accdd281e000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000029911687df4000" +
                "0000000000000000000000000000000000000000000000000000000000000044" +
                "025717920000000000000000000000002b4a66557a79263275826ad31a4cddc2" +
                "789334bd00000000000000000000000000000000000000000000000000000000" +
                "0000762b00000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000024" +
                "f47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cf" +
                "f1b9f61900000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000024" +
                "f47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cf" +
                "f1b9f61900000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000",
            topics = listOf(
                Word.apply("0x6869791f0a34781b29882982cc39e882768cf2c96995c2a110c577c53bc932d5"),
                Word.apply("0x000000000000000000000000dfa346c49c159c58d8316978b0f721ecebd10a3c"),
                Word.apply("0x000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d"),
                Word.apply("0xdcbcc2880a85e7b7e1b80c2fd714dcd6f275ee203298a52a0702b75691aec628"),
            )
        )
        val event2 = FillEvent.apply(log2)
        val fillEvent2 = FillEvent(
            log2,
            Address.apply("0xdfa346c49c159c58d8316978b0f721ecebd10a3c"), // makerAddress
            Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"), // feeRecipientAddress
            Binary.apply("0xdcbcc2880a85e7b7e1b80c2fd714dcd6f275ee203298a52a0702b75691aec628").bytes(), // orderHash
            Binary.apply("0x025717920000000000000000000000002b4a66557a79263275826ad31a4cddc2789334bd000000000000000000000000000000000000000000000000000000000000762b")
                .bytes(), // makerAssetData
            Binary.apply("0xf47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619")
                .bytes(), // takerAssetData
            Binary.apply("0xf47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619")
                .bytes(), // makerFeeAssetData
            Binary.apply("0x").bytes(), // takerFeeAssetData
            Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"), // takerAddress
            Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"), // senderAddress
            1.toBigInteger(), // makerAssetFilledAmount
            1520000000000000.toBigInteger(), // takerAssetFilledAmount
            190000000000000.toBigInteger(), // makerFeePaid
            0.toBigInteger(), // takerFeePaid
            11700000000000000.toBigInteger() // protocolFeePaid
        )
        assertThat(FillEventSimple(event2))
            .isEqualTo(FillEventSimple(fillEvent2))
    }

    @Test
    fun `convert selling by bid order`() = runBlocking<Unit> {
        // https://polygonscan.com/tx/0x41cdc1f41c866cd70b30efa5255044b29326856151709ad3cba99fa8229ee586
        val log = log(
            data = "0000000000000000000000000000000000000000000000000000000000000160" +
                "00000000000000000000000000000000000000000000000000000000000001c0" +
                "0000000000000000000000000000000000000000000000000000000000000340" +
                "0000000000000000000000000000000000000000000000000000000000000360" +
                "000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d" +
                "000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d" +
                "00000000000000000000000000000000000000000000000000071afd498d0000" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000203005a1aa4000" +
                "0000000000000000000000000000000000000000000000000000000000000024" +
                "f47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cf" +
                "f1b9f61900000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000144" +
                "a7cb5fb70000000000000000000000002953399124f0cbb46d2cbacd8a89cf05" +
                "9997496300000000000000000000000000000000000000000000000000000000" +
                "0000008000000000000000000000000000000000000000000000000000000000" +
                "000000c000000000000000000000000000000000000000000000000000000000" +
                "0000010000000000000000000000000000000000000000000000000000000000" +
                "00000001965f73921d8304e702fabc58e31f1fe07ea5e3680000000000000e00" +
                "0000000100000000000000000000000000000000000000000000000000000000" +
                "0000000100000000000000000000000000000000000000000000000000000000" +
                "0000000100000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000",
            topics = listOf(
                Word.apply("0x6869791f0a34781b29882982cc39e882768cf2c96995c2a110c577c53bc932d5"),
                Word.apply("0x000000000000000000000000bf228c81e042502adb2c3a8e281b826c61eea5ec"),
                Word.apply("0x000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d"),
                Word.apply("0xf207b7b54818d03ad29ac37d3d18f32c4cdf668d68a4855f1510e7e30b189413"),
            )
        )
        val event = FillEvent.apply(log)
        val fillEvent = FillEvent(
            log,
            Address.apply("0xbf228c81e042502adb2c3a8e281b826c61eea5ec"), // makerAddress
            Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"), // feeRecipientAddress
            Binary.apply("0xf207b7b54818d03ad29ac37d3d18f32c4cdf668d68a4855f1510e7e30b189413").bytes(), // orderHash
            Binary.apply("0xf47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619")
                .bytes(), // makerAssetData
            Binary.apply("0xa7cb5fb70000000000000000000000002953399124f0cbb46d2cbacd8a89cf0599974963000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000001965f73921d8304e702fabc58e31f1fe07ea5e3680000000000000e00000000010000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")
                .bytes(), // takerAssetData
            Binary.apply("0x").bytes(), // makerFeeAssetData
            Binary.apply("0x").bytes(), // takerFeeAssetData
            Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"), // takerAddress
            Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"), // senderAddress
            2000000000000000.toBigInteger(), // makerAssetFilledAmount
            1.toBigInteger(), // takerAssetFilledAmount
            0.toBigInteger(), // makerFeePaid
            0.toBigInteger(), // takerFeePaid
            9060000000000000.toBigInteger() // protocolFeePaid
        )
        assertThat(FillEventSimple(event))
            .isEqualTo(FillEventSimple(fillEvent))
        val date = Instant.now().epochSecond

        val block = EthereumBlockchainBlock(
            number = randomLong(),
            hash = randomString(),
            parentHash = randomString(),
            timestamp = date,
            ethBlock = mockk()
        )
        val index = 1
        val totalLogs = 21

        val ethLog = EthereumBlockchainLog(
            ethLog = log,
            ethTransaction = transaction,
            index = index,
            total = totalLogs,
        )
        val result = zeroExExchangeOrderMatchDescriptor.getEventRecords(
            log = ethLog,
            block = block
        ).single()
        assertThat(result.asEthereumLogRecord().data as OrderSideMatch).isEqualTo(orderSideMatchFromConverter)
        coVerify {
            zeroExOrderParser.parseMatchOrdersData(
                txHash = transaction.hash(),
                txInput = transaction.input(),
                txFrom = transaction.from(),
                event = coMatch { actEvent ->
                    FillEventSimple(actEvent) == FillEventSimple(event)
                },
                index = index,
                totalLogs = totalLogs
            )
        }
        coVerify {
            zeroExOrderEventConverter.convert(
                matchOrdersData,
                from = transaction.from(),
                date = Instant.ofEpochSecond(date),
                orderHash = Word.apply(event.orderHash()),
                makerAddress = event.makerAddress(),
                takerAssetFilledAmount = event.takerAssetFilledAmount(),
                makerAssetFilledAmount = event.makerAssetFilledAmount(),
                input = transaction.input(),
            )
        }

        // it's the second log by these orders only for information. test for it is the same and is omitted
        val log2 = log(
            data = "0000000000000000000000000000000000000000000000000000000000000160" +
                "00000000000000000000000000000000000000000000000000000000000002e0" +
                "0000000000000000000000000000000000000000000000000000000000000340" +
                "00000000000000000000000000000000000000000000000000000000000003a0" +
                "000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d" +
                "000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "00000000000000000000000000000000000000000000000000071afd498d0000" +
                "00000000000000000000000000000000000000000000000000005af3107a4000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000203005a1aa4000" +
                "0000000000000000000000000000000000000000000000000000000000000144" +
                "a7cb5fb70000000000000000000000002953399124f0cbb46d2cbacd8a89cf05" +
                "9997496300000000000000000000000000000000000000000000000000000000" +
                "0000008000000000000000000000000000000000000000000000000000000000" +
                "000000c000000000000000000000000000000000000000000000000000000000" +
                "0000010000000000000000000000000000000000000000000000000000000000" +
                "00000001965f73921d8304e702fabc58e31f1fe07ea5e3680000000000000e00" +
                "0000000100000000000000000000000000000000000000000000000000000000" +
                "0000000100000000000000000000000000000000000000000000000000000000" +
                "0000000100000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000024" +
                "f47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cf" +
                "f1b9f61900000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000024" +
                "f47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cf" +
                "f1b9f61900000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000",
            topics =
            listOf(
                Word.apply("0x6869791f0a34781b29882982cc39e882768cf2c96995c2a110c577c53bc932d5"),
                Word.apply("0x000000000000000000000000965f73921d8304e702fabc58e31f1fe07ea5e368"),
                Word.apply("0x000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d"),
                Word.apply("0xdaa270f057efc5f7fb883a5c135caa6643495c2a93cc8135073d2bd9fb063bfe"),
            )
        )
        val event2 = FillEvent.apply(log2)
        val fillEvent2 = FillEvent(
            log2,
            Address.apply("0x965f73921d8304e702fabc58e31f1fe07ea5e368"), // makerAddress
            Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"), // feeRecipientAddress
            Binary.apply("0xdaa270f057efc5f7fb883a5c135caa6643495c2a93cc8135073d2bd9fb063bfe").bytes(), // orderHash
            Binary.apply("0xa7cb5fb70000000000000000000000002953399124f0cbb46d2cbacd8a89cf0599974963000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000001965f73921d8304e702fabc58e31f1fe07ea5e3680000000000000e00000000010000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")
                .bytes(), // makerAssetData
            Binary.apply("0xf47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619")
                .bytes(), // takerAssetData
            Binary.apply("0xf47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619")
                .bytes(), // makerFeeAssetData
            Binary.apply("0x").bytes(), // takerFeeAssetData
            Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"), // takerAddress
            Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"), // senderAddress
            1.toBigInteger(), // makerAssetFilledAmount
            2000000000000000.toBigInteger(), // takerAssetFilledAmount
            100000000000000.toBigInteger(), // makerFeePaid
            0.toBigInteger(), // takerFeePaid
            9060000000000000.toBigInteger() // protocolFeePaid
        )
        assertThat(FillEventSimple(event2))
            .isEqualTo(FillEventSimple(fillEvent2))
    }

    @Test
    fun `convert buying 1155 by sell order`() = runBlocking<Unit> {
        // https://polygonscan.com/tx/0x7a91f7df871fa7718a4057684951d476db3fa1427604c335fd760bb3bc9ac49e
        val log = log(
            data = "0000000000000000000000000000000000000000000000000000000000000160" +
                "00000000000000000000000000000000000000000000000000000000000002e0" +
                "0000000000000000000000000000000000000000000000000000000000000340" +
                "00000000000000000000000000000000000000000000000000000000000003a0" +
                "000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d" +
                "000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d" +
                "0000000000000000000000000000000000000000000000001bc16d674ec80000" +
                "000000000000000000000000000000000000000000000000000b8d287f35dc46" +
                "000000000000000000000000000000000000000000000000000171a50fe6bb88" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000000000000000000000000000002aa1efb94e0000" +
                "0000000000000000000000000000000000000000000000000000000000000144" +
                "a7cb5fb700000000000000000000000022d5f9b75c524fec1d6619787e582644" +
                "cd4d742200000000000000000000000000000000000000000000000000000000" +
                "0000008000000000000000000000000000000000000000000000000000000000" +
                "000000c000000000000000000000000000000000000000000000000000000000" +
                "0000010000000000000000000000000000000000000000000000000000000000" +
                "0000000100000000000000000000000000000000000000000000000000000000" +
                "000000d100000000000000000000000000000000000000000000000000000000" +
                "0000000100000000000000000000000000000000000000000000000000000000" +
                "0000000100000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000024" +
                "f47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cf" +
                "f1b9f61900000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000024" +
                "f47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cf" +
                "f1b9f61900000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000",
            topics = listOf(
                Word.apply("0x6869791f0a34781b29882982cc39e882768cf2c96995c2a110c577c53bc932d5"),
                Word.apply("0x000000000000000000000000f10fb2fd902cbeb9bccef76cc9f4756eff76c92c"),
                Word.apply("0x000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d"),
                Word.apply("0xf3c8f29c6bc8c7ae4574de304b42f7e35a38848144c8429ff01ed749e13c6d77"),
            )
        )
        val event = FillEvent.apply(log)
        val fillEvent = FillEvent(
            log,
            Address.apply("0xf10fb2fd902cbeb9bccef76cc9f4756eff76c92c"), // makerAddress
            Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"), // feeRecipientAddress
            Binary.apply("0xf3c8f29c6bc8c7ae4574de304b42f7e35a38848144c8429ff01ed749e13c6d77").bytes(), // orderHash
            Binary.apply("0xa7cb5fb700000000000000000000000022d5f9b75c524fec1d6619787e582644cd4d7422000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000d10000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")
                .bytes(), // makerAssetData
            Binary.apply("0xf47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619")
                .bytes(), // takerAssetData
            Binary.apply("0xf47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619")
                .bytes(), // makerFeeAssetData
            Binary.apply("0x").bytes(), // takerFeeAssetData
            Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"), // takerAddress
            Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"), // senderAddress
            2000000000000000000.toBigInteger(), // makerAssetFilledAmount
            3251429816261702.toBigInteger(), // takerAssetFilledAmount
            406428727032712.toBigInteger(), // makerFeePaid
            0.toBigInteger(), // takerFeePaid
            12000000000000000.toBigInteger() // protocolFeePaid
        )
        assertThat(FillEventSimple(event))
            .isEqualTo(FillEventSimple(fillEvent))
        val date = Instant.now().epochSecond
        val index = 1
        val totalLogs = 21
        val ethBlock = EthereumBlockchainBlock(
            number = 1,
            hash = randomWord(),
            parentHash = randomWord(),
            timestamp = date,
            ethBlock = mockk()
        )
        val ethLog = EthereumBlockchainLog(
            ethLog = log,
            ethTransaction = transaction,
            index = index,
            total = totalLogs,
        )
        val result = zeroExExchangeOrderMatchDescriptor.getEthereumEventRecords(ethBlock, ethLog).single()
        assertThat(result.asEthereumLogRecord().data as OrderSideMatch).isEqualTo(orderSideMatchFromConverter)
        coVerify {
            zeroExOrderParser.parseMatchOrdersData(
                txHash = transaction.hash(),
                txInput = transaction.input(),
                txFrom = transaction.from(),
                event = coMatch { actEvent ->
                    FillEventSimple(actEvent) == FillEventSimple(event)
                },
                index = index,
                totalLogs = totalLogs
            )
        }
        coVerify {
            zeroExOrderEventConverter.convert(
                matchOrdersData,
                from = transaction.from(),
                date = Instant.ofEpochSecond(date),
                orderHash = Word.apply(event.orderHash()),
                makerAddress = event.makerAddress(),
                takerAssetFilledAmount = event.takerAssetFilledAmount(),
                makerAssetFilledAmount = event.makerAssetFilledAmount(),
                input = transaction.input(),
            )
        }

        // it's the second log by these orders only for information. test for it is the same and is omitted
        val log2 = log(
            data = "0000000000000000000000000000000000000000000000000000000000000160" +
                "00000000000000000000000000000000000000000000000000000000000001c0" +
                "0000000000000000000000000000000000000000000000000000000000000340" +
                "0000000000000000000000000000000000000000000000000000000000000360" +
                "000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d" +
                "000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d" +
                "000000000000000000000000000000000000000000000000000b8d287f35dc46" +
                "0000000000000000000000000000000000000000000000001bc16d674ec80000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000000000000000000000000000002aa1efb94e0000" +
                "0000000000000000000000000000000000000000000000000000000000000024" +
                "f47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cf" +
                "f1b9f61900000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000144" +
                "a7cb5fb700000000000000000000000022d5f9b75c524fec1d6619787e582644" +
                "cd4d742200000000000000000000000000000000000000000000000000000000" +
                "0000008000000000000000000000000000000000000000000000000000000000" +
                "000000c000000000000000000000000000000000000000000000000000000000" +
                "0000010000000000000000000000000000000000000000000000000000000000" +
                "0000000100000000000000000000000000000000000000000000000000000000" +
                "000000d100000000000000000000000000000000000000000000000000000000" +
                "0000000100000000000000000000000000000000000000000000000000000000" +
                "0000000100000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000",
            topics = listOf(
                Word.apply("0x6869791f0a34781b29882982cc39e882768cf2c96995c2a110c577c53bc932d5"),
                Word.apply("0x00000000000000000000000006737052e87392acad6b5a23c8ded8dd8e4db07d"),
                Word.apply("0x000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d"),
                Word.apply("0xef68293b01ac69bc07565eff24076efaa54d467933fed48f4a5108bf227274f4"),
            )
        )
        val event2 = FillEvent.apply(log2)
        val fillEvent2 = FillEvent(
            log2,
            Address.apply("0x06737052e87392acad6b5a23c8ded8dd8e4db07d"), // makerAddress
            Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"), // feeRecipientAddress
            Binary.apply("0xef68293b01ac69bc07565eff24076efaa54d467933fed48f4a5108bf227274f4").bytes(), // orderHash
            Binary.apply("0xf47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619")
                .bytes(), // makerAssetData
            Binary.apply("0xa7cb5fb700000000000000000000000022d5f9b75c524fec1d6619787e582644cd4d7422000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000d10000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")
                .bytes(), // takerAssetData
            Binary.apply("0x")
                .bytes(), // makerFeeAssetData
            Binary.apply("0x").bytes(), // takerFeeAssetData
            Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"), // takerAddress
            Address.apply("0xf715beb51ec8f63317d66f491e37e7bb048fcc2d"), // senderAddress
            3251429816261702.toBigInteger(), // makerAssetFilledAmount
            2000000000000000000.toBigInteger(), // takerAssetFilledAmount
            0.toBigInteger(), // makerFeePaid
            0.toBigInteger(), // takerFeePaid
            12000000000000000.toBigInteger() // protocolFeePaid
        )
        assertThat(FillEventSimple(event2))
            .isEqualTo(FillEventSimple(fillEvent2))
    }

    @Test
    fun `convert fill order via execute transaction`() = runBlocking {
        // https://polygonscan.com/tx/0x48a3a2fdccbbb61a9dc90f88969b9095d5b64aac11c504492bf105a00184558e
        val log = log(
            data = "0000000000000000000000000000000000000000000000000000000000000160" +
                "00000000000000000000000000000000000000000000000000000000000001e0" +
                "0000000000000000000000000000000000000000000000000000000000000240" +
                "0000000000000000000000000000000000000000000000000000000000000260" +
                "00000000000000000000000028e9e72dbf7adee19b5279c23e40a1b0b35c2b90" +
                "0000000000000000000000004e8c5dcd73df0448058e28b5205d1c63df7b30d9" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "0000000000000000000000000000000000000000000000000000000005f5e100" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000005543df729c000" +
                "0000000000000000000000000000000000000000000000000000000000000044" +
                "02571792000000000000000000000000f556faf23fc2feefa33ee6db2d1ee4c7" +
                "0e53451300000000000000000000000000000000000000000000000000000000" +
                "00005ddc00000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000024" +
                "f47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cf" +
                "f1b9f61900000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000",
            topics = listOf(
                Word.apply("0x6869791f0a34781b29882982cc39e882768cf2c96995c2a110c577c53bc932d5"),
                Word.apply("0x000000000000000000000000fd71dc9721d9ddcf0480a582927c3dcd42f3064c"),
                Word.apply("0x0000000000000000000000000000000000000000000000000000000000000000"),
                Word.apply("0xf28f67d3c870c5fe78b9dbe7c80b0a33b8e59a8da3eb0dd8ebc20c1cda2b9352"),
            )
        )
        val event = FillEvent.apply(log)
        val fillEvent = FillEvent(
            log,
            Address.apply("0xfd71dc9721d9ddcf0480a582927c3dcd42f3064c"), // makerAddress
            Address.apply("0x0000000000000000000000000000000000000000"), // feeRecipientAddress
            Binary.apply("0xf28f67d3c870c5fe78b9dbe7c80b0a33b8e59a8da3eb0dd8ebc20c1cda2b9352").bytes(), // orderHash
            Binary.apply("0x02571792000000000000000000000000f556faf23fc2feefa33ee6db2d1ee4c70e5345130000000000000000000000000000000000000000000000000000000000005ddc")
                .bytes(), // makerAssetData
            Binary.apply("0xf47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619")
                .bytes(), // takerAssetData
            Binary.apply("0x")
                .bytes(), // makerFeeAssetData
            Binary.apply("0x").bytes(), // takerFeeAssetData
            Address.apply("0x28e9e72dbf7adee19b5279c23e40a1b0b35c2b90"), // takerAddress
            Address.apply("0x4e8c5dcd73df0448058e28b5205d1c63df7b30d9"), // senderAddress
            1.toBigInteger(), // makerAssetFilledAmount
            100000000.toBigInteger(), // takerAssetFilledAmount
            0.toBigInteger(), // makerFeePaid
            0.toBigInteger(), // takerFeePaid
            1500000000000000.toBigInteger() // protocolFeePaid
        )
        assertThat(FillEventSimple(event))
            .isEqualTo(FillEventSimple(fillEvent))

        val date = Instant.now().epochSecond
        val index = 1
        val totalLogs = 21
        val ethBlock = EthereumBlockchainBlock(
            number = 1,
            hash = randomWord(),
            parentHash = randomWord(),
            timestamp = date,
            ethBlock = mockk()
        )
        val ethLog = EthereumBlockchainLog(
            ethLog = log,
            ethTransaction = transaction,
            index = index,
            total = totalLogs,
        )
        val result = zeroExExchangeOrderMatchDescriptor.getEthereumEventRecords(ethBlock, ethLog).single().asEthereumLogRecord()
        assertThat(result.data).isEqualTo(orderSideMatchFromConverter)
        coVerify {
            zeroExOrderParser.parseMatchOrdersData(
                txHash = transaction.hash(),
                txInput = transaction.input(),
                txFrom = transaction.from(),
                event = coMatch { actEvent ->
                    FillEventSimple(actEvent) == FillEventSimple(event)
                },
                index = index,
                totalLogs = totalLogs
            )
        }
        coVerify {
            zeroExOrderEventConverter.convert(
                matchOrdersData,
                from = transaction.from(),
                date = Instant.ofEpochSecond(date),
                orderHash = Word.apply(event.orderHash()),
                makerAddress = event.makerAddress(),
                takerAssetFilledAmount = event.takerAssetFilledAmount(),
                makerAssetFilledAmount = event.makerAssetFilledAmount(),
                input = transaction.input(),
            )
        }
    }

    private fun log(data: String, topics: List<Word>) = Log(
        BigInteger.ONE, // logIndex
        BigInteger.TEN, // transactionIndex
        Word.apply(ByteArray(32)), // transactionHash
        Word.apply(ByteArray(32)), // blockHash
        BigInteger.ZERO, // blockNumber
        Address.ZERO(), // address
        Binary.apply( // data
            data
        ),
        false, // removed
        Lists.toScala( // topics
            topics
        ),
        "" // type
    )

    private companion object {
        val matchOrdersData = ZeroExMatchOrdersData(
            leftOrder = ZeroExOrder(
                makerAddress = AddressFactory.create(),
                takerAddress = AddressFactory.create(),
                feeRecipientAddress = AddressFactory.create(),
                senderAddress = AddressFactory.create(),
                makerAssetAmount = 11.toBigInteger(),
                takerAssetAmount = 12.toBigInteger(),
                makerFee = 13.toBigInteger(),
                takerFee = 14.toBigInteger(),
                expirationTimeSeconds = 15.toBigInteger(),
                salt = 16.toBigInteger(),
                makerAssetData = Binary.apply("0x11"),
                takerAssetData = Binary.apply("0x12"),
                makerFeeAssetData = Binary.apply("0x13"),
                takerFeeAssetData = Binary.apply("0x14"),
            ),
            takerAddress = AddressFactory.create(),
            rightOrder = ZeroExOrder(
                makerAddress = AddressFactory.create(),
                takerAddress = AddressFactory.create(),
                feeRecipientAddress = AddressFactory.create(),
                senderAddress = AddressFactory.create(),
                makerAssetAmount = 21.toBigInteger(),
                takerAssetAmount = 22.toBigInteger(),
                makerFee = 23.toBigInteger(),
                takerFee = 24.toBigInteger(),
                expirationTimeSeconds = 25.toBigInteger(),
                salt = 26.toBigInteger(),
                makerAssetData = Binary.apply("0x21"),
                takerAssetData = Binary.apply("0x22"),
                makerFeeAssetData = Binary.apply("0x23"),
                takerFeeAssetData = Binary.apply("0x24"),
            ),
            leftSignature = Binary.apply("0x1111"),
            rightSignature = Binary.apply("0x2222"),
            feeData = listOf(
                ZeroExFeeData(
                    recipient = AddressFactory.create(),
                    paymentTokenAmount = 100.toBigInteger(),
                )
            ),
            paymentTokenAddress = AddressFactory.create(),
        )
        val orderSideMatchFromConverter = OrderSideMatch(
            hash = WordFactory.create(),
            maker = AddressFactory.create(),
            taker = AddressFactory.create(),
            make = randomErc20(EthUInt256.TEN),
            take = randomErc20(EthUInt256.of(5)),
            fill = EthUInt256.ZERO,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            side = OrderSide.LEFT,
            makeUsd = null,
            takeUsd = null,
            makePriceUsd = null,
            takePriceUsd = null,
            makeValue = null,
            takeValue = null
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
                "0xbbbfa60c00000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000000000000003e0000000000000000000000000000000000000000000000000000000000000072000000000000000000000000000000000000000000000000000000000000007a000000000000000000000000000000000000000000000000000000000000008200000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f6190000000000000000000000004d3b39791d9bfe56304b32c35fe8f3d411d85a020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d0000000000000000000000000000000000000000000000000005666e940f000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000625935e7000000000000000000000000000000000000000000000000015909b08de1da0100000000000000000000000000000000000000000000000000000000000001c0000000000000000000000000000000000000000000000000000000000000022000000000000000000000000000000000000000000000000000000000000002a000000000000000000000000000000000000000000000000000000000000002e00000000000000000000000000000000000000000000000000000000000000024f47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000044025717920000000000000000000000002b4a66557a79263275826ad31a4cddc2789334bd000000000000000000000000000000000000000000000000000000000000762b000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000dfa346c49c159c58d8316978b0f721ecebd10a3c0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d000000000000000000000000f715beb51ec8f63317d66f491e37e7bb048fcc2d00000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000005666e940f00000000000000000000000000000000000000000000000000000000accdd281e000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000625935e700000000000000000000000000000000000000000000000000fef66f825d18ca00000000000000000000000000000000000000000000000000000000000001c0000000000000000000000000000000000000000000000000000000000000024000000000000000000000000000000000000000000000000000000000000002a000000000000000000000000000000000000000000000000000000000000003000000000000000000000000000000000000000000000000000000000000000044025717920000000000000000000000002b4a66557a79263275826ad31a4cddc2789334bd000000000000000000000000000000000000000000000000000000000000762b000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000024f47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000024f47261b00000000000000000000000007ceb23fd6bc0add59e62ac25578270cff1b9f619000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000421c7763a38bdc4a8cc3bfdab4302a54c5c76f0aafb5ff54fafb4a809d34d84277dd3a940201f11fd8bf824c2732af189ef6f922ef351157ea0d3a3c13e6c4543db30300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000421c40e0c8008601b2df04d165574e899598e63caa7f7a68f2c2ba274bd088c56e8162513d0f1a69a31770981117d8533133295cef7fa4ec5978376caa8e7ff8c0d10300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000020000000000000000000000005b3256965e7c3cf26e11fcaf296dfc8807c010730000000000000000000000000000000000000000000000000000228f908060000000000000000000000000000bbdd174198c3bafff09f58d62119e680141ab4400000000000000000000000000000000000000000000000000008a3e42018000"
            )
        )
    }

    private data class FillEventSimple(
        val log: Log,
        val makerAddress: Address,
        val feeRecipientAddress: Address,
        val orderHash: Binary,
        val makerAssetData: Binary,
        val takerAssetData: Binary,
        val makerFeeAssetData: Binary,
        val takerFeeAssetData: Binary,
        val takerAddress: Address,
        val senderAddress: Address,
        val makerAssetFilledAmount: BigInteger,
        val takerAssetFilledAmount: BigInteger,
        val makerFeePaid: BigInteger,
        val takerFeePaid: BigInteger,
        val protocolFeePaid: BigInteger
    ) {
        constructor(event: FillEvent) : this(
            log = event.log(),
            makerAddress = event.makerAddress(),
            feeRecipientAddress = event.feeRecipientAddress(),
            orderHash = Binary.apply(event.orderHash()),
            makerAssetData = Binary.apply(event.makerAssetData()),
            takerAssetData = Binary.apply(event.takerAssetData()),
            makerFeeAssetData = Binary.apply(event.makerFeeAssetData()),
            takerFeeAssetData = Binary.apply(event.takerFeeAssetData()),
            takerAddress = event.takerAddress(),
            senderAddress = event.senderAddress(),
            makerAssetFilledAmount = event.makerAssetFilledAmount(),
            takerAssetFilledAmount = event.takerAssetFilledAmount(),
            makerFeePaid = event.makerFeePaid(),
            takerFeePaid = event.takerFeePaid(),
            protocolFeePaid = event.protocolFeePaid(),
        )
    }
}
