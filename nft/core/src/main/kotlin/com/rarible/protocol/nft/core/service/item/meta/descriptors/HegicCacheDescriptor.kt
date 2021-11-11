package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.cache.CacheDescriptor
import com.rarible.protocol.contracts.external.hegic.Hegic
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.span.SpanType
import org.apache.commons.lang3.time.DateUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*

@Component
@CaptureSpan(type = SpanType.SERVICE, subtype = "hegic-descriptor")
class HegicCacheDescriptor(
    sender: MonoTransactionSender,
    @Value("\${api.hegic.address}") hegicAddress: String,
    @Value("\${api.hegic.cache-timeout}") private val cacheTimeout: Long,
    @Value("\${api.properties.api-url}") private val apiUrl: String
) : CacheDescriptor<ItemProperties> {
    private val hegic = Hegic(Address.apply(hegicAddress), sender)
    final val token = "hegic"
    override val collection: String = "cache_$token"

    override fun getMaxAge(value: ItemProperties?): Long = if (value == null) {
        DateUtils.MILLIS_PER_HOUR
    } else {
        cacheTimeout
    }

    private val formatterDate = SimpleDateFormat("dd.MM")
    private val formatterTime = SimpleDateFormat("hh:mm a")
    private val formatterUTC = SimpleDateFormat("dd.MM.yyyy hh'h'mm 'UTC'", Locale.ENGLISH)

    init {
        formatterDate.timeZone = TimeZone.getTimeZone("UTC")
        formatterTime.timeZone = TimeZone.getTimeZone("UTC")
        formatterUTC.timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun get(id: String): Mono<ItemProperties> {
        return hegic.getUnderlyingOptionParams(id.toBigInteger()).call()
            .flatMap { tuple ->
                val period = tuple._7()
                val amount = tuple._4()
                val strike = tuple._3()
                val optionType = tuple._8()
                val dealType = if (optionType == BigInteger.ONE) "put" else "call"
                val optionTypeText = if (optionType == BigInteger.ONE) "sell" else "buy"
                val expirationTimeMillis = period.multiply(1000.toBigInteger()).toLong()
                val expirationDate = Date(expirationTimeMillis)
                val expirationDateUTCText = formatterUTC.format(expirationDate)
                hegic.getOptionCostETH(period, amount, strike, optionType).call()
                    .map {
                        val ethCost = String.format(Locale.ENGLISH, "%,d", it.divide(BigInteger.TEN.pow(16)).toLong())
                        val attributes = listOf(
                            ItemAttribute("token", token),
                            ItemAttribute("state", tuple._1().toString()),
                            ItemAttribute("holder", tuple._2().toString()),
                            ItemAttribute("strike", strike.toString()),
                            ItemAttribute("amount", amount.toString()),
                            ItemAttribute("lockedAmount", tuple._5().toString()),
                            ItemAttribute("premium", tuple._6().toString()),
                            ItemAttribute("expirationDate", formatterDate.format(expirationDate)),
                            ItemAttribute("expirationTime", formatterTime.format(expirationDate)),
                            ItemAttribute("optionType", optionType.toString()),
                            ItemAttribute("ethCost", ethCost),
                            ItemAttribute("assetUnderlying", "ETH"),
                            ItemAttribute("amountUnderlying", "1")
                        )
                        ItemProperties(
                            name = "\$$ethCost $dealType option for 1 ETH. Expires on $expirationDateUTCText",
                            description = "This NFT represents a Hegic $dealType option for 1 ETH at strike price \$$ethCost. It expires on the $expirationDateUTCText.\n" +
                                    "Holding this NFT gives the holder the right to $optionTypeText 1 ETH at \$$ethCost anytime before the maturity date ($expirationDateUTCText).\n" +
                                    "Hegic options are cash-settled so you will receive the difference between ETH's price at exercise and strike price. This is, you will not need to buy the underlying asset when you exercise. \n" +
                                    "Options provider is Hegic Protocol. To exercise, please visit www.hegic.co.",
                            image = "$apiUrl/image/$token/$id.svg",
                            attributes = attributes,
                            imagePreview = null,
                            imageBig = null,
                            animationUrl = null
                        )
                    }
            }
    }
}
