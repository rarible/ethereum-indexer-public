### ERC20-indexer

Listens to EVM-based blockchain and reconstructs state for ERC-20 token balances.

#### [API](./api)

This module has controllers to
- query data about ERC-20 balances

#### [Listener](./listener)

This module listens to the events and updates data accordingly. It's based on [log listener](https://github.com/rarible/ethereum-core/tree/master/listener-log)

#### Data Model

[Erc20TokenHistory](./core/src/main/kotlin/com/rarible/protocol/erc20/core/model/Erc20TokenHistory.kt) - events listened from the blockchain

[Erc20Balance](./core/src/main/kotlin/com/rarible/protocol/erc20/core/model/Erc20Balance.kt) - balance for the specific user/token 