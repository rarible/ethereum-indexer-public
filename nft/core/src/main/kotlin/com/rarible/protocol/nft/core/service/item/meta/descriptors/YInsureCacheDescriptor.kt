package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.cache.CacheDescriptor
import com.rarible.protocol.contracts.external.quotation.QuotationData
import com.rarible.protocol.contracts.external.yinsure.YInsure
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemProperties
import org.apache.commons.lang3.time.DateUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender
import java.text.SimpleDateFormat
import java.util.*

@Component
class YInsureCacheDescriptor(
    sender: MonoTransactionSender,
    @Value("\${api.yinsure.address}") yInsureAddress: String,
    @Value("\${api.yinsure.cache-timeout}") private val cacheTimeout: Long,
    @Value("\${api.quotation.address}") quotationDataAddress: String,
    @Value("\${api.properties.api-url}") private val apiUrl: String
) : CacheDescriptor<ItemProperties> {
    private val yInsure = YInsure(Address.apply(yInsureAddress), sender)
    private val quotationData = QuotationData(Address.apply(quotationDataAddress), sender)

    final val token = "yinsure"
    override val collection: String = "cache_$token"

    override fun getMaxAge(value: ItemProperties?): Long = if (value == null) {
        DateUtils.MILLIS_PER_HOUR
    } else {
        cacheTimeout
    }

    val formatterShort = SimpleDateFormat("dd/MM/yyyy")
    val formatterLong = SimpleDateFormat("E MMM dd yyyy", Locale.ENGLISH)

    private val yInsurePlatformMap = listOf(
        YInsurePlatform(
            "0x9D25057e62939D3408406975aD75Ffe834DA4cDd".toLowerCase(),
            "yearn.finance",
            Pair("#5999FF", "#0B5FD3"),
            "yfi.png"
        ),
        YInsurePlatform(
            "0xc1D2819CE78f3E15Ee69c6738eB1B400A26e632A".toLowerCase(),
            "Aave",
            Pair("#64D4FF", "#008ADB"),
            "aave.png"
        ),
        YInsurePlatform(
            "0x9424B1412450D0f8Fc2255FAf6046b98213B76Bd".toLowerCase(),
            "Balancer",
            Pair("#575757", "#000000"),
            "balancer.png"
        ),
        YInsurePlatform(
            "0x3d9819210A31b4961b30EF54bE2aeD79B9c9Cd3B".toLowerCase(),
            "Compound",
            Pair("#68E3AA", "#00AA5A"),
            "compound.png"
        ),
        YInsurePlatform(
            "0x79a8C46DeA5aDa233ABaFFD40F3A0A2B1e5A4F27".toLowerCase(),
            "Curve",
            Pair("#00A3FF", "#0066FF"),
            "curve.png"
        ),
        YInsurePlatform(
            "0x02285AcaafEB533e03A7306C55EC031297df9224".toLowerCase(),
            "dforce",
            Pair("#00A3FF", "#0066FF"),
            "dforce.jpeg"
        ),
        YInsurePlatform(
            "0xAFcE80b19A8cE13DEc0739a1aaB7A028d6845Eb3".toLowerCase(),
            "mstable",
            Pair("#00A3FF", "#0066FF"),
            "mstable.png"
        ),
        YInsurePlatform(
            "0xb529964F86fbf99a6aA67f72a27e59fA3fa4FEaC".toLowerCase(),
            "opyn",
            Pair("#00A3FF", "#0066FF"),
            "opyn.png"
        ),
        YInsurePlatform(
            "0xC011a73ee8576Fb46F5E1c5751cA3B9Fe0af2a6F".toLowerCase(),
            "SynthetiX",
            Pair("#00A3FF", "#0066FF"),
            "synthetix.png"
        ),
        YInsurePlatform(
            "0x3e532e6222afe9Bcf02DCB87216802c75D5113aE".toLowerCase(),
            "UMA",
            Pair("#00A3FF", "#0066FF"),
            "uma.png"
        ),
        YInsurePlatform(
            "0x5C69bEe701ef814a2B6a3EDD4B1652CB9cc5aA6f".toLowerCase(),
            "Uniswap V2",
            Pair("#FF7CAF", "#EB367C"),
            "uniswap.png"
        )
    )

    override fun get(id: String): Mono<ItemProperties> {
        return yInsure.tokens(id.toBigInteger())
            .flatMap { tuple ->
                val currency = String(tuple._2()).replace("\u0000", "")
                val amount = String.format(Locale.ENGLISH, "%,d", tuple._3().toLong())
                val expireTime = tuple._1()
                val expireTimeMillis = expireTime.multiply(1000.toBigInteger()).toLong()
                val validUntil = formatterShort.format(Date(expireTimeMillis))
                val validUntilLong = formatterLong.format(Date(expireTimeMillis))
                quotationData.getscAddressOfCover(tuple._8())
                    .map {
                        val platform = yInsurePlatformMap.firstOrNull { map -> map.address == it._2.toString() }
                            ?: YInsurePlatform(it._2.toString(), "Unknown", Pair("#00A3FF", "#0066FF"), "ethereum.svg")

                        val attributes = listOf(
                            ItemAttribute("token", token),
                            ItemAttribute("currency", currency),
                            ItemAttribute("amount", amount),
                            ItemAttribute("expireTime", expireTime.toString()),
                            ItemAttribute("expirationTimestamp", expireTime.toString()),
                            ItemAttribute("validUntil", validUntil),
                            ItemAttribute("scAddressToCover", it._2.toString()),
                            ItemAttribute("iconUrl", platform.iconUrl),
                            ItemAttribute("platform", platform.name),
                            ItemAttribute("startColor", platform.gradientColor.first),
                            ItemAttribute("stopColor", platform.gradientColor.second)
                        )
                        ItemProperties(
                            name = "${platform.name} | $amount $currency \uD83D\uDD12 | $validUntil",
                            description = "Covers ${platform.name} Smart Contract risks worth $amount $currency. Policy is valid until $validUntilLong",
                            image = "$apiUrl/image/$token/$id.svg",
                            attributes = attributes,
                            imagePreview = null,
                            imageBig = null,
                            animationUrl = null
                        )
                    }
            }
    }

    private data class YInsurePlatform(
        val address: String,
        val name: String,
        val gradientColor: Pair<String, String>,
        val iconUrl: String
    )
}
