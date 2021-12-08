# Rarible Protocol Ethereum Indexers

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

### OpenAPI

Indexers use OpenAPI to describe APIs (and events). Clients (kotlin, typescript etc.) and server controller interfaces are generated automatically using yaml OpenAPI files.

See more information in [Rarible Etehreum Protocol OpenAPI](https://github.com/rarible/ethereum-openapi).

OpenAPI docs: [https://ethereum-api.rarible.org/v0.1/doc](https://ethereum-api.rarible.org/v0.1/doc)

### Suggestions

You are welcome to [suggest features](https://github.com/rarible/protocol/discussions) and [report bugs found](https://github.com/rarible/protocol/issues)!

### License

[GPL v3 license](LICENSE) is used for all services and other parts of the indexer.