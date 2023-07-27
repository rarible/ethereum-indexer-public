package com.rarible.protocol.order.core.service.curve

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.protocol.order.core.configuration.SudoSwapAddresses
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class CompositePoolCurveTest {
    private val addresses = SudoSwapAddresses(randomAddress(), randomAddress(), randomAddress())
    private val sudoSwapLinearCurve = mockk<SudoSwapLinearCurve>()
    private val sudoSwapExponentialCurve = mockk<SudoSwapExponentialCurve>()
    private val sudoSwapChainCurve = mockk<SudoSwapChainCurve>()

    private val curve = CompositePoolCurve(
        sudoSwapAddresses = addresses,
        sudoSwapLinearCurve = sudoSwapLinearCurve,
        sudoSwapExponentialCurve = sudoSwapExponentialCurve,
        sudoSwapChainCurve = sudoSwapChainCurve,
    )

    @Test
    fun `should call liner curve`() = runBlocking<Unit> {
        val curveAddress = addresses.linearCurveV1
        val spotPrice = randomBigInt()
        val delta = randomBigInt()
        val numItems = randomBigInt()
        val feeMultiplier = randomBigInt()
        val protocolFeeMultiplier = randomBigInt()
        coEvery {
            sudoSwapLinearCurve.getBuyInfo(
                curve = curveAddress,
                spotPrice = spotPrice,
                delta = delta,
                numItems = numItems,
                feeMultiplier = feeMultiplier,
                protocolFeeMultiplier = protocolFeeMultiplier
            )
        } returns mockk()
        curve.getBuyInfo(
            curve = curveAddress,
            spotPrice = spotPrice,
            delta = delta,
            numItems = numItems,
            feeMultiplier = feeMultiplier,
            protocolFeeMultiplier = protocolFeeMultiplier
        )
        coVerify {
            sudoSwapLinearCurve.getBuyInfo(any(), any(), any(), any(), any(), any())
        }
        coVerify(exactly = 0) {
            sudoSwapExponentialCurve.getBuyInfo(any(), any(), any(), any(), any(), any())
        }
        coVerify(exactly = 0) {
            sudoSwapChainCurve.getBuyInfo(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `should call exponential curve`() = runBlocking<Unit> {
        val curveAddress = addresses.exponentialCurveV1
        val spotPrice = randomBigInt()
        val delta = randomBigInt()
        val numItems = randomBigInt()
        val feeMultiplier = randomBigInt()
        val protocolFeeMultiplier = randomBigInt()
        coEvery {
            sudoSwapExponentialCurve.getBuyInfo(
                curve = curveAddress,
                spotPrice = spotPrice,
                delta = delta,
                numItems = numItems,
                feeMultiplier = feeMultiplier,
                protocolFeeMultiplier = protocolFeeMultiplier
            )
        } returns mockk()
        curve.getBuyInfo(
            curve = curveAddress,
            spotPrice = spotPrice,
            delta = delta,
            numItems = numItems,
            feeMultiplier = feeMultiplier,
            protocolFeeMultiplier = protocolFeeMultiplier
        )
        coVerify {
            sudoSwapExponentialCurve.getBuyInfo(any(), any(), any(), any(), any(), any())
        }
        coVerify(exactly = 0) {
            sudoSwapLinearCurve.getBuyInfo(any(), any(), any(), any(), any(), any())
        }
        coVerify(exactly = 0) {
            sudoSwapChainCurve.getBuyInfo(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `should call chain curve`() = runBlocking<Unit> {
        val curveAddress = randomAddress()
        val spotPrice = randomBigInt()
        val delta = randomBigInt()
        val numItems = randomBigInt()
        val feeMultiplier = randomBigInt()
        val protocolFeeMultiplier = randomBigInt()
        coEvery {
            sudoSwapChainCurve.getBuyInfo(
                curve = curveAddress,
                spotPrice = spotPrice,
                delta = delta,
                numItems = numItems,
                feeMultiplier = feeMultiplier,
                protocolFeeMultiplier = protocolFeeMultiplier
            )
        } returns mockk()
        curve.getBuyInfo(
            curve = curveAddress,
            spotPrice = spotPrice,
            delta = delta,
            numItems = numItems,
            feeMultiplier = feeMultiplier,
            protocolFeeMultiplier = protocolFeeMultiplier
        )
        coVerify {
            sudoSwapChainCurve.getBuyInfo(any(), any(), any(), any(), any(), any())
        }
        coVerify(exactly = 0) {
            sudoSwapLinearCurve.getBuyInfo(any(), any(), any(), any(), any(), any())
        }
        coVerify(exactly = 0) {
            sudoSwapExponentialCurve.getBuyInfo(any(), any(), any(), any(), any(), any())
        }
    }
}
