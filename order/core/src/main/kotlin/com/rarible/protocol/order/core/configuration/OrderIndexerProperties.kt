package com.rarible.protocol.order.core.configuration

import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.order.core.model.ScannerVersion
import com.rarible.protocol.order.core.model.TraceMethod
import io.daonomic.rpc.domain.Binary
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.NestedConfigurationProperty
import scalether.domain.Address
import java.math.BigDecimal
import java.time.Duration

const val RARIBLE_PROTOCOL_ORDER_INDEXER = "common"

@ConstructorBinding
@ConfigurationProperties(RARIBLE_PROTOCOL_ORDER_INDEXER)
data class OrderIndexerProperties(
    val blockchain: Blockchain,
    val kafkaReplicaSet: String,
    val eip712DomainName: String,
    val eip712DomainVersion: String,
    val openseaEip712DomainName: String,
    val openseaEip712DomainVersion: String,
    var openSeaNonceIncrement: Long = 0,
    val minSeaportMakeWeiPrice: Int = 1000,
    val chainId: Int,
    var operatorPrivateKey: Binary,
    val protocolCommission: Int,
    val metricRootPath: String,
    @NestedConfigurationProperty
    val exchangeContractAddresses: ExchangeContractAddresses,
    @NestedConfigurationProperty
    val auctionContractAddresses: AuctionContractAddresses,
    @NestedConfigurationProperty
    val transferProxyAddresses: TransferProxyAddresses,
    @NestedConfigurationProperty
    val currencyContractAddresses: CurrencyContractAddresses,
    @NestedConfigurationProperty
    val publish: PublishProperties = PublishProperties(),
    @NestedConfigurationProperty
    val featureFlags: FeatureFlags = FeatureFlags(),
    val blockCountBeforeSnapshot: Int = 12,
    val handleApprovalAfterBlock: Long = 0,
    val traceMethod: TraceMethod = TraceMethod.TRACE_TRANSACTION,
    @NestedConfigurationProperty
    val raribleOrderExpiration: RaribleOrderExpirationProperties = RaribleOrderExpirationProperties(),
    @NestedConfigurationProperty
    val sudoSwapAddresses: SudoSwapAddresses = SudoSwapAddresses(),
    val bidValidation: BidValidationProperties = BidValidationProperties(),
    val orderEventHandle: OrderEventHandleProperties = OrderEventHandleProperties(),
    val poolEventHandle: PoolEventHandleProperties = PoolEventHandleProperties(),
) {
    val minSeaportMakePrice = BigDecimal.valueOf(minSeaportMakeWeiPrice.toLong()) * BigDecimal.valueOf(1, 18)

    data class BidValidationProperties(
        val minPriceUsd: BigDecimal = BigDecimal.ONE, //1 USD
        val minPercentFromFloorPrice: BigDecimal = 0.75.toBigDecimal(), //Bid price should be not less the 75% from floor price
    )

    data class CurrencyContractAddresses(
        var weth: Address
    )

    data class ExchangeContractAddresses(
        var v1: Address,
        var v1Old: Address? = null,
        var v2: Address,
        var openSeaV1: Address,
        var openSeaV2: Address,
        var seaportV1: Address,
        var cryptoPunks: Address,
        var zeroEx: Address,
        var looksrareV1: Address,
        var x2y2V1: Address,
    )

    data class AuctionContractAddresses(
        var v1: Address
    )

    data class TransferProxyAddresses(
        val transferProxy: Address,
        val erc20TransferProxy: Address,
        val erc721LazyTransferProxy: Address,
        val erc1155LazyTransferProxy: Address,
        var cryptoPunksTransferProxy: Address,
        val seaportTransferProxy: Address,
        val x2y2TransferProxyErc721: Address,
        val x2y2TransferProxyErc1155: Address,
        val looksrareTransferManagerERC721: Address,
        val looksrareTransferManagerERC1155: Address,
        val looksrareTransferManagerNonCompliantERC721: Address
    )

    data class PublishProperties(
        val publishSeaportOrders: Boolean = true,
        val publishX2Y2Orders: Boolean = false,
        val publishLooksrareOrders: Boolean = false,
        val publishCryptoPunksOrders: Boolean = true,
        val publishAmmOrders: Boolean = false,
        val publishAuctionActivity: Boolean = false,
    )

    data class FeatureFlags(
        val showAllOrdersByDefault: Boolean = false,
        val showOpenSeaOrdersWithOtherPlatforms: Boolean = false,
        val showX2Y2OrdersWithOtherPlatforms: Boolean = false,
        val showLooksrareOrdersWithOtherPlatforms: Boolean = false,
        val showSudoSwapOrdersWithOtherPlatforms: Boolean = false,
        val hideOpenSeaSignatures: Boolean = false,
        val maxOpenSeaNonceCalculation: Int = 10,
        val skipGetTrace: Boolean = false,
        val checkOnChainApprove: Boolean = false,
        val applyOnChainApprove: Boolean = false,
        val searchSudoSwapErc1155Transfer: Boolean = false,
        val checkMinimalBidPrice: Boolean = false,
        val checkMinimalCollectionBidPriceOnly: Boolean = false,
        val scannerVersion: ScannerVersion = ScannerVersion.V1,
        val enableAuction: Boolean = false,
        val skipEventsIfNoTraceFound: Boolean = false,
        val getPoolInfoFromChain: Boolean = true
    )

    data class RaribleOrderExpirationProperties(
        val bidExpirePeriod: Duration = Duration.ofDays(60L),
        val delayPeriod: Duration = Duration.ofMinutes(10L)
    )

    sealed class EthereumEventHandleProperties {
        abstract val parallel: Boolean
        abstract val chunkSize: Int
    }

    data class OrderEventHandleProperties(
        override val parallel: Boolean = false,
        override val chunkSize: Int = 20,
        val handleSeaport: Boolean = true,
    ) : EthereumEventHandleProperties()

    data class PoolEventHandleProperties(
        override val parallel: Boolean = false,
        override val chunkSize: Int = 20
    ) : EthereumEventHandleProperties()
}

data class SudoSwapAddresses(
    var pairFactoryV1: Address = Address.ZERO(),
    var exponentialCurveV1: Address = Address.ZERO(),
    var linearCurveV1: Address = Address.ZERO(),
)

