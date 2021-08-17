### NFT-Order

This service connects [NFT indexer](../nft) and [order indexer](../order). It basically exposes the same interface as NFT indexer, but also adds some extra fields to Item and Ownership. 

### Extended domain model

[Item](./core/src/main/kotlin/com/rarible/protocol/nftorder/core/model/Item.kt) is extended with:
- bestSellOrder: sell order with the best price (lowest)
- bestBidOrder: bid order with the best price (highest)
- unlockable: if Item is unlockable

[Ownership](./core/src/main/kotlin/com/rarible/protocol/nftorder/core/model/Ownership.kt) is extended with:
- bestSellOrder: sell order with the best price (lowest)

Rarible protocol orders have higher priority than from other platforms. So if there is an order made on Rarible Protocol, it's considered better than order made on other platform. 