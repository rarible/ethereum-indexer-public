package com.rarible.protocol.order.core.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.Tuples
import com.rarible.protocol.contracts.Tuples.keccak256
import com.rarible.protocol.order.core.model.AssetType.Companion.Type
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

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = GenerativeArtAssetType::class, name = "GEN_ART"),
    JsonSubTypes.Type(value = EthAssetType::class, name = "ETH"),
    JsonSubTypes.Type(value = Erc20AssetType::class, name = "ERC20"),
    JsonSubTypes.Type(value = Erc721AssetType::class, name = "ERC721"),
    JsonSubTypes.Type(value = Erc1155AssetType::class, name = "ERC1155"),
    JsonSubTypes.Type(value = Erc721LazyAssetType::class, name = "ERC721_LAZY"),
    JsonSubTypes.Type(value = Erc1155LazyAssetType::class, name = "ERC1155_LAZY"),
    JsonSubTypes.Type(value = CryptoPunksAssetType::class, name = "CRYPTO_PUNKS"),
    JsonSubTypes.Type(value = CollectionAssetType::class, name = "COLLECTION"),
    JsonSubTypes.Type(value = AmmNftAssetType::class, name = "AMM_NFT"),
)
sealed class AssetType(
    /**
     * defined as bytes4 in smart-contracts
     */
    @get:Transient val type: Type,
    /**
     * defined as bytes in smart-contracts
     */
    @get:Transient @JsonIgnore val data: Binary,

    /**
     * shows if a type is NFT
     */
    @JsonIgnore var nft: Boolean
) {

    /**
     * for compatibility with old contracts
     */
    open fun toLegacy(): LegacyAssetType? = null

    fun forPeople() = Tuple2(type, data)

    fun forTx() = Tuple2(type.id.bytes(), data.bytes())

    companion object {
        enum class Type(val id: Binary) {
            ETH(id("ETH")),
            ERC20(id("ERC20")),
            ERC721(id("ERC721")),
            ERC721_LAZY(id("ERC721_LAZY")),
            ERC1155(id("ERC1155")),
            ERC1155_LAZY(id("ERC1155_LAZY")),
            CRYPTO_PUNKS(id("CRYPTO_PUNKS")),
            GEN_ART(id("GEN_ART")),
            COLLECTION(id("COLLECTION")),
            AMM_NFT(id("AMM_NFT")),
        }

        val AssetType.isLazy: Boolean
            get() = this.type == Type.ERC1155_LAZY || this.type == Type.ERC721_LAZY

        private val TYPE_HASH: Word = keccak256("AssetType(bytes4 assetClass,bytes data)")

        fun hash(type: AssetType): Word = keccak256(Tuples.assetTypeHashType().encode(Tuple3.apply(
            TYPE_HASH.bytes(),
            type.type.id.bytes(),
            keccak256(type.data).bytes()
        )))
    }
}

/**
 * Base class for NFT-like AssetType-s (ERC721, ERC1155, etc).
 */

sealed class NftCollectionAssetType(
    type: Type,
    data: Binary,
    nft: Boolean
) : AssetType(type, data, nft) {
    abstract val token: Address
}

sealed class NftAssetType(
    type: Type,
    data: Binary,
    nft: Boolean
) : NftCollectionAssetType(type, data, nft) {
    abstract val tokenId: EthUInt256
}

data class GenerativeArtAssetType(val token: Address) : AssetType(Type.GEN_ART, AddressType.encode(token), false) {
    constructor(data: Binary) : this(AddressType.decode(data, 0).value())
}

object EthAssetType : AssetType(Type.ETH, Binary.apply(), false) {
    override fun toLegacy() = LegacyAssetType(LegacyAssetTypeClass.ETH, Address.ZERO(), BigInteger.ZERO)
    @Suppress("USELESS_IS_CHECK")
    override fun equals(other: Any?) = this is EthAssetType // Workaround for RPN-879: deserialization leads to a new instance of the same class.
    override fun hashCode() = "EthAssetType".hashCode()
    override fun toString() = "EthAssetType"
}

data class Erc20AssetType(val token: Address) : AssetType(Type.ERC20, AddressType.encode(token), false) {
    override fun toLegacy() = LegacyAssetType(LegacyAssetTypeClass.ERC20, token, BigInteger.ZERO)

    constructor(data: Binary) : this(AddressType.decode(data, 0).value())
}

data class Erc721AssetType(override val token: Address, override val tokenId: EthUInt256) : NftAssetType(
    Type.ERC721, Tuples.addressUintType().encode(Tuple2(token, tokenId.value)), true
) {
    override fun toLegacy() = LegacyAssetType(LegacyAssetTypeClass.ERC721, token, tokenId.value)

    companion object {
        fun apply(data: Binary) = run {
            val decoded = Tuples.addressUintType().decode(data, 0)
            Erc721AssetType(decoded.value()._1, EthUInt256(decoded.value()._2))
        }
    }
}

data class Erc1155AssetType(override val token: Address, override val tokenId: EthUInt256) : NftAssetType(
    Type.ERC1155, Tuples.addressUintType().encode(Tuple2(token, tokenId.value)), true
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
    override val token: Address,
    override val tokenId: EthUInt256,
    val uri: String,
    val creators: List<Part>,
    val royalties: List<Part>,
    val signatures: List<Binary>
) : NftAssetType(
    type = Type.ERC721_LAZY,
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
    override val token: Address,
    override val tokenId: EthUInt256,
    val uri: String,
    val supply: EthUInt256,
    val creators: List<Part>,
    val royalties: List<Part>,
    val signatures: List<Binary>
) : NftAssetType(
    type = Type.ERC1155_LAZY,
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

data class CryptoPunksAssetType(
    override val token: Address,
    override val tokenId: EthUInt256
) : NftAssetType(
    type = Type.CRYPTO_PUNKS,
    data = Tuples.addressUintType().encode(Tuple2(token, tokenId.value)),
    nft = true
) {
    companion object {
        fun apply(data: Binary) = run {
            val decoded = Tuples.addressUintType().decode(data, 0)
            CryptoPunksAssetType(decoded.value()._1, EthUInt256(decoded.value()._2))
        }
    }
}

data class CollectionAssetType(override val token: Address) : NftCollectionAssetType(Type.COLLECTION, AddressType.encode(token), true) {
    constructor(data: Binary) : this(AddressType.decode(data, 0).value())
}

data class AmmNftAssetType(override val token: Address) : NftCollectionAssetType(Type.AMM_NFT, AddressType.encode(token), true) {
    constructor(data: Binary) : this(AddressType.decode(data, 0).value())
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
        Type.ETH.id -> EthAssetType
        Type.ERC20.id -> Erc20AssetType(Binary(_2()))
        Type.ERC721.id -> Erc721AssetType.apply(Binary(_2()))
        Type.ERC1155.id -> Erc1155AssetType.apply(Binary(_2()))
        Type.ERC721_LAZY.id -> Erc721LazyAssetType.apply(Binary(_2()))
        Type.ERC1155_LAZY.id -> Erc1155LazyAssetType.apply(Binary(_2()))
        Type.CRYPTO_PUNKS.id -> CryptoPunksAssetType.apply(Binary(_2()))
        Type.GEN_ART.id -> GenerativeArtAssetType(Binary(_2()))
        Type.COLLECTION.id -> CollectionAssetType(Binary(_2()))
        else -> throw IllegalArgumentException("asset type not supported: $type")
    }

fun Tuple2<Address, BigInteger>.toPart() =
    Part(_1(), EthUInt256(_2()))
