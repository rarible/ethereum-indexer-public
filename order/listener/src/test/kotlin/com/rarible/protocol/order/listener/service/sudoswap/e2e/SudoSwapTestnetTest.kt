package com.rarible.protocol.order.listener.service.sudoswap.e2e

import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.OrderSudoSwapAmmDataV1Dto
import com.rarible.protocol.dto.SudoSwapCurveTypeDto
import com.rarible.protocol.dto.SudoSwapPoolTypeDto
import com.rarible.protocol.order.core.model.SudoSwapPoolType
import com.rarible.protocol.order.core.service.curve.PoolCurve.Companion.eth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration

@Disabled("Manual tests for SudoSwap on dev")
class SudoSwapTestnetTest : AbstractSudoSwapTestnetTest() {
    @Test
    fun `should create nft pool with liner curve`() = runBlocking<Unit> {
        val token = createToken(userSender, poller)
        val tokenIds = mintAndApprove(5, userSender, token, sudoswapPairFactory)

        val delta = BigDecimal("0.2").eth()
        val fee = BigInteger.ZERO
        val spotPrice = BigDecimal("0.500000000000000000")

        val (poolAddress, orderHash) = createPool(
            sender = userSender,
            nft = token.address(),
            bondingCurve = sudoswapLinerCurve,
            assetRecipient = userSender.from(),
            poolType = SudoSwapPoolType.NFT,
            delta = delta,
            fee = fee,
            spotPrice = spotPrice.multiply(decimal).toBigInteger(),
            tokenIds = tokenIds
        )
        val expectedPrice = getSingleBuyNFTQuote(userSender, poolAddress) // spotPrice + delta + protocolFee (0.5%)

        checkOrder(orderHash) {
            assertThat(it.make.value).isEqualTo(BigInteger.ONE)
            assertThat(it.take.value).isEqualTo(expectedPrice.inputValue)
            assertThat(it.makeStock).isEqualTo(tokenIds.size.toBigInteger())
            assertThat(it.makePrice?.stripTrailingZeros()).isEqualTo(expectedPrice.price())
            assertThat(it.status).isEqualTo(OrderStatusDto.ACTIVE)

            with(it.data as OrderSudoSwapAmmDataV1Dto) {
                assertThat(this.poolAddress).isEqualTo(poolAddress)
                assertThat(this.poolType).isEqualTo(SudoSwapPoolTypeDto.NFT)
                assertThat(this.bondingCurve).isEqualTo(sudoswapLinerCurve)
                assertThat(this.curveType).isEqualTo(SudoSwapCurveTypeDto.LINEAR)
                assertThat(this.delta).isEqualTo(delta)
                assertThat(this.fee).isEqualTo(fee)
            }
        }
        checkHoldItems(orderHash, token.address(), tokenIds)
    }

    @Test
    fun `should create trade pool with exponential curve`() = runBlocking<Unit> {
        val token = createToken(userSender, poller)
        val tokenIds = mintAndApprove(5, userSender, token, sudoswapPairFactory)

        val delta = BigDecimal("1.01").eth()
        val fee = BigInteger.ZERO
        val spotPrice = BigDecimal("0.500000000000000000")

        val (poolAddress, orderHash) = createPool(
            sender = userSender,
            nft = token.address(),
            bondingCurve = sudoswapExponentialCurve,
            assetRecipient = Address.ZERO(),
            poolType = SudoSwapPoolType.TRADE,
            delta = delta,
            fee = fee,
            spotPrice = spotPrice.multiply(decimal).toBigInteger(),
            tokenIds = tokenIds
        )
        val expectedPrice = getSingleBuyNFTQuote(userSender, poolAddress)

        checkOrder(orderHash) {
            assertThat(it.make.value).isEqualTo(BigInteger.ONE)
            assertThat(it.take.value).isEqualTo(expectedPrice.inputValue)
            assertThat(it.makeStock).isEqualTo(tokenIds.size.toBigInteger())
            assertThat(it.makePrice?.stripTrailingZeros()).isEqualTo(expectedPrice.price())
            assertThat(it.status).isEqualTo(OrderStatusDto.ACTIVE)

            with(it.data as OrderSudoSwapAmmDataV1Dto) {
                assertThat(this.poolAddress).isEqualTo(poolAddress)
                assertThat(this.poolType).isEqualTo(SudoSwapPoolTypeDto.TRADE)
                assertThat(this.bondingCurve).isEqualTo(sudoswapExponentialCurve)
                assertThat(this.curveType).isEqualTo(SudoSwapCurveTypeDto.EXPONENTIAL)
                assertThat(this.delta).isEqualTo(delta)
                assertThat(this.fee).isEqualTo(fee)
            }
        }
        checkHoldItems(orderHash, token.address(), tokenIds)
    }

