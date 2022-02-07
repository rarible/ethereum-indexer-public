package com.rarible.protocol.nft.core.repository.history

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.nft.core.model.ItemHistory
import com.rarible.protocol.nft.core.model.ItemTransfer
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.Index

internal object NftItemHistoryRepositoryIndexes {
    val TRANSFER_FROM_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${ItemHistory::type.name}", Sort.Direction.ASC)
        .on(LogEvent::status.name, Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ItemTransfer::from.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ItemTransfer::date.name}", Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val TRANSFER_TO_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${ItemHistory::type.name}", Sort.Direction.ASC)
        .on(LogEvent::status.name, Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ItemTransfer::owner.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ItemTransfer::date.name}", Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val TRANSFER_MULTI_USERS_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${ItemHistory::type.name}", Sort.Direction.ASC)
        .on(LogEvent::status.name, Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ItemTransfer::date.name}", Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val BY_COLLECTION_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${ItemHistory::type.name}", Sort.Direction.ASC)
        .on(LogEvent::status.name, Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ItemTransfer::token.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ItemTransfer::date.name}", Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val BY_COLLECTION_TRANSFERS_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${ItemHistory::type.name}", Sort.Direction.ASC)
        .on(LogEvent::status.name, Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ItemTransfer::token.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ItemTransfer::from.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ItemTransfer::date.name}", Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val BY_COLLECTION_OWNER_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${ItemHistory::type.name}", Sort.Direction.ASC)
        .on(LogEvent::status.name, Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ItemTransfer::token.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ItemTransfer::owner.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ItemTransfer::date.name}", Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val BY_ITEM_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${ItemHistory::type.name}", Sort.Direction.ASC)
        .on(LogEvent::status.name, Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ItemTransfer::token.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ItemTransfer::tokenId.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ItemTransfer::date.name}", Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val BY_TYPE_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${ItemHistory::type.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ItemTransfer::date.name}", Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    private val BY_ITEM_INT_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${ItemTransfer::token.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ItemTransfer::tokenId.name}", Sort.Direction.ASC)
        .on(LogEvent::blockNumber.name, Sort.Direction.ASC)
        .on(LogEvent::logIndex.name, Sort.Direction.ASC)
        .background()

    val ALL_INDEXES = listOf(
        TRANSFER_FROM_DEFINITION,
        TRANSFER_TO_DEFINITION,
        TRANSFER_MULTI_USERS_DEFINITION,
        BY_COLLECTION_DEFINITION,
        BY_COLLECTION_TRANSFERS_DEFINITION,
        BY_COLLECTION_OWNER_DEFINITION,
        BY_ITEM_DEFINITION,
        BY_ITEM_INT_DEFINITION,
        BY_TYPE_DEFINITION
    )
}
