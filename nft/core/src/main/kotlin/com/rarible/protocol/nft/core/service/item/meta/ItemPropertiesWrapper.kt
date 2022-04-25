package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.nft.core.model.ItemProperties

class ItemPropertiesWrapper {
    constructor(itemProperties: ItemProperties?, propertiesProcessed: Boolean) {
        this.itemProperties = itemProperties
        this.propertiesProcessed = propertiesProcessed
    }

    var itemProperties :  ItemProperties? = null
    var propertiesProcessed :  Boolean = false

}
