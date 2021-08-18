package com.rarible.protocol.unlockable.util

import com.rarible.ethereum.common.generateNewKeys
import com.rarible.ethereum.common.toBinary
import com.rarible.ethereum.domain.EthUInt256
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.web3j.crypto.Sign
import scalether.domain.Address

internal class SignUtilTest {

    @Test
    fun `should recover the signed message`() {
        val (privateKey, publicKey, ownerAddress) = generateNewKeys()
        val signature = Sign.signMessage(
            SignUtil.addStart(
                LockMessageUtil.getLockMessage(
                    Address.ONE(),
                    EthUInt256.TEN,
                    "ipfs://content"
                )
            ).bytes(), publicKey, privateKey
        ).toBinary()

        val signer = SignUtil.recover(
            LockMessageUtil.getLockMessage(
                Address.ONE(),
                EthUInt256.TEN,
                "ipfs://content"
            ),
            signature
        )

        Assertions.assertEquals(ownerAddress, signer)
    }
}
