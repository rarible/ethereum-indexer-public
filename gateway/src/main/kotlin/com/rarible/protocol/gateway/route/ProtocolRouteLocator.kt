package com.rarible.protocol.gateway.route

import com.rarible.protocol.gateway.configuration.GatewayProperties
import com.rarible.protocol.gateway.service.cluster.UriProvider
import org.springframework.cloud.gateway.route.Route
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.cloud.gateway.route.builder.RouteLocatorDsl
import org.springframework.cloud.gateway.route.builder.filters
import org.springframework.cloud.gateway.route.builder.routes
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.net.URI

@Component
class ProtocolRouteLocator(
    private val properties: GatewayProperties,
    private val locatorBuilder: RouteLocatorBuilder,
    private val uriProvider: UriProvider
) : RouteLocator {

    override fun getRoutes(): Flux<Route> = locatorBuilder.routes {
        val blockchain = properties.blockchain

        nftIndexerApiV1(uriProvider.getNftIndexerApiUri(blockchain))
        nftOrderApiV1(uriProvider.getNftIndexerApiUri(blockchain))
        nftOrderOriginApiV1(uriProvider.getNftOrderApiUri(blockchain))
        orderIndexerApiV1(uriProvider.getOrderIndexerApiUri(blockchain))
        erc20IndexerApiV1(uriProvider.getErc20IndexerApiUri(blockchain))
        unlockableApiV1(uriProvider.getUnlockableApiUri(blockchain))
    }.routes

    private fun RouteLocatorDsl.nftIndexerApiV1(nftIndexerApiUri: URI) {
        route("nft-indexer-api-v1-get-post") {
            path(
                "/v0.1/nft/items/*/meta",
                "/v0.1/nft/items/*/resetMeta",
                "/v0.1/nft/items/*/lazy",
                "/v0.1/nft/mints",
                "/v0.1/nft/collections/**"
            ).and(method(HttpMethod.GET, HttpMethod.POST, HttpMethod.DELETE))

            filters {
                rewritePath(
                    "/v0.1/nft/(?<segment>.*)",
                    "/v0.1/\${segment}"
                )
            }
            uri(nftIndexerApiUri)
        }
    }

    private fun RouteLocatorDsl.nftOrderApiV1(nftOrderApiUri: URI) {
        route("nft-order-api-v1-get-post") {
            path("/v0.1/nft/**").and(method(HttpMethod.GET, HttpMethod.POST))
            filters {
                rewritePath(
                    "/v0.1/nft/(?<segment>.*)",
                    "/v0.1/\${segment}"
                )
            }
            uri(nftOrderApiUri)
        }
    }

    private fun RouteLocatorDsl.nftOrderOriginApiV1(nftOrderApiUri: URI) {
        route("nft-order-api-v1-get-post-origin") {
            path("/v0.1/nft-order/**").and(method(HttpMethod.GET, HttpMethod.POST))
            filters {
                rewritePath(
                    "/v0.1/nft-order/(?<segment>.*)",
                    "/v0.1/\${segment}"
                )
            }
            uri(nftOrderApiUri)
        }
    }

    private fun RouteLocatorDsl.orderIndexerApiV1(orderIndexerApi: URI) {
        route("order-indexer-api-v1") {
            path("/v0.1/order/**").and(method(HttpMethod.GET, HttpMethod.POST))
            filters {
                rewritePath(
                    "/v0.1/order/(?<segment>.*)",
                    "/v0.1/\${segment}"
                )
            }
            uri(orderIndexerApi)
        }
    }

    private fun RouteLocatorDsl.erc20IndexerApiV1(erc20IndexerApi: URI) {
        route("erc20-indexer-api-v1-get") {
            path("/v0.1/erc20/**").and(method(HttpMethod.GET))
            filters {
                rewritePath(
                    "/v0.1/erc20/(?<segment>.*)",
                    "/v0.1/\${segment}"
                )
            }
            uri(erc20IndexerApi)
        }
    }

    private fun RouteLocatorDsl.unlockableApiV1(unlockableApi: URI) {
        route("unlockable-api-v1-legacy") {
            path("/v0.1/unlockable/**").and(method(HttpMethod.GET, HttpMethod.POST))
            filters {
                rewritePath(
                    "/v0.1/unlockable/(?<segment>.*)",
                    "/v0.1/\${segment}"
                )
            }
            uri(unlockableApi)
        }
    }
}
