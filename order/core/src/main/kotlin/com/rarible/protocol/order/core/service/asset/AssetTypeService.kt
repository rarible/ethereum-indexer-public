package com.rarible.protocol.order.core.service.asset

import com.rarible.core.contract.model.ContractType
import com.rarible.ethereum.contract.service.ContractService
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import org.springframework.stereotype.Service
import scalether.domain.Address

@Service
class AssetTypeService(
    private val contractService: ContractService
) {
    suspend fun toAssetType(token: Address, tokenId: EthUInt256): AssetType {
        return if (token == Address.ZERO()) {
            EthAssetType
        } else {
            val contract = contractService.get(token)
            when (contract.type) {
                ContractType.ERC721_TOKEN -> Erc721AssetType(token, tokenId)
                ContractType.ERC1155_TOKEN -> Erc1155AssetType(token, tokenId)
                ContractType.ERC20_TOKEN -> Erc20AssetType(token)
            }
        }
    }
}
