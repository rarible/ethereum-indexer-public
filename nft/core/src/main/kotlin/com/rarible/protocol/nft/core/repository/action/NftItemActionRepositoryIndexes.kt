package com.rarible.protocol.nft.core.repository.action

import com.rarible.protocol.nft.core.model.Action
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.Index

internal object NftItemActionRepositoryIndexes {
    private val BY_ITEM_ID_AND_TYPE_DEFINITION: Index = Index()
        .on(Action::token.name, Sort.Direction.ASC)
        .on(Action::tokenId.name, Sort.Direction.ASC)
        .on(Action::type.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    private val BY_STATE_AND_ACTION_AT_DEFINITION: Index = Index()
        .on(Action::state.name, Sort.Direction.ASC)
        .on(Action::actionAt.name, Sort.Direction.ASC)
        .background()

    val ALL_INDEXES = listOf(
        BY_ITEM_ID_AND_TYPE_DEFINITION,
        BY_STATE_AND_ACTION_AT_DEFINITION
    )
}
