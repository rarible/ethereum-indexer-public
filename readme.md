### Rarible Protocol Ethereum Indexers

Here is the list of indexers:
- [NFT indexer](./nft)
- [ERC-20 indexer](./erc20)
- [Order indexer](./order)

### Architecture

Every indexer listens to specific part of the Ethereum blockchain, users can use these indexers to query data about the blockchain state. Also, indexers emit events when state changes.
Indexers are built using Spring Framework and use these external services:
- mongodb for main data storage
- kafka for handling events

### [OpenAPI](./api)

Indexers use OpenAPI to describe APIs (and events). Clients (kotlin, typescript etc.) and server controller interfaces are generated automatically using yaml OpenAPI files. 

### License

MIT license is used for [api and clients](./api)

GPL v3 license is used for all services and other parts of the indexer