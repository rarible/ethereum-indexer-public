package com.rarible.protocol.nft.core.misc

import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.Ownership
import org.assertj.core.api.ObjectAssert

fun ObjectAssert<Item?>.isEqualToItem(expected: Item) {
    this.usingRecursiveComparison().ignoringFields(Item::version.name).isEqualTo(expected)
}

fun ObjectAssert<Ownership?>.isEqualToOwnership(expected: Ownership) {
    this.usingRecursiveComparison().ignoringFields(Ownership::version.name).isEqualTo(expected)
}
