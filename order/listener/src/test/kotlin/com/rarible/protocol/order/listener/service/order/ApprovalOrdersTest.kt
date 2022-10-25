package com.rarible.protocol.order.listener.service.order

import com.rarible.contracts.test.erc721.TestERC721
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.order.core.data.randomErc20
import com.rarible.protocol.order.core.data.randomErc721
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.time.Duration

@ExperimentalCoroutinesApi
@FlowPreview
@IntegrationTest
class ApprovalOrdersTest: AbstractIntegrationTest() {

    @Test
    internal fun `should handle approve for rarible`() {
        checkPlatform(Platform.RARIBLE, transferProxyAddresses.transferProxy)
    }

    @Test
    internal fun `should handle approve for seaport`() {
        checkPlatform(Platform.OPEN_SEA, transferProxyAddresses.seaportTransferProxy)
    }

    @Test
    internal fun `should handle approve for punks`() {
        checkPlatform(Platform.CRYPTO_PUNKS, transferProxyAddresses.cryptoPunksTransferProxy)
    }

    @Test
    internal fun `should handle erc721 approve for looksrare`() {
        checkPlatform(Platform.LOOKSRARE, transferProxyAddresses.looksrareTransferManagerERC721)
    }

    @Test
    internal fun `should handle erc1155 approve for looksrare`() {
        checkPlatform(Platform.LOOKSRARE, transferProxyAddresses.looksrareTransferManagerERC1155)
    }

    @Test
    internal fun `should handle non erc721 approve for looksrare`() {
        checkPlatform(Platform.LOOKSRARE, transferProxyAddresses.looksrareTransferManagerNonCompliantERC721)
    }

    @Test
    internal fun `should make x2y2 order cancel if not approve`() {
        checkPlatform(Platform.X2Y2, exchangeContractAddresses.x2y2V1, noApprovalStatus = OrderStatus.CANCELLED)
    }

    private fun checkPlatform(
        platform: Platform,
        proxy: Address,
        noApprovalStatus: OrderStatus = OrderStatus.INACTIVE
    ) {
        runBlocking {
            val (owner, userSender, _) = newSender()
            val token = createToken(userSender)
            val saved = createOrder(owner, token.address(), platform)

            Wait.waitAssert(Duration.ofSeconds(10)) {
                setApprovalAndCheckStatus(token, proxy, true, saved.hash, OrderStatus.ACTIVE)
            }
            Wait.waitAssert(Duration.ofSeconds(10)) {
                setApprovalAndCheckStatus(token, proxy, false, saved.hash, noApprovalStatus)
            }
        }
    }

    private suspend fun setApprovalAndCheckStatus(
        token: TestERC721,
        proxy: Address,
        approved: Boolean,
        hash: Word,
        expectedStatus: OrderStatus
    ) {
        token.setApprovalForAll(proxy, approved).execute().verifySuccess()
        val updated = orderRepository.findById(Word.apply(hash))
        assertThat(updated).isNotNull
        assertThat(updated!!.approved).isEqualTo(approved)
        assertThat(updated.status).isEqualTo(expectedStatus)
    }


    private suspend fun createOrder(maker: Address, token: Address, platform: Platform): Order {
        val version = createOrderVersion().copy(
            maker = maker,
            make = randomErc721(token),
            take = randomErc20(),
            platform = platform
        )
        return orderUpdateService.save(version)
    }
}
