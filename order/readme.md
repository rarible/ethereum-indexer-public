### Order indexer

Stores off-chain orders and listens to EVM-based blockchain and reconstructs state for all orders. Supports these types of orders:
- Rarible V1 off-chain orders
- Rarible V2 off-chain orders
- Rarible V2 (on-chain) orders
- Rarible Auctions (WIP)
- TBA

#### [API](./api)

This module has controllers to
- query data about orders (all, sell orders, bids, by item, by collection etc)
- create or update orders
- query information about events
- post information about pending transactions

#### [Listener](./listener)

This module listens to the events and updates data accordingly. It's based on [log listener](https://github.com/rarible/ethereum-core/tree/master/listener-log)

#### Data Model

[Order](./core/src/main/kotlin/com/rarible/protocol/order/core/model/Order.kt) - represents intent of the user to exchange his assets to some other assets

[OrderExchangeHistory](./core/src/main/kotlin/com/rarible/protocol/order/core/model/OrderExchangeHistory.kt) - events listened from the blockchain. Usually, these events signal order was filled (possibly partially) or cancelled.

Order indexer supports different kinds of assets:
- ERC-20
- ERC-721
- ERC-1155
- ETH
- and others.

Full list of supported assets can be viewed [here](core/src/main/kotlin/com/rarible/protocol/order/core/model/AssetType.kt) 