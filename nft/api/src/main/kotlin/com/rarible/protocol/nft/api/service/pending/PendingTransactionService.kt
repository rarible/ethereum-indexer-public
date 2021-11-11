package com.rarible.protocol.nft.api.service.pending

import com.rarible.contracts.erc1155.IERC1155
import com.rarible.contracts.erc1155.TransferSingleEvent
import com.rarible.contracts.erc721.IERC721
import com.rarible.contracts.erc721.TransferEvent
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.ethereum.log.service.AbstractPendingTransactionService
import com.rarible.ethereum.log.service.LogEventService
import com.rarible.protocol.contracts.Signatures
import com.rarible.protocol.contracts.collection.CreateERC1155RaribleEvent
import com.rarible.protocol.contracts.collection.CreateERC1155RaribleUserEvent
import com.rarible.protocol.contracts.collection.CreateERC721RaribleEvent
import com.rarible.protocol.contracts.collection.CreateERC721RaribleUserEvent
import com.rarible.protocol.contracts.erc1155.rarible.factory.ERC1155RaribleFactoryC2
import com.rarible.protocol.contracts.erc1155.rarible.factory.user.ERC1155RaribleUserFactoryC2
import com.rarible.protocol.contracts.erc1155.v1.CreateERC1155_v1Event
import com.rarible.protocol.contracts.erc1155.v1.RaribleUserToken
import com.rarible.protocol.contracts.erc721.rarible.factory.ERC721RaribleFactoryC2
import com.rarible.protocol.contracts.erc721.rarible.factory.user.ERC721RaribleUserFactoryC2
import com.rarible.protocol.contracts.erc1155.rarible.ERC1155Rarible
import com.rarible.protocol.contracts.erc721.rarible.ERC721Rarible
import com.rarible.protocol.contracts.erc721.v3.CreateEvent
import com.rarible.protocol.contracts.erc721.v4.CreateERC721_v4Event
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.CreateCollection
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.service.BlockProcessor
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesService
import com.rarible.protocol.nft.core.span.SpanType
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Service
import org.web3j.rlp.Utils
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender
import java.math.BigInteger
import com.rarible.protocol.contracts.erc721.v2.MintableOwnableToken as MintableOwnableTokenV2
import com.rarible.protocol.contracts.erc721.v3.MintableOwnableToken as MintableOwnableTokenV3
import com.rarible.protocol.contracts.erc721.v4.MintableOwnableToken as MintableOwnableTokenV4

