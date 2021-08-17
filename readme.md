### Rarible Protocol Ethereum Indexers

Here is the list of indexers:
- [NFT indexer](./nft) - aggregates data about NFTs
- [ERC-20 indexer](./erc20) - aggregates data about ERC-20 tokens and balances 
- [Order indexer](./order) - aggregates data about Orders from different platforms
- [NFT-order](./nft-order) - connects together nft and order indexers

### Architecture

Every indexer listens to specific part of the Ethereum blockchain, users can use these indexers to query data about the blockchain state. Also, indexers emit events when state changes.
Indexers are built using Spring Framework and use these external services:
- mongodb for main data storage
- kafka for handling events

### [OpenAPI](./api)

Indexers use OpenAPI to describe APIs (and events). Clients (kotlin, typescript etc.) and server controller interfaces are generated automatically using yaml OpenAPI files.

OpenAPI doc: [https://ethereum-api.rarible.org/v0.1/doc](https://ethereum-api.rarible.org/v0.1/doc)

### Suggestions

You are welcome to suggest features and report bugs found! You can do it here: https://github.com/rarible/protocol-issues/issues

### License

MIT license is used for [api and clients](./api)

GPL v3 license is used for all services and other parts of the indexer