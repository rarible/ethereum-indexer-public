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

    val BY_TYPE_STATUS_UPDATED_AT_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${ItemHistory::type.name}", Sort.Direction.ASC)
        .on(LogEvent::status.name, Sort.Direction.ASC)
        .on(LogEvent::updatedAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    private val BY_ITEM_INT_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${ItemTransfer::token.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${ItemTransfer::tokenId.name}", Sort.Direction.ASC)
        .on(LogEvent::blockNumber.name, Sort.Direction.ASC)
        .on(LogEvent::logIndex.name, Sort.Direction.ASC)
        .background()

    private val BY_ITEM_AND_OWNER_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${ItemHistory::type.name}", Sort.Direction.DESC)
        .on(LogEvent::status.name, Sort.Direction.DESC)
        .on("${LogEvent::data.name}.${ItemTransfer::token.name}", Sort.Direction.DESC)
        .on("${LogEvent::data.name}.${ItemTransfer::tokenId.name}", Sort.Direction.DESC)
        .on("${LogEvent::data.name}.${ItemTransfer::owner.name}", Sort.Direction.DESC)
        .on("${LogEvent::data.name}.${ItemHistory::date.name}", Sort.Direction.DESC)
        .on("_id", Sort.Direction.DESC)
        .background()

    // TODO: Maybe this index should be removed
    val BY_UPDATED_AT_FIELD: Index = Index()
        .on(LogEvent::updatedAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val ALL_INDEXES = listOf(
        TRANSFER_FROM_DEFINITION,
        TRANSFER_TO_DEFINITION,
        BY_TYPE_STATUS_UPDATED_AT_DEFINITION,
        BY_COLLECTION_DEFINITION,
        BY_COLLECTION_TRANSFERS_DEFINITION,
        BY_COLLECTION_OWNER_DEFINITION,
        BY_ITEM_DEFINITION,
        BY_ITEM_INT_DEFINITION,
        BY_TYPE_DEFINITION,
        BY_ITEM_AND_OWNER_DEFINITION,
        BY_UPDATED_AT_FIELD
    )
}
