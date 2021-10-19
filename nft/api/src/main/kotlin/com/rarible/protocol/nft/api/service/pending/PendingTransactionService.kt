package com.rarible.protocol.nft.api.service.pending

import com.rarible.contracts.erc1155.IERC1155
import com.rarible.contracts.erc1155.TransferSingleEvent
import com.rarible.contracts.erc721.IERC721
import com.rarible.contracts.erc721.TransferEvent
import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.ethereum.log.service.AbstractPendingTransactionService
import com.rarible.ethereum.log.service.LogEventService
import com.rarible.protocol.contracts.Signatures
import com.rarible.protocol.contracts.erc1155.v1.CreateERC1155_v1Event
import com.rarible.protocol.contracts.erc1155.v1.RaribleUserToken
import com.rarible.protocol.contracts.erc721.v3.CreateEvent
import com.rarible.protocol.contracts.erc721.v4.CreateERC721_v4Event
import com.rarible.protocol.nft.core.model.CreateCollection
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.repository.TokenRepository
import com.rarible.protocol.nft.core.service.BlockProcessor
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesService
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Service
import org.web3j.rlp.Utils
import scalether.domain.Address
import java.math.BigInteger
import com.rarible.protocol.contracts.erc721.v2.MintableOwnableToken as MintableOwnableTokenV2
import com.rarible.protocol.contracts.erc721.v3.MintableOwnableToken as MintableOwnableTokenV3
import com.rarible.protocol.contracts.erc721.v4.MintableOwnableToken as MintableOwnableTokenV4

@ExperimentalCoroutinesApi
@Service
class PendingTransactionService(
    private val tokenRepository: TokenRepository,
    private val itemPropertiesService: ItemPropertiesService,
    blockProcessor: BlockProcessor,
    logEventService: LogEventService
) : AbstractPendingTransactionService(logEventService, blockProcessor) {

    override suspend fun process(
        hash: Word,
        from: Address,
        nonce: Long,
        to: Address?,
        id: Binary,
        data: Binary
    ) : List<LogEvent> {
        logger.info("processing tx $hash to: $to data: $data")

        val pendingLogs = if (to == null) {
            tryToProcessCollectionCreate(from, nonce, id, data)
        } else {
            tryToProcessTokenTransfer(from, to, id, data)
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

    private suspend fun tryToProcessTokenTransfer(from: Address, to: Address, id: Binary, data: Binary): List<PendingLog> {
        val pendingLog =  tokenRepository
            .findById(to).awaitFirstOrNull()
            ?.let { processTxToToken(from, to, id, data) }

        return listOfNotNull(pendingLog)
    }

    private fun tryToProcessCollectionCreate(from: Address, nonce: Long, id: Binary, data: Binary): List<PendingLog> {
        val input = id.add(data)
        val pendingLog =  processTxToCreate(from, nonce, input)

        return listOfNotNull(pendingLog)
    }

    private suspend fun processTxToToken(from: Address, to: Address, id: Binary, data: Binary): PendingLog? {
        logger.info("Process tx to token to:$to id:$id data:$data")

        checkTx(id, data, Signatures.mintSignature())?.let {
            itemPropertiesService.saveTemporaryProperties("$to:${it._2()}", it._3()).awaitFirstOrNull()
            return PendingLog(
                ItemTransfer(
                    owner = it._1(),
                    token = to,
                    tokenId = EthUInt256(it._2()),
                    date = nowMillis(),
                    from = it._1(),
                    value = EthUInt256.ONE
                ), to, TransferEvent.id()
            )
        }
        checkTx(id, data, IERC721.transferFromSignature())?.let {
            return PendingLog(
                ItemTransfer(
                    owner = it._2(),
                    token = to,
                    tokenId = EthUInt256(it._3()),
                    date = nowMillis(),
                    from = it._1(),
                    value = EthUInt256.ONE
                ), to, TransferEvent.id()
            )
        }
        checkTx(id, data, Signatures.erc721V3mintSignature())?.let {
            itemPropertiesService.saveTemporaryProperties("$to:${it._1()}", it._5()).awaitFirstOrNull()
            return PendingLog(
                ItemTransfer(
                    owner = from,
                    token = to,
                    tokenId = EthUInt256(it._1()),
                    date = nowMillis(),
                    from = Address.ZERO(),
                    value = EthUInt256.ONE
                ), to, TransferEvent.id()
            )
        }
        checkTx(id, data, Signatures.erc721V4mintSignature())?.let {
            itemPropertiesService.saveTemporaryProperties("$to:${it._1()}", it._6()).awaitFirstOrNull()
            return PendingLog(
                ItemTransfer(
                    owner = from,
                    token = to,
                    tokenId = EthUInt256(it._1()),
                    date = nowMillis(),
                    from = Address.ZERO(),
                    value = EthUInt256.ONE
                ), to, TransferEvent.id()
            )
        }
        checkTx(id, data, IERC721.burnSignature())?.let {
            return PendingLog(
                ItemTransfer(
                    owner = Address.ZERO(),
                    token = to,
                    tokenId = EthUInt256(it),
                    date = nowMillis(),
                    from = from,
                    value = EthUInt256.ONE
                ), to, TransferEvent.id()
            )
        }
        checkTx(id, data, IERC1155.safeTransferFromSignature())?.let {
            return PendingLog(
                ItemTransfer(
                    owner = it._2(),
                    token = to,
                    tokenId = EthUInt256(it._3()),
                    date = nowMillis(),
                    from = it._1(),
                    value = EthUInt256(it._4())
                ), to, TransferSingleEvent.id()
            )
        }
        checkTx(id, data, Signatures.erc1155MintSignatureV1())?.let {
            itemPropertiesService.saveTemporaryProperties("$to:${it._1()}", it._7()).awaitFirstOrNull()
            return PendingLog(
                ItemTransfer(
                    owner = from,
                    token = to,
                    tokenId = EthUInt256(it._1()),
                    date = nowMillis(),
                    from = Address.ZERO(),
                    value = EthUInt256(it._6())
                ), to, TransferSingleEvent.id()
            )
        }
        checkTx(id, data, IERC1155.burnSignature())?.let {
            return PendingLog(
                ItemTransfer(
                    owner = Address.ZERO(),
                    token = to,
                    tokenId = EthUInt256(it._2()),
                    date = nowMillis(),
                    from = from,
                    value = EthUInt256(it._3())
                ), to, TransferSingleEvent.id()
            )
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
                return PendingLog(CreateCollection(
                    id = address,
                    owner = from,
                    name = it.get()._1(),
                    symbol = it.get()._2()
                ), address, CreateEvent.id())
            }
        }
        MintableOwnableTokenV4.checkConstructorTx(input).let {
            if (it.isDefined) {
                return PendingLog(CreateCollection(
                    id = address,
                    owner = from,
                    name = it.get()._1(),
                    symbol = it.get()._2()
                ), address, CreateERC721_v4Event.id())
            }
        }
        RaribleUserToken.checkConstructorTx(input).let {
            if (it.isDefined) {
                return PendingLog(CreateCollection(
                    id = address,
                    owner = from,
                    name = it.get()._1(),
                    symbol = it.get()._2()
                ), address, CreateERC1155_v1Event.id())
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
