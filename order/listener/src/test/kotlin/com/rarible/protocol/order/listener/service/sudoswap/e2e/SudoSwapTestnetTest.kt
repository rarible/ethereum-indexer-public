package com.rarible.protocol.order.listener.service.sudoswap.e2e

import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.OrderSudoSwapAmmDataV1Dto
import com.rarible.protocol.dto.SudoSwapCurveTypeDto
import com.rarible.protocol.dto.SudoSwapPoolTypeDto
import com.rarible.protocol.order.core.model.SudoSwapPoolType
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

@Disabled("Manual tests for SudoSwap on dev")
class SudoSwapTestnetTest : AbstractSudoSwapTestnetTest() {
    @Test
    fun `should create nft pool with liner curve`() = runBlocking<Unit> {
        val token = createToken(userSender, poller)
        val tokenIds = mintAndApprove(5, userSender, token, sudoswapPairFactory)

        val delta = BigDecimal.valueOf(0.2).multiply(decimal).toBigInteger()
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
        checkOrder(orderHash) {
            assertThat(it.make.value).isEqualTo(tokenIds.size)
            assertThat(it.makeStock).isEqualTo(tokenIds.size.toBigInteger())
            assertThat(it.makePrice).isEqualTo(spotPrice)
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
    fun `should deposit nft to pool`() = runBlocking<Unit> {
        val token = createToken(userSender, poller)
        val tokenIds = mintAndApprove(5, userSender, token, sudoswapPairFactory)
        val (poolAddress, orderHash) = createPool(userSender, token.address())

        checkOrder(orderHash) {
            assertThat(it.make.value).isEqualTo(BigInteger.ZERO)
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
            assertThat(it.make.value).isEqualTo(tokenIds.size)
            assertThat(it.makeStock).isEqualTo(tokenIds.size)
            assertThat(it.status).isEqualTo(OrderStatusDto.ACTIVE)
        }
        checkHoldItems(orderHash, token.address(), tokenIds)
    }

    @Test
    fun `should swap token to any nft`() = runBlocking<Unit> {
        val token = createToken(userSender, poller)
        val tokenIds = mintAndApprove(5, userSender, token, sudoswapPairFactory)
        val (poolAddress, orderHash) = createPool(
            userSender,
            token.address(),
            spotPrice = BigDecimal("0.001").multiply(decimal).toBigInteger(),
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
            assertThat(it.makeStock).isEqualTo(3)
        }
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
            assertThat(it.makeStock).isEqualTo(3)
        }
    }
}