    @Test
    fun `should buy nft by pool`() = runBlocking<Unit> {
        val token = createToken(userSender, poller)
        val tokenIds = mintAndApprove(5, userSender, token, sudoswapPairFactory)

        val delta = BigDecimal("0.1").eth()
        val fee = BigDecimal("0.001").eth()
        val spotPrice = BigDecimal("0.500000000000000000")
        val (poolAddress, orderHash) = createPool(
            sender = userSender,
            nft = token.address(),
            bondingCurve = sudoswapLinerCurve,
            assetRecipient = Address.ZERO(),
            poolType = SudoSwapPoolType.TRADE,
            delta = delta,
            fee = fee,
            spotPrice = spotPrice.eth(),
            tokenIds = tokenIds,
            value = BigInteger("5").eth()
        )
        checkOrder(orderHash) {
            assertThat(it.make.value).isEqualTo(BigInteger.ONE)
            assertThat(it.makeStock).isEqualTo(tokenIds.size)
        }
        val buyTokenIds = mintAndApprove(5, userSender, token, poolAddress)
        swapNFTsForToken(
            sender = userSender,
            poolAddress = poolAddress,
            tokenIds = buyTokenIds,
            minExpectedTokenOutput = BigDecimal("0.01").eth(),
            tokenRecipient = userSender.from(),
        )
        val expectedPrice = getSingleBuyNFTQuote(userSender, poolAddress)
        checkOrder(orderHash) {
            assertThat(it.make.value).isEqualTo(BigInteger.ONE)
            assertThat(it.makeStock).isEqualTo((tokenIds + buyTokenIds).size)
            assertThat(it.take.value).isEqualTo(expectedPrice.inputValue)
            assertThat(it.makePrice?.stripTrailingZeros()).isEqualTo(expectedPrice.price())
        }
        checkHoldItems(orderHash, token.address(), tokenIds + buyTokenIds)
    }

    @Test
    fun `should deposit nft to pool`() = runBlocking<Unit> {
        val token = createToken(userSender, poller)
        val tokenIds = mintAndApprove(5, userSender, token, sudoswapPairFactory)
        val (poolAddress, orderHash) = createPool(userSender, token.address())

        checkOrder(orderHash) {
            assertThat(it.make.value).isEqualTo(BigInteger.ONE)
            assertThat(it.makeStock).isEqualTo(BigInteger.ZERO)
            assertThat(it.status).isEqualTo(OrderStatusDto.INACTIVE)
        }
        checkHoldItems(orderHash, token.address(), emptyList())

        depositNFTs(
            userSender,
            poolAddress,
            token.address(),
            tokenIds,
        )
        checkOrder(orderHash) {
            assertThat(it.make.value).isEqualTo(BigInteger.ONE)
            assertThat(it.makeStock).isEqualTo(tokenIds.size)
            assertThat(it.status).isEqualTo(OrderStatusDto.ACTIVE)
        }
        checkHoldItems(orderHash, token.address(), tokenIds)
    }

    @Test
    fun `should not calculate deposit for not pool collection`() = runBlocking<Unit> {
        val token = createToken(userSender, poller)
        val wrongToken = createToken(userSender, poller)
        val tokenIds = mintAndApprove(5, userSender, token, sudoswapPairFactory)
        val wrongTokenIds = mintAndApprove(5, userSender, wrongToken, sudoswapPairFactory)
        val (poolAddress, orderHash) = createPool(userSender, token.address(), tokenIds = tokenIds)

        checkOrder(orderHash) {
            assertThat(it.make.value).isEqualTo(BigInteger.ONE)
            assertThat(it.makeStock).isEqualTo(tokenIds.size)
        }
        depositNFTs(
            userSender,
            poolAddress,
            wrongToken.address(),
            wrongTokenIds,
        )
        delay(Duration.ofSeconds(5))
        checkOrder(orderHash) {
            assertThat(it.make.value).isEqualTo(BigInteger.ONE)
            assertThat(it.makeStock).isEqualTo(tokenIds.size)
        }
    }

    @Test
    fun `should swap token to any nft`() = runBlocking<Unit> {
        val token = createToken(userSender, poller)
        val tokenIds = mintAndApprove(5, userSender, token, sudoswapPairFactory)
        val (poolAddress, orderHash) = createPool(
            userSender,
            token.address(),
            spotPrice = BigDecimal("0.01").multiply(decimal).toBigInteger(),
            delta = BigDecimal("0.001").multiply(decimal).toBigInteger(),
            tokenIds = tokenIds)

        checkOrder(orderHash) {
            assertThat(it.makeStock).isEqualTo(5)
        }
        swapTokenForAnyNFTs(
            sender = userSender,
            poolAddress = poolAddress,
            nftCount = 3,
            value = BigDecimal("1").multiply(decimal).toBigInteger()
        )
        checkOrder(orderHash) {
            assertThat(it.make.value).isEqualTo(BigInteger.ONE)
            assertThat(it.makeStock).isEqualTo(2)
        }
    }

