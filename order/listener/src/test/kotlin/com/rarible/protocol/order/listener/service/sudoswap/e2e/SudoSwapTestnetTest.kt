package com.rarible.protocol.order.listener.service.sudoswap.e2e

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.contracts.exchange.sudoswap.v1.factory.LSSVMPairFactoryV1
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.LSSVMPairV1
import com.rarible.protocol.dto.OrderSudoSwapAmmDataV1Dto
import com.rarible.protocol.dto.SudoSwapCurveTypeDto
import com.rarible.protocol.order.core.model.ItemId
import com.rarible.protocol.order.core.model.SudoSwapPoolType
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration

@Disabled("This is manual test")
class SudoSwapTestnetTest : AbstractSudoSwapTestnetTest() {

    @Test
    fun `should create trade pool with liner curve`() = runBlocking<Unit> {
        val token = createToken(userSender, poller)
        val tokenId1 = mint(userSender, token)
        val tokenId2 = mint(userSender, token)
        val tokenId3 = mint(userSender, token)
        val tokenId4 = mint(userSender, token)
        val tokenId5 = mint(userSender, token)

        token.approve(sudoswapPairFactory, tokenId1).execute().verifySuccess()
        token.approve(sudoswapPairFactory, tokenId2).execute().verifySuccess()
        token.approve(sudoswapPairFactory, tokenId3).execute().verifySuccess()
        token.approve(sudoswapPairFactory, tokenId4).execute().verifySuccess()
        token.approve(sudoswapPairFactory, tokenId5).execute().verifySuccess()

        val initialNFTIDs = arrayOf(tokenId1, tokenId2)
        val delta = BigDecimal.valueOf(0.2).multiply(decimal).toBigInteger()
        val fee = BigInteger.ZERO
        val spotPrice = BigDecimal("0.500000000000000000")

        val factory = LSSVMPairFactoryV1(sudoswapPairFactory, userSender)
        val result = factory.createPairETH(
            token.address(), //_nft
            sudoswapLinerCurve,//_bondingCurve
            userSender.from(),//_assetRecipient
            SudoSwapPoolType.NFT.value.toBigInteger(),//_poolType
            delta,//_delta
            fee,//_fee
            spotPrice.multiply(decimal).toBigInteger(),//_spotPrice
            initialNFTIDs//_initialNFTIDs
        ).execute().verifySuccess()

        val poolAddress = getPoolAddressFromCreateLog(result)
        logger.info("Pool $poolAddress was created")
        val orderHash = sudoSwapEventConverter.getPoolHash(poolAddress)
        logger.info("Amm order hash $orderHash")

        checkOrder(orderHash) {
            assertThat(it.make.value).isEqualTo(initialNFTIDs.size)
            assertThat(it.makePrice).isEqualTo(spotPrice)

            with(it.data as OrderSudoSwapAmmDataV1Dto) {
                assertThat(this.poolAddress).isEqualTo(poolAddress)
                assertThat(this.bondingCurve).isEqualTo(sudoswapLinerCurve)
                assertThat(this.curveType).isEqualTo(SudoSwapCurveTypeDto.LINEAR)
                assertThat(this.delta).isEqualTo(delta)
                assertThat(this.fee).isEqualTo(fee)
            }
        }
        checkHoldItems(orderHash, token.address(), initialNFTIDs.toList())

        val depositNFTIDs = arrayOf(tokenId3, tokenId4)
        factory.depositNFTs(
            token.address(),
            depositNFTIDs,
            poolAddress
        ).execute().awaitFirst()

        val holdNFTIDs = depositNFTIDs.toList() + initialNFTIDs.toList()

        checkOrder(orderHash) {
            assertThat(it.make.value).isEqualTo(holdNFTIDs.size)
            assertThat(it.makePrice).isEqualTo(spotPrice)
        }
        checkHoldItems(orderHash, token.address(), holdNFTIDs)

        val pair = LSSVMPairV1(poolAddress, userSender)
        pair.swapTokenForAnyNFTs(
            BigInteger.ONE,
            BigDecimal.valueOf(1).multiply(decimal).toBigInteger(),
            userSender.from(),
            false,
            Address.ZERO()
        ).withSender(userSender).withValue(BigDecimal.valueOf(1).multiply(decimal).toBigInteger()).execute().verifySuccess()

        var expectedNextSpotPrice = BigDecimal("0.700000000000000000")
        checkOrder(orderHash) {
            assertThat(it.make.value).isEqualTo(holdNFTIDs.size - 1)
            assertThat(expectedNextSpotPrice.multiply(decimal).toBigInteger()).isEqualTo(pair.spotPrice().call().awaitFirst())
            assertThat(it.makePrice).isEqualTo(expectedNextSpotPrice)
        }
        val actualHoldIds = pair.allHeldIds.call().awaitFirst().toMutableList()
        checkHoldItems(orderHash, token.address(), actualHoldIds.toList())

        val toWithdraw = actualHoldIds.first()
        val toSell = actualHoldIds - toWithdraw

        pair.withdrawERC721(
            token.address(),
            arrayOf(toWithdraw)
        ).execute().verifySuccess()

        expectedNextSpotPrice = BigDecimal("0.700000000000000000")
        checkOrder(orderHash) {
            assertThat(it.make.value).isEqualTo(holdNFTIDs.size - 2)
            assertThat(expectedNextSpotPrice.multiply(decimal).toBigInteger()).isEqualTo(pair.spotPrice().call().awaitFirst())
            assertThat(it.makePrice).isEqualTo(expectedNextSpotPrice)
        }
        checkHoldItems(orderHash, token.address(), toSell)

        pair.swapTokenForSpecificNFTs(
            toSell.toTypedArray(),
            BigDecimal.valueOf(5).multiply(decimal).toBigInteger(),
            userSender.from(),
            false,
            Address.ZERO()
        ).withSender(userSender).withValue(BigDecimal.valueOf(5).multiply(decimal).toBigInteger()).execute().verifySuccess()

        expectedNextSpotPrice = BigDecimal("1.100000000000000000")
        checkOrder(orderHash) {
            assertThat(it.make.value).isEqualTo(BigInteger.ZERO)
            assertThat(expectedNextSpotPrice.multiply(decimal).toBigInteger()).isEqualTo(pair.spotPrice().call().awaitFirst())
            assertThat(it.makePrice).isEqualTo(expectedNextSpotPrice)
        }
        checkHoldItems(orderHash, token.address(), emptyList())
    }

    private suspend fun checkHoldItems(orderHash: Word, collection: Address, tokenIds: List<BigInteger>) {
        val expectedItemIds = tokenIds.map { ItemId(collection, it).toString() }
        Wait.waitAssert(Duration.ofSeconds(20)) {
            val result = ethereumOrderApi.getAmmOrderItemIds(orderHash.prefixed(), null, null).awaitFirst()
            assertThat(result.ids).containsExactlyInAnyOrderElementsOf(expectedItemIds)
        }
    }
}

