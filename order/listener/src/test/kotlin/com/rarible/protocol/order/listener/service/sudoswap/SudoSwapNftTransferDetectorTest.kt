package com.rarible.protocol.order.listener.service.sudoswap

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.listener.data.log
import io.daonomic.rpc.domain.Word
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum
import scalether.domain.Address
import java.math.BigInteger

@Suppress("ReactiveStreamsUnusedPublisher")
internal class SudoSwapNftTransferDetectorTest {
    private val ethereum = mockk<MonoEthereum>()
    private val featureFlags = mockk<OrderIndexerProperties.FeatureFlags>()
    private val detector = SudoSwapNftTransferDetector(ethereum, featureFlags)

    @BeforeEach
    fun setup() {
        every { featureFlags.searchSudoSwapErc1155Transfer } returns false
    }

    @Test
    fun `should detect erc721 nft transfers`() = runBlocking<Unit> {
        val transactionHash = Word.apply(randomWord())
        val poolAddress = Address.apply("0x3474606e53eae51f6a4f787e8c8d33999c6eae61")
        val collectionAddress = randomAddress()

        val unneededTransfer = log(
            topics = listOf(
                Word.apply("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"),
                Word.apply("0x0000000000000000000000003474606e53eae51f6a4f787e8c8d33999c6eae61"),
                Word.apply("0x0000000000000000000000007f4b31f1578bb1d49d420379b7db20d08a5c8935"),
                Word.apply("0x000000000000000000000000000000000000000000000000000000000000000f"),
            ),
            transactionHash = transactionHash,
            address = collectionAddress
        )
        val priceUpdate = log(
            topics = listOf(Word.apply("0xf06180fdbe95e5193df4dcd1352726b1f04cb58599ce58552cc952447af2ffbb")),
            data = "000000000000000000000000000000000000000000000000002386f26fc10000",
            transactionHash = transactionHash,
            address = poolAddress
        )
        val wrongTransfer = log(
            topics = listOf(
                Word.apply("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"),
                Word.apply("0x0000000000000000000000007f4b31f1578bb1d49d420379b7db20d08a5c8935"),
                Word.apply("0x0000000000000000000000007f4b31f1578bb1d49d420379b7db20d08a5c8935"),
                Word.apply("0x0000000000000000000000000000000000000000000000000000000000000001"),
            ),
            transactionHash = transactionHash,
            address = collectionAddress
        )
        val transfer0 = log(
            topics = listOf(
                Word.apply("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"),
                Word.apply("0x0000000000000000000000003474606e53eae51f6a4f787e8c8d33999c6eae61"),
                Word.apply("0x0000000000000000000000007f4b31f1578bb1d49d420379b7db20d08a5c8935"),
                Word.apply("0x0000000000000000000000000000000000000000000000000000000000000001"),
            ),
            transactionHash = transactionHash,
            address = collectionAddress
        )
        val transfer1 = log(
            topics = listOf(
                Word.apply("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"),
                Word.apply("0x0000000000000000000000003474606e53eae51f6a4f787e8c8d33999c6eae61"),
                Word.apply("0x0000000000000000000000007f4b31f1578bb1d49d420379b7db20d08a5c8935"),
                Word.apply("0x0000000000000000000000000000000000000000000000000000000000000002"),
            ),
            transactionHash = transactionHash,
            address = collectionAddress
        )
        val outNft = log(
            topics = listOf(Word.apply("0xbc479dfc6cb9c1a9d880f987ee4b30fa43dd7f06aec121db685b67d587c93c93")),
            logIndex = 1,
            transactionHash = transactionHash,
            address = poolAddress
        )
        every { ethereum.ethGetLogsJava(any()) } returns Mono.just(listOf(unneededTransfer, priceUpdate, wrongTransfer, transfer0, transfer1, outNft))
        val tokenIds = detector.detectNftTransfers(outNft, collectionAddress)
        assertThat(tokenIds).containsExactly(BigInteger.valueOf(2), BigInteger.valueOf(1))
    }

