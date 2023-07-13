package com.rarible.protocol.order.listener.service.order

import com.rarible.contracts.test.erc721.TestERC721
import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.order.core.data.randomErc20
import com.rarible.protocol.order.core.data.randomErc721
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.approve.ApproveService
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import java.time.Duration
import java.time.temporal.ChronoUnit

@ExperimentalCoroutinesApi
@FlowPreview
@IntegrationTest
class ApprovalOrdersIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var approveService: ApproveService

    @Test
    internal fun `should handle approve for rarible`() {
        checkPlatform(Platform.RARIBLE, transferProxyAddresses.transferProxy)
    }

    @Test
    internal fun `should handle approve for seaport`() {
        checkPlatform(Platform.OPEN_SEA, transferProxyAddresses.seaportTransferProxy, OrderStatus.CANCELLED)
    }

    @Test
    @Disabled // TODO flaky test fix it later
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
    internal fun `should make x2y2 order cancel if not approve for erc721`() {
        checkPlatform(
            Platform.X2Y2,
            transferProxyAddresses.x2y2TransferProxyErc721,
            noApprovalStatus = OrderStatus.CANCELLED
        )
    }

    @Test
    internal fun `should make x2y2 order cancel if not approve for erc1155`() {
        checkPlatform(
            Platform.X2Y2,
            transferProxyAddresses.x2y2TransferProxyErc1155,
            noApprovalStatus = OrderStatus.CANCELLED
        )
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

            setApproval(token, proxy, true)
            Wait.waitAssert(Duration.ofSeconds(10)) {
                checkStatus(true, saved.hash, OrderStatus.ACTIVE)
                assertThat(approveService.checkOnChainApprove(owner, token.address(), platform)).isEqualTo(true)
            }
            setApproval(token, proxy, false)
            Wait.waitAssert(Duration.ofSeconds(10)) {
                checkStatus(false, saved.hash, noApprovalStatus)
                assertThat(approveService.checkOnChainApprove(owner, token.address(), platform)).isEqualTo(false)
            }
        }
    }

    private suspend fun setApproval(
        token: TestERC721,
        proxy: Address,
        approved: Boolean,
    ) {
        token.setApprovalForAll(proxy, approved).execute().verifySuccess()
    }

    private suspend fun checkStatus(
        approved: Boolean,
        hash: Word,
        expectedStatus: OrderStatus,
    ) {
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
            platform = platform,
            end = nowMillis().plus(7, ChronoUnit.DAYS).epochSecond,
        )
        return save(version)
    }
}
