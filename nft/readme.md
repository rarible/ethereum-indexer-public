### NFT-indexer

Listens to EVM-based blockchain and reconstructs state for all NFTs found in the blockchain.
Types of NFTs supported:
- ERC-721
- ERC-1155
- Crypto Punks (in progress)

#### [API](./api)

This module has controllers to 
- query data about NFTs (who owns what, how many ERC-1155 address has)
- query data about events (transfers, mints, burns)
- create lazy-minted NFTs
- post information about pending transactions 

#### [Listener](./listener)

This module listens to the events and updates data accordingly. It's based on [log listener](https://github.com/rarible/ethereum-core/tree/master/listener-log)

#### Data Model

[Item](./core/src/main/kotlin/com/rarible/protocol/nft/core/model/Item.kt) - represents one NFT. Item can be owned only by one address (ERC-721) or many owners can own it (ERC-1155).

[Ownership](./core/src/main/kotlin/com/rarible/protocol/nft/core/model/Ownership.kt) - represents item owned by address (relation between owner and Item).

[ItemHistory](./core/src/main/kotlin/com/rarible/protocol/nft/core/model/ItemHistory.kt) - all events about Items and Ownerships (transfer, mint, burn etc.). Listener listens to these events from the blockchain, saves them and recreates the state of Items and Ownerships.

[Token](./core/src/main/kotlin/com/rarible/protocol/nft/core/model/Token.kt) - represents smart contract (collection) in the blockchain. Items live inside these smart contracts.