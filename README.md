# Rarible Protocol Ethereum Indexers

On Rarible, we have several types of indexers:

**[NFT indexer](./nft)** — is used to index all history of NFTs related actions, i.e., mint, transfer, and burn. Indexer gets logs from an Ethereum network, and accordingly creates NFT items in a NoSQL database. Especially, it listens to a change of a state of an NFT item ownership.

**[ERC-20 indexer](./erc20)** — is responsible for tracking user balances. If we mapped it to the Rarible usage, it checks if a user has enough of a given currency to make a bid. It handles all the information about a user's wallet status.

**[Order indexer](./order)** — aggregates data about Orders from different platforms. In order to properly display order information (order means intent, e.g., intent to sell, or in simpler words, intent to sell an NFT for a given price), we need an order price in addition to other properties.

## Architecture

Every indexer listens to a specific part of the Ethereum blockchain, users can use these indexers to query data about the blockchain state. Also, indexers emit events when state changes.

Indexers were built using Spring Framework and use these external services:

* MongoDB for main data storage
* Kafka for handling events

[Order diagrams](docs/order.md)

## Running Indexer with Docker Compose

### Requirements

The recommended requirements for running the Indexer via Docker Compose are listed below:

* JDK 8
    * [Windows](https://www.oracle.com/ru/java/technologies/javase/javase8-archive-downloads.html)
    * [macOS M1](https://www.azul.com/downloads/?os=macos&architecture=arm-64-bit&package=jdk#download-openjdk)
* Maven
    * [Windows](https://maven.apache.org/download.cgi)
    * [macOS M1](https://dev.to/shane/configure-m1-mac-to-use-jdk8-with-maven-4b4g)
* [Docker](https://docs.docker.com/desktop/)
    * After installation, turn on _Use Docker Compose V2_ in the Settings > General

### Get started

1. Clone the project:

    ```shell
    git clone https://github.com/rarible/ethereum-indexer.git
    ```

2. Go to the project folder and run Maven command:

    ```shell
    mvn clean package -DskipTests
    ```

3. Go to the docker folder and run `build` command:

    ```shell
    cd docker/
    docker-compose build
    ```

4. To start docker compose, run `up` command:

    ```shell
    docker-compose up
    ```

Now you can see how the indexer works.

This is convenient for testing. For example, you can deploy a contract in a test node, configure the SDK for it and see how the information in the indexer will be updated after minting.

## OpenAPI

Indexers use OpenAPI to describe APIs (and events). Clients (Kotlin, TypeScript etc.) and server controller interfaces are generated automatically using YAML OpenAPI files.

See more information on [Rarible Ethereum Protocol OpenAPI](https://github.com/rarible/ethereum-openapi) repository.

Ethereum OpenAPI docs: [https://ethereum-api.rarible.org/v0.1/doc](https://ethereum-api.rarible.org/v0.1/doc)

## Suggestions

You are welcome to [suggest features](https://github.com/rarible/protocol/discussions) and [report bugs found](https://github.com/rarible/protocol/issues)!

## Contributing

The codebase is maintained using the "contributor workflow" where everyone without exception contributes patch proposals using "pull requests" (PRs). This facilitates social contribution, easy testing, and peer review.

See more information on [CONTRIBUTING.md](https://github.com/rarible/protocol/blob/main/CONTRIBUTING.md).

## License

[GPL v3 license](LICENSE) is used for all services and other parts of the indexer.