    @Test
    fun `should swap token to specific nft`() = runBlocking<Unit> {
        val token = createToken(userSender, poller)
        val tokenIds1 = mintAndApprove(2, userSender, token, sudoswapPairFactory)
        val tokenIds2 = mintAndApprove(3, userSender, token, sudoswapPairFactory)
        val (poolAddress, orderHash) = createPool(
            userSender,
            token.address(),
            spotPrice = BigDecimal("0.001").multiply(decimal).toBigInteger(),
            delta = BigDecimal("0.001").multiply(decimal).toBigInteger(),
            tokenIds = tokenIds1 + tokenIds2)

        checkOrder(orderHash) {
            assertThat(it.makeStock).isEqualTo(5)
        }
        swapTokenForSpecificNFTs(
            sender = userSender,
            poolAddress = poolAddress,
            tokenIds = tokenIds1,
            value = BigDecimal("1").multiply(decimal).toBigInteger()
        )
        checkOrder(orderHash) {
            assertThat(it.make.value).isEqualTo(BigInteger.ONE)
            assertThat(it.makeStock).isEqualTo(3)
        }
        checkHoldItems(orderHash, token.address(), tokenIds2)

        checkItemAmmOrderExist(orderHash, token.address(), tokenIds2)
        checkItemAmmOrderNotExist(token.address(), tokenIds1)
    }

    @Test
    fun `should withdraw nft from pool`() = runBlocking<Unit> {
        val token = createToken(userSender, poller)
        val tokenIds1 = mintAndApprove(2, userSender, token, sudoswapPairFactory)
        val tokenIds2 = mintAndApprove(3, userSender, token, sudoswapPairFactory)
        val (poolAddress, orderHash) = createPool(
            userSender,
            token.address(),
            tokenIds = tokenIds1 + tokenIds2
        )
        checkOrder(orderHash) {
            assertThat(it.makeStock).isEqualTo(5)
        }
        withdrawERC721(
            sender = userSender,
            poolAddress = poolAddress,
            token = token.address(),
            tokenIds = tokenIds1
        )
        checkOrder(orderHash) {
            assertThat(it.make.value).isEqualTo(BigInteger.ONE)
            assertThat(it.makeStock).isEqualTo(3)
        }
        checkHoldItems(orderHash, token.address(), tokenIds2)

        checkItemAmmOrderExist(orderHash, token.address(), tokenIds2)
        checkItemAmmOrderNotExist(token.address(), tokenIds1)
    }

    @Test
    fun `should change pool delta`() = runBlocking<Unit> {
        val token = createToken(userSender, poller)
        val delta = BigDecimal("0.2").multiply(decimal).toBigInteger()
        val (poolAddress, orderHash) = createPool(
            userSender,
            token.address(),
            delta = delta
        )
        checkOrder(orderHash) {
            val data = it.data as OrderSudoSwapAmmDataV1Dto
            assertThat(data.delta).isEqualTo(delta)
        }
        val newDelta = BigDecimal("0.5").multiply(decimal).toBigInteger()
        changeDelta(
            sender = userSender,
            poolAddress = poolAddress,
            newDelta = newDelta,
        )
        checkOrder(orderHash) {
            val data = it.data as OrderSudoSwapAmmDataV1Dto
            assertThat(data.delta).isEqualTo(newDelta)
        }
    }

    @Test
    fun `should change pool fee`() = runBlocking<Unit> {
        val token = createToken(userSender, poller)
        val tokenIds = mintAndApprove(1, userSender, token, sudoswapPairFactory)
        val delta = BigDecimal("1.01").eth()
        val fee = BigDecimal("0.0002").eth()
        val spotPrice = BigDecimal("0.500000000000000000")
        val (poolAddress, orderHash) = createPool(
            sender = userSender,
            nft = token.address(),
            bondingCurve = sudoswapExponentialCurve,
            assetRecipient = Address.ZERO(),
            poolType = SudoSwapPoolType.TRADE,
            delta = delta,
            fee = fee,
            spotPrice = spotPrice.multiply(decimal).toBigInteger(),
            tokenIds = tokenIds
        )
        checkOrder(orderHash) {
            val data = it.data as OrderSudoSwapAmmDataV1Dto
            assertThat(data.fee).isEqualTo(fee)
        }
        val newFee = BigDecimal("0.0005").eth()
        changeFee(
            sender = userSender,
            poolAddress = poolAddress,
            newFee = newFee,
        )
        checkOrder(orderHash) {
            val data = it.data as OrderSudoSwapAmmDataV1Dto
            assertThat(data.fee).isEqualTo(newFee)
        }
    }
}