    @Test
    fun `should detect erc1155 nft transfers`() = runBlocking<Unit> {
        every { featureFlags.searchSudoSwapErc1155Transfer } returns true
        val transactionHash = Word.apply(randomWord())
        val poolAddress = Address.apply("0x3474606e53eae51f6a4f787e8c8d33999c6eae61")
        val collectionAddress = randomAddress()

        val unneededTransfer = log(
            topics = listOf(
                Word.apply("0xc3d58168c5ae7397731d063d5bbf3d657854427343f4c083240f7aacaa2d0f62"),
                Word.apply("0x0000000000000000000000003474606e53eae51f6a4f787e8c8d33999c6eae61"),
                Word.apply("0x0000000000000000000000007f4b31f1578bb1d49d420379b7db20d08a5c8935"),
                Word.apply("0x000000000000000000000000000000000000000000000000000000000000000f"),
            ),
            data = "0x00000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000001",
            transactionHash = transactionHash,
            address = collectionAddress
        )
        val priceUpdate = log(
            topics = listOf(Word.apply("0xf06180fdbe95e5193df4dcd1352726b1f04cb58599ce58552cc952447af2ffbb")),
            data = "000000000000000000000000000000000000000000000000000c85ddae39f3d7",
            transactionHash = transactionHash,
            address = poolAddress
        )
        val wrongTransfer = log(
            topics = listOf(
                Word.apply("0xc3d58168c5ae7397731d063d5bbf3d657854427343f4c083240f7aacaa2d0f62"),
                Word.apply("0x0000000000000000000000007f4b31f1578bb1d49d420379b7db20d08a5c8935"),
                Word.apply("0x0000000000000000000000007f4b31f1578bb1d49d420379b7db20d08a5c8935"),
                Word.apply("0x0000000000000000000000000000000000000000000000000000000000000001"),
            ),
            data = "0x00000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000001",
            transactionHash = transactionHash,
            address = collectionAddress
        )
        val transfer = log(
            topics = listOf(
                Word.apply("0xc3d58168c5ae7397731d063d5bbf3d657854427343f4c083240f7aacaa2d0f62"),
                Word.apply("0x0000000000000000000000005f7e5e3b18999f1b24e8d367c64d7c94d0435e26"),
                Word.apply("0x0000000000000000000000003474606e53eae51f6a4f787e8c8d33999c6eae61"),
                Word.apply("0x0000000000000000000000005f7e5e3b18999f1b24e8d367c64d7c94d0435e26"),
            ),
            data = "0x00000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000001",
            transactionHash = transactionHash,
            address = collectionAddress
        )
        val outNft = log(
            topics = listOf(Word.apply("0xbc479dfc6cb9c1a9d880f987ee4b30fa43dd7f06aec121db685b67d587c93c93")),
            logIndex = 1,
            transactionHash = transactionHash,
            address = poolAddress
        )
        every { ethereum.ethGetLogsJava(any()) } returns Mono.just(listOf(unneededTransfer, priceUpdate, wrongTransfer, transfer, outNft))
        val tokenIds = detector.detectNftTransfers(outNft, collectionAddress)
        assertThat(tokenIds).containsExactly(BigInteger.valueOf(1))
    }

    @Test
    fun `should detect correct SpotPriceUpdate`() = runBlocking<Unit> {
        val transactionHash = Word.apply(randomWord())
        val poolAddress = Address.apply("0x3474606e53eae51f6a4f787e8c8d33999c6eae61")
        val collectionAddress = randomAddress()

        val priceUpdate = log(
            topics = listOf(Word.apply("0xf06180fdbe95e5193df4dcd1352726b1f04cb58599ce58552cc952447af2ffbb")),
            data = "000000000000000000000000000000000000000000000000002386f26fc10000",
            transactionHash = transactionHash,
            address = poolAddress
        )
        val transfer1 = log(
            topics = listOf(
                Word.apply("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"),
                Word.apply("0x0000000000000000000000003474606e53eae51f6a4f787e8c8d33999c6eae61"),
                Word.apply("0x0000000000000000000000007f4b31f1578bb1d49d420379b7db20d08a5c8935"),
                Word.apply("0x0000000000000000000000000000000000000000000000000000000000000002"),
            ),
            transactionHash = transactionHash,
            address = collectionAddress
        )
        val outNft = log(
            topics = listOf(Word.apply("0xbc479dfc6cb9c1a9d880f987ee4b30fa43dd7f06aec121db685b67d587c93c93")),
            logIndex = 1,
            transactionHash = transactionHash,
            address = poolAddress
        )
        val otherPriceUpdate = log(
            topics = listOf(Word.apply("0xf06180fdbe95e5193df4dcd1352726b1f04cb58599ce58552cc952447af2ffbb")),
            data = "000000000000000000000000000000000000000000000000002386f26fc10000",
            transactionHash = transactionHash,
            address = poolAddress
        )
        every { ethereum.ethGetLogsJava(any()) } returns Mono.just(listOf(priceUpdate, transfer1, outNft, otherPriceUpdate))
        val tokenIds = detector.detectNftTransfers(outNft, collectionAddress)
        assertThat(tokenIds).containsExactly(BigInteger.valueOf(2))
    }
}
