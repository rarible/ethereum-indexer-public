package com.rarible.protocol.nft.core.model

import com.rarible.contracts.erc1155.TransferBatchEvent
import com.rarible.contracts.erc1155.TransferSingleEvent
import com.rarible.contracts.erc721.TransferEvent
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.creators.CreatorsEvent
import com.rarible.protocol.contracts.royalties.RoyaltiesSetEvent
import com.rarible.protocol.contracts.royalties.SecondarySaleFeesEvent
import com.rarible.protocol.contracts.test.crypto.punks.PunkBoughtEvent
import com.rarible.protocol.contracts.test.crypto.punks.PunkTransferEvent
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.bson.types.ObjectId
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import scalether.domain.Address
import java.time.Instant
import com.rarible.protocol.contracts.test.crypto.punks.AssignEvent as PunkAssignEvent

enum class ItemType(val topic: Set<Word>) {
    TRANSFER(setOf(
        TransferEvent.id(),
        TransferSingleEvent.id(),
        TransferBatchEvent.id(),
        PunkAssignEvent.id(),
        PunkTransferEvent.id(),
        PunkBoughtEvent.id()
    )),
    ROYALTY(setOf(SecondarySaleFeesEvent.id(), RoyaltiesSetEvent.id())),
    CREATORS(setOf(CreatorsEvent.id())),
    LAZY_MINT(setOf()),
    BURN_LAZY_MINT(setOf())
}

sealed class ItemHistory(var type: ItemType) : EventData {
    abstract val owner: Address?
    abstract val token: Address
    abstract val tokenId: EthUInt256
    abstract val date: Instant
}

data class ItemTransfer(
    override val owner: Address,
    override val token: Address,
    override val tokenId: EthUInt256,
    override val date: Instant,
    val from: Address,
    val value: EthUInt256 = EthUInt256.ONE
) : ItemHistory(ItemType.TRANSFER)

data class ItemRoyalty(
    override val token: Address,
    override val tokenId: EthUInt256,
    override val date: Instant,
    val royalties: List<Part>
) : ItemHistory(ItemType.ROYALTY) {
    override val owner: Address?
        get() = null
}

data class ItemCreators(
    override val token: Address,
    override val tokenId: EthUInt256,
    override val date: Instant,
    val creators: List<Part>
) : ItemHistory(ItemType.CREATORS) {
    @get:AccessType(AccessType.Type.PROPERTY)
    override var owner: Address? = null
}

sealed class LazyItemHistory(type: ItemType) : ItemHistory(type) {
    @Id
    var id: String = ObjectId().toString()
}

data class ItemLazyMint(
    override val token: Address,
    override val tokenId: EthUInt256,
    val value: EthUInt256,
    override val date: Instant,
    val uri: String,
    val standard: TokenStandard,
    val creators: List<Part>,
    val royalties: List<Part>,
    val signatures: List<Binary>
) : LazyItemHistory(ItemType.LAZY_MINT) {
    @get:AccessType(AccessType.Type.PROPERTY)
    override var owner: Address = creators.first().account
}

data class BurnItemLazyMint(
    val from: Address,
    override val token: Address,
    override val tokenId: EthUInt256,
    val value: EthUInt256,
    override val date: Instant
) : LazyItemHistory(ItemType.BURN_LAZY_MINT) {
    override val owner: Address
        get() = Address.ZERO()
}
