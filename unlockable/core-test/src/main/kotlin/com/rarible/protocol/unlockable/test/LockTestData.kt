package com.rarible.protocol.unlockable.test

import com.rarible.ethereum.common.toBinary
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.unlockable.util.LockMessageUtil
import com.rarible.protocol.unlockable.util.SignUtil
import io.daonomic.rpc.domain.Binary
import org.web3j.crypto.Sign
import scalether.domain.Address
import java.math.BigInteger

class LockTestData(
    val nftItem: NftItemDto,
    val lockContent: String,
    val ownerAddress: Address,
    val privateKey: BigInteger,
    val publicKey: BigInteger
) {

    fun getItemUnlockSignature(): Binary {

        val lockMessage = LockMessageUtil.getUnlockMessage(nftItem.contract, EthUInt256(nftItem.tokenId))

        return Sign.signMessage(
            SignUtil.addStart(lockMessage).bytes(),
            publicKey,
            privateKey
        ).toBinary()
    }

    fun getItemLockSignature(): Binary {

        val lockMessage =
            LockMessageUtil.getLockMessage(nftItem.contract, EthUInt256(nftItem.tokenId), lockContent)

        return Sign.signMessage(
            SignUtil.addStart(lockMessage).bytes(),
            publicKey,
            privateKey
        ).toBinary()
    }
}