@ExperimentalCoroutinesApi
@Service
@CaptureSpan(type = SpanType.SERVICE, subtype = "pending-transaction")
class PendingTransactionService(
    private val sender: MonoTransactionSender,
    private val tokenRepository: TokenRepository,
    private val itemPropertiesService: ItemPropertiesService,
    private val properties: NftIndexerProperties,
    blockProcessor: BlockProcessor,
    logEventService: LogEventService
) : AbstractPendingTransactionService(logEventService, blockProcessor) {

    private fun erc721Factory() = setOf(
        Address.apply(properties.factory.erc721Rarible),
        Address.apply(properties.factory.erc721RaribleUser)
    )

    private fun erc1155Factory() = setOf(
        Address.apply(properties.factory.erc1155Rarible),
        Address.apply(properties.factory.erc1155RaribleUser)
    )

    override suspend fun process(
        hash: Word,
        from: Address,
        nonce: Long,
        to: Address?,
        id: Binary,
        data: Binary
    ): List<LogEvent> {
        logger.info("processing tx $hash to: $to data: $data")

        val pendingLogs = when {
            to == null -> tryToProcessCollectionCreate(from, nonce, id, data)
            erc721Factory().contains(to) -> listOfNotNull(processTxToERC721Factory(from, id, data))
            erc1155Factory().contains(to) -> listOfNotNull(processTxToERC1155Factory(from, id, data))
            else -> tryToProcessTokenTransfer(from, to, id, data)
        }
        return pendingLogs.map { (event, address, topic) ->
            LogEvent(
                data = event,
                address = address,
                topic = topic,
                transactionHash = hash,
                status = LogEventStatus.PENDING,
                index = 0,
                minorLogIndex = 0
            )
        }
    }

    private suspend fun tryToProcessTokenTransfer(
        from: Address,
        to: Address,
        id: Binary,
        data: Binary
    ): List<PendingLog> {
        val pendingLog = tokenRepository
            .findById(to).awaitFirstOrNull()
            ?.let { processTxToToken(from, to, id, data) }

        return pendingLog ?: listOf()
    }

    private fun tryToProcessCollectionCreate(from: Address, nonce: Long, id: Binary, data: Binary): List<PendingLog> {
        val input = id.add(data)
        val pendingLog = processTxToCreate(from, nonce, input)

        return listOfNotNull(pendingLog)
    }

    private suspend fun processTxToERC721Factory(from: Address, id: Binary, data: Binary): PendingLog? {
        logger.info("Process tx to ERC721factory from:$from id:$id data:$data")

        checkTx(id, data, ERC721RaribleFactoryC2.createTokenSignature())?.let {
            val provider = ERC721RaribleFactoryC2(Address.apply(properties.factory.erc721Rarible), sender)
            val name = it._1()
            val symbol = it._2()
            val address = provider.getAddress(name, symbol, it._3(), it._4(), it._5()).awaitSingle()
            return PendingLog(
                CreateCollection(
                    id = address,
                    owner = from,
                    name = name,
                    symbol = symbol
                ), address, CreateERC721RaribleEvent.id()
            )
        }
        checkTx(id, data, ERC721RaribleUserFactoryC2.createTokenSignature())?.let {
            val provider = ERC721RaribleUserFactoryC2(Address.apply(properties.factory.erc721RaribleUser), sender)
            val name = it._1()
            val symbol = it._2()
            val address = provider.getAddress(name, symbol, it._3(), it._4(), it._5(), it._6()).awaitSingle()
            return PendingLog(
                CreateCollection(
                    id = address,
                    owner = from,
                    name = name,
                    symbol = symbol
                ), address, CreateERC721RaribleUserEvent.id()
            )
        }
        return null
    }

    private suspend fun processTxToERC1155Factory(from: Address, id: Binary, data: Binary): PendingLog? {
        logger.info("Process tx to ERC1155factory from:$from id:$id data:$data")

        checkTx(id, data, ERC1155RaribleFactoryC2.createTokenSignature())?.let {
            val provider = ERC1155RaribleFactoryC2(Address.apply(properties.factory.erc1155Rarible), sender)
            val name = it._1()
            val symbol = it._2()
            val address = provider.getAddress(name, symbol, it._3(), it._4(), it._5()).awaitSingle()
            return PendingLog(
                CreateCollection(
                    id = address,
                    owner = from,
                    name = name,
                    symbol = symbol
                ), address, CreateERC1155RaribleEvent.id()
            )
        }
        checkTx(id, data, ERC1155RaribleUserFactoryC2.createTokenSignature())?.let {
            val provider = ERC1155RaribleUserFactoryC2(Address.apply(properties.factory.erc1155RaribleUser), sender)
            val name = it._1()
            val symbol = it._2()
            val address = provider.getAddress(name, symbol, it._3(), it._4(), it._5(), it._6()).awaitSingle()
            return PendingLog(
                CreateCollection(
                    id = address,
                    owner = from,
                    name = name,
                    symbol = symbol
                ), address, CreateERC1155RaribleUserEvent.id()
            )
        }
        return null
    }

    private suspend fun processTxToToken(from: Address, to: Address, id: Binary, data: Binary): List<PendingLog>? {
        logger.info("Process tx to token to:$to id:$id data:$data")

        checkTx(id, data, Signatures.mintSignature())?.let {
            itemPropertiesService.saveTemporaryProperties("$to:${it._2()}", it._3()).awaitFirstOrNull()
            return listOf(
                PendingLog(
                    ItemTransfer(
                        owner = it._1(),
                        token = to,
                        tokenId = EthUInt256(it._2()),
                        date = nowMillis(),
                        from = it._1(),
                        value = EthUInt256.ONE
                    ), to, TransferEvent.id()
                )
            )
        }
        checkTx(id, data, IERC721.transferFromSignature())?.let {
            return listOf(
                PendingLog(
                    ItemTransfer(
                        owner = it._2(),
                        token = to,
                        tokenId = EthUInt256(it._3()),
                        date = nowMillis(),
                        from = it._1(),
                        value = EthUInt256.ONE
                    ), to, TransferEvent.id()
                )
            )
        }
        checkTx(id, data, Signatures.erc721V3mintSignature())?.let {
            itemPropertiesService.saveTemporaryProperties("$to:${it._1()}", it._5()).awaitFirstOrNull()
            return listOf(
                PendingLog(
                    ItemTransfer(
                        owner = from,
                        token = to,
                        tokenId = EthUInt256(it._1()),
                        date = nowMillis(),
                        from = Address.ZERO(),
                        value = EthUInt256.ONE
                    ), to, TransferEvent.id()
                )
            )
        }
        checkTx(id, data, Signatures.erc721V4mintSignature())?.let {
            itemPropertiesService.saveTemporaryProperties("$to:${it._1()}", it._6()).awaitFirstOrNull()
            return listOf(
                PendingLog(
                    ItemTransfer(
                        owner = from,
                        token = to,
                        tokenId = EthUInt256(it._1()),
                        date = nowMillis(),
                        from = Address.ZERO(),
                        value = EthUInt256.ONE
                    ), to, TransferEvent.id()
                )
            )
        }
        checkTx(id, data, IERC721.burnSignature())?.let {
            return listOf(
                PendingLog(
                    ItemTransfer(
                        owner = Address.ZERO(),
                        token = to,
                        tokenId = EthUInt256(it),
                        date = nowMillis(),
                        from = from,
                        value = EthUInt256.ONE
                    ), to, TransferEvent.id()
                )
            )
        }
        checkTx(id, data, IERC1155.safeTransferFromSignature())?.let {
            return listOf(
                PendingLog(
                    ItemTransfer(
                        owner = it._2(),
                        token = to,
                        tokenId = EthUInt256(it._3()),
                        date = nowMillis(),
                        from = it._1(),
                        value = EthUInt256(it._4())
                    ), to, TransferSingleEvent.id()
                )
            )
        }
        checkTx(id, data, Signatures.erc1155MintSignatureV1())?.let {
            itemPropertiesService.saveTemporaryProperties("$to:${it._1()}", it._7()).awaitFirstOrNull()
            return listOf(
                PendingLog(
                    ItemTransfer(
                        owner = from,
                        token = to,
                        tokenId = EthUInt256(it._1()),
                        date = nowMillis(),
                        from = Address.ZERO(),
                        value = EthUInt256(it._6())
                    ), to, TransferSingleEvent.id()
                )
            )
        }
        checkTx(id, data, IERC1155.burnSignature())?.let {
            return listOf(
                PendingLog(
                    ItemTransfer(
                        owner = Address.ZERO(),
                        token = to,
                        tokenId = EthUInt256(it._2()),
                        date = nowMillis(),
                        from = from,
                        value = EthUInt256(it._3())
                    ), to, TransferSingleEvent.id()
                )
            )
        }
        checkTx(id, data, ERC721Rarible.mintAndTransferSignature())?.let {
            itemPropertiesService.saveTemporaryProperties("$to:${it._1()._1()}", it._1()._2()).awaitFirstOrNull()
            val creator = it._1()._3()[0]._1()
            val mint = PendingLog(
                ItemTransfer(
                    owner = creator,
                    token = to,
                    tokenId = EthUInt256(it._1()._1()),
                    date = nowMillis(),
                    from = Address.ZERO(),
                    value = EthUInt256.ONE
                ), to, TransferEvent.id()
            )
            if (creator == it._2()) {
                return listOf(mint)
            } else {
                return listOf(mint,
                    PendingLog(
                        ItemTransfer(
                            owner = it._2(),
                            token = to,
                            tokenId = EthUInt256(it._1()._1()),
                            date = nowMillis(),
                            from = creator,
                            value = EthUInt256.ONE
                        ), to, TransferEvent.id()
                    )
                )
            }
        }
        checkTx(id, data, ERC1155Rarible.mintAndTransferSignature())?.let {
            itemPropertiesService.saveTemporaryProperties("$to:${it._1()._1()}", it._1()._2()).awaitFirstOrNull()
            val creator = it._1()._4()[0]._1()
            val mint = PendingLog(
                ItemTransfer(
                    owner = creator,
                    token = to,
                    tokenId = EthUInt256(it._1()._1()),
                    date = nowMillis(),
                    from = Address.ZERO(),
                    value = EthUInt256.of(it._3())
                ), to, TransferSingleEvent.id()
            )
            if (creator == it._2()) {
                return listOf(mint)
            } else {
                return listOf(mint,
                    PendingLog(
                        ItemTransfer(
                            owner = it._2(),
                            token = to,
                            tokenId = EthUInt256(it._1()._1()),
                            date = nowMillis(),
                            from = creator,
                            value = EthUInt256.of(it._3())
                        ), to, TransferSingleEvent.id()
                    )
                )
            }
        }
        return null
    }

    private fun processTxToCreate(from: Address, nonce: Long, input: Binary): PendingLog? {
        logger.info("Process tx to create collection from:$from, input:$input")

        val address = Address.apply(Utils.generateContractAddress(from.bytes(), BigInteger.valueOf(nonce)))

        MintableOwnableTokenV2.checkConstructorTx(input).let {
            if (it.isDefined) {
                return PendingLog(
                    CreateCollection(
                        id = address,
                        owner = from,
                        name = it.get()._1(),
                        symbol = it.get()._2()
                    ),
                    address,
                    CreateEvent.id()
                )
            }
        }
        MintableOwnableTokenV3.checkConstructorTx(input).let {
            if (it.isDefined) {
                return PendingLog(
                    CreateCollection(
                        id = address,
                        owner = from,
                        name = it.get()._1(),
                        symbol = it.get()._2()
                    ), address, CreateEvent.id()
                )
            }
        }
        MintableOwnableTokenV4.checkConstructorTx(input).let {
            if (it.isDefined) {
                return PendingLog(
                    CreateCollection(
                        id = address,
                        owner = from,
                        name = it.get()._1(),
                        symbol = it.get()._2()
                    ), address, CreateERC721_v4Event.id()
                )
            }
        }
        RaribleUserToken.checkConstructorTx(input).let {
            if (it.isDefined) {
                return PendingLog(
                    CreateCollection(
                        id = address,
                        owner = from,
                        name = it.get()._1(),
                        symbol = it.get()._2()
                    ), address, CreateERC1155_v1Event.id()
                )
            }
        }
        return null
    }

    private data class PendingLog(
        val eventData: EventData,
        val address: Address,
        val topic: Word
    )
}
