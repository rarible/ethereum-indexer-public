package com.rarible.protocol.order.api.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.api.exceptions.ValidationApiException
import com.rarible.protocol.order.api.service.order.OrderService
import com.rarible.protocol.order.core.converters.model.AssetTypeConverter
import com.rarible.protocol.order.core.converters.model.OrderDataConverter
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.Order.Companion.legacyMessage
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.toOrderExactFields
import com.rarible.protocol.order.core.service.TransferProxyService
import io.daonomic.rpc.domain.Binary
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import scalether.domain.Address

@RestController
class EncodeController(
    private val orderService: OrderService,
    private val transferProxyService: TransferProxyService,
    eip712Domain: EIP712Domain,
    mapper: ObjectMapper
) : OrderEncodeControllerApi {

    private val factory = JsonNodeFactory.instance
    private val types = mapper.readValue<ObjectNode>(TYPES)
    private val eip712DomainDto = with(eip712Domain) {
        EIP712DomainDto(
            name = name,
            chainId = chainId.toInt(),
            verifyingContract = verifyingContract,
            version = version
        )
    }

    override suspend fun encodeOrder(form: OrderFormDto): ResponseEntity<EncodedOrderDto> {
        val order = orderService.convertFormToVersion(form).toOrderExactFields()
        val signMessage = when (order.type) {
            OrderType.RARIBLE_V1 -> {
                TextSignMessageDto(message = order.legacyMessage())
            }
            OrderType.RARIBLE_V2 -> {
                val struct: ObjectNode = with(order) {
                    factory.objectNode()
                        .put("maker", maker.toString())
                        .set<ObjectNode>("makeAsset", make.toJson())
                        .put("taker", (taker ?: Address.ZERO()).toString())
                        .set<ObjectNode>("takeAsset", take.toJson())
                        .put("salt", salt.value.toString())
                        .put("start", start ?: 0L)
                        .put("end", end ?: 0L)
                        .put("dataType", Binary.apply(data.getDataVersion()).toString())
                        .put("data", data.toEthereum().toString())
                }
                EIP712SignMessageDto(
                    eip712DomainDto,
                    struct,
                    "Order",
                    types
                )
            }
            OrderType.OPEN_SEA_V1 -> throw ValidationApiException("Unsupported order type ${order.type}")
            OrderType.CRYPTO_PUNKS -> throw ValidationApiException("CryptoPunks are not supported")
        }
        val encodedOrder = EncodedOrderDto(transferProxyService.getTransferProxy(order.make.type), signMessage)
        return ResponseEntity.ok(encodedOrder)
    }

    override suspend fun encodeOrderAssetType(
        @RequestBody assetType: AssetTypeDto
    ): ResponseEntity<EncodedOrderDataDto> {
        val source = AssetTypeConverter.convert(assetType)
        val encodedOrderData = EncodedOrderDataDto(source.type, source.data)
        return ResponseEntity.ok(encodedOrderData)
    }

    override suspend fun encodeOrderData(
        @RequestBody data: OrderDataDto
    ): ResponseEntity<EncodedOrderDataDto> {
        val source = OrderDataConverter.convert(data)
        val encodedOrderData = EncodedOrderDataDto(
            Binary.apply(source.getDataVersion()),
            source.toEthereum()
        )
        return ResponseEntity.ok(encodedOrderData)
    }

    private fun Asset.toJson(): ObjectNode =
        factory.objectNode()
            .set<ObjectNode>("assetType", type.toJson())
            .put("value", value.value.toString())

    private fun AssetType.toJson(): ObjectNode =
        factory.objectNode()
            .put("assetClass", type.toString())
            .put("data", data.toString())

    companion object {
        const val TYPES = """
            {
               "AssetType":[
                  {
                     "name":"assetClass",
                     "type":"bytes4"
                  },
                  {
                     "name":"data",
                     "type":"bytes"
                  }
               ],
               "Asset":[
                  {
                     "name":"assetType",
                     "type":"AssetType"
                  },
                  {
                     "name":"value",
                     "type":"uint256"
                  }
               ],
               "Order":[
                  {
                     "name":"maker",
                     "type":"address"
                  },
                  {
                     "name":"makeAsset",
                     "type":"Asset"
                  },
                  {
                     "name":"taker",
                     "type":"address"
                  },
                  {
                     "name":"takeAsset",
                     "type":"Asset"
                  },
                  {
                     "name":"salt",
                     "type":"uint256"
                  },
                  {
                     "name":"start",
                     "type":"uint256"
                  },
                  {
                     "name":"end",
                     "type":"uint256"
                  },
                  {
                     "name":"dataType",
                     "type":"bytes4"
                  },
                  {
                     "name":"data",
                     "type":"bytes"
                  }
               ]
            }
        """
    }
}
