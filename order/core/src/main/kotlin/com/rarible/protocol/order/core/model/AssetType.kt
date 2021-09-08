package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.Tuples
import com.rarible.protocol.contracts.Tuples.keccak256
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Bytes
import io.daonomic.rpc.domain.Word
import org.springframework.data.annotation.Transient
import scala.Tuple2
import scala.Tuple3
import scala.Tuple5
import scala.Tuple6
import scalether.abi.AddressType
import scalether.domain.Address
import java.math.BigInteger

sealed class AssetType(
    /**
     * defined as bytes4 in smart-contracts
     */
    @get:Transient val type: Binary,
    /**
     * defined as bytes in smart-contracts
     */
    @get:Transient val data: Binary,

    /**
     * shows if a type is NFT
     */
    var nft: Boolean
) {

    /**
     * for compatibility with old contracts
     */
    open fun toLegacy(): LegacyAssetType? = null

    fun forPeople() = Tuple2(type, data)

    fun forTx() = Tuple2(type.bytes(), data.bytes())

    companion object {
        val ETH = id("ETH")
        val ERC20 = id("ERC20")
        val ERC721 = id("ERC721")
        val ERC721_LAZY = id("ERC721_LAZY")
        val ERC1155 = id("ERC1155")
        val ERC1155_LAZY = id("ERC1155_LAZY")
        val GEN_ART = id("GEN_ART")

        val AssetType.isLazy: Boolean
            get() = this.type == ERC1155_LAZY || this.type == ERC721_LAZY

        private val TYPE_HASH: Word = keccak256("AssetType(bytes4 assetClass,bytes data)")

        fun hash(type: AssetType): Word = keccak256(Tuples.assetTypeHashType().encode(Tuple3.apply(
            TYPE_HASH.bytes(),
            type.type.bytes(),
            keccak256(type.data).bytes()
        )))

        @JvmStatic
        fun main(args: Array<String>) {
            println(ETH)
            println(ERC721)
            println(ERC1155)
            println(ERC20)
            println(ERC721_LAZY)
            println(ERC1155_LAZY)
        }
    }
}

data class GenerativeArtAssetType(val token: Address) : AssetType(GEN_ART, AddressType.encode(token), false) {
    constructor(data: Binary) : this(AddressType.decode(data, 0).value())
}

object EthAssetType : AssetType(ETH, Binary.apply(), false) {
    override fun toLegacy() = LegacyAssetType(LegacyAssetTypeClass.ETH, Address.ZERO(), BigInteger.ZERO)
}

data class Erc20AssetType(val token: Address) : AssetType(ERC20, AddressType.encode(token), false) {
    override fun toLegacy() = LegacyAssetType(LegacyAssetTypeClass.ERC20, token, BigInteger.ZERO)

    constructor(data: Binary) : this(AddressType.decode(data, 0).value())
}

data class Erc721AssetType(val token: Address, val tokenId: EthUInt256) : AssetType(
    ERC721, Tuples.addressUintType().encode(Tuple2(token, tokenId.value)), true
) {
    override fun toLegacy() = LegacyAssetType(LegacyAssetTypeClass.ERC721, token, tokenId.value)

    companion object {
        fun apply(data: Binary) = run {
            val decoded = Tuples.addressUintType().decode(data, 0)
            Erc721AssetType(decoded.value()._1, EthUInt256(decoded.value()._2))
        }
    }
}

data class Erc1155AssetType(val token: Address, val tokenId: EthUInt256) : AssetType(
    ERC1155, Tuples.addressUintType().encode(Tuple2(token, tokenId.value)), true
) {
    override fun toLegacy() = LegacyAssetType(LegacyAssetTypeClass.ERC1155, token, tokenId.value)

    companion object {
        fun apply(data: Binary) = run {
            val decoded = Tuples.addressUintType().decode(data, 0)
            Erc1155AssetType(decoded.value()._1, EthUInt256(decoded.value()._2))
        }
    }
}

data class Erc721LazyAssetType(
    val token: Address,
    val tokenId: EthUInt256,
    val uri: String,
    val creators: List<Part>,
    val royalties: List<Part>,
    val signatures: List<Binary>
) : AssetType(
    type = ERC721_LAZY,
    data = Tuples.lazy721Type().encode(Tuple2(
        token,
        Tuple5(
            tokenId.value,
            uri,
            creators.toEthereum(),
            royalties.toEthereum(),
            signatures.toEthereum()
        )
    )),
    nft = true
) {
    companion object {
        fun apply(data: Binary) = run {
            val decoded = Tuples.lazy721Type().decode(data, 0).value()
            val message = decoded._2
            Erc721LazyAssetType(
                decoded._1,
                EthUInt256(message._1()),
                message._2(),
                message._3().map { it.toPart() },
                message._4().map { it.toPart() },
                message._5().map { Binary.apply(it) }
            )
        }
    }
}

data class Erc1155LazyAssetType(
    val token: Address,
    val tokenId: EthUInt256,
    val uri: String,
    val supply: EthUInt256,
    val creators: List<Part>,
    val royalties: List<Part>,
    val signatures: List<Binary>
) : AssetType(
    type = ERC1155_LAZY,
    data = Tuples.lazy1155Type().encode(Tuple2(
        token,
        Tuple6(
            tokenId.value,
            uri,
            supply.value,
            creators.toEthereum(),
            royalties.toEthereum(),
            signatures.toEthereum()
        )
    )),
    nft = true
) {
    companion object {
        fun apply(data: Binary) = run {
            val decoded = Tuples.lazy1155Type().decode(data, 0).value()
            val message = decoded._2
            Erc1155LazyAssetType(
                decoded._1,
                EthUInt256(message._1()),
                message._2(),
                EthUInt256(message._3()),
                message._4().map { it.toPart() },
                message._5().map { it.toPart() },
                message._6().map { Binary.apply(it) }
            )
        }
    }
}

fun List<Bytes>.hash(): ByteArray = keccak256(fold(ByteArray(0)) { acc, next -> acc + next.bytes() }).bytes()

private fun List<Binary>.toEthereum() = map { it.bytes() }.toTypedArray()
private fun List<Part>.toEthereum() = map { it.toEthereum() }.toTypedArray()

enum class LegacyAssetTypeClass(val value: BigInteger, val nft: Boolean) {
    ETH(0.toBigInteger(), false),
    ERC20(1.toBigInteger(), false),
    ERC1155(2.toBigInteger(), true),
    ERC721(3.toBigInteger(), true),
}

data class LegacyAssetType(val clazz: LegacyAssetTypeClass, val token: Address, val tokenId: BigInteger)

fun Tuple2<ByteArray, ByteArray>.toAssetType() =
    when (val type = Binary(_1())) {
        AssetType.ETH -> EthAssetType
        AssetType.ERC20 -> Erc20AssetType(Binary(_2()))
        AssetType.ERC721 -> Erc721AssetType.apply(Binary(_2()))
        AssetType.ERC1155 -> Erc1155AssetType.apply(Binary(_2()))
        AssetType.ERC721_LAZY -> Erc721LazyAssetType.apply(Binary(_2()))
        AssetType.ERC1155_LAZY -> Erc1155LazyAssetType.apply(Binary(_2()))
        AssetType.GEN_ART -> GenerativeArtAssetType(Binary(_2()))
        else -> throw IllegalArgumentException("asset type not supported: $type")
    }

fun Tuple2<Address, BigInteger>.toPart() =
    Part(_1(), EthUInt256(_2()))
