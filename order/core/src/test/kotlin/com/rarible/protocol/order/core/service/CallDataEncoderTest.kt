package com.rarible.protocol.order.core.service

import com.rarible.protocol.order.core.model.Transfer
import io.daonomic.rpc.domain.Binary
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigInteger

internal class CallDataEncoderTest {
    private val callDataEncoder = CallDataEncoder()

    @Test
    fun `should encode sell ERC721 callData`() {
        val transfer = Transfer.Erc721Transfer(
            from = Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
            to = Address.ZERO(),
            tokenId = BigInteger.valueOf(110711),
            safe = false
        )
        val transferCallData = callDataEncoder.encodeTransferCallData(transfer)
        assertThat(transferCallData.callData).isEqualTo(Binary.apply("0x23b872dd00000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b60000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001b077"))
        assertThat(transferCallData.replacementPattern).isEqualTo(Binary.apply("0x000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0000000000000000000000000000000000000000000000000000000000000000"))
    }

    @Test
    fun `should decode sell ERC721 callData`() {
        val transfer = Transfer.Erc721Transfer(
            from = Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
            to = Address.ZERO(),
            tokenId = BigInteger.valueOf(110711),
            safe = false
        )
        val decodedTransfer = callDataEncoder.decodeTransfer(Binary.apply("0x23b872dd00000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b60000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001b077"))
        assertThat(decodedTransfer).isEqualTo(transfer)
    }

    @Test
    fun `should encode buy ERC721 callData`() {
        val transfer = Transfer.Erc721Transfer(
            from = Address.ZERO(),
            to = Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
            tokenId = BigInteger.valueOf(110711),
            safe = false
        )
        val transferCallData = callDataEncoder.encodeTransferCallData(transfer)
        assertThat(transferCallData.callData).isEqualTo(Binary.apply("0x23b872dd000000000000000000000000000000000000000000000000000000000000000000000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b6000000000000000000000000000000000000000000000000000000000001b077"))
        assertThat(transferCallData.replacementPattern).isEqualTo(Binary.apply("0x00000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"))
    }

    @Test
    fun `should decode buy ERC721 callData`() {
        val transfer = Transfer.Erc721Transfer(
            from = Address.ZERO(),
            to = Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
            tokenId = BigInteger.valueOf(110711),
            safe = false
        )
        val decodedTransfer = callDataEncoder.decodeTransfer(Binary.apply("0x23b872dd000000000000000000000000000000000000000000000000000000000000000000000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b6000000000000000000000000000000000000000000000000000000000001b077"))
        assertThat(decodedTransfer).isEqualTo(transfer)
    }

    @Test
    fun `should encode sell ERC1155 callData`() {
        val transfer = Transfer.Erc1155Transfer(
            from = Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
            to = Address.ZERO(),
            tokenId = BigInteger.valueOf(110249),
            value = BigInteger.valueOf(3),
            data = Binary.apply(ByteArray(0))
        )
        val transferCallData = callDataEncoder.encodeTransferCallData(transfer)
        assertThat(transferCallData.callData).isEqualTo(Binary.apply("0xf242432a00000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b60000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001aea9000000000000000000000000000000000000000000000000000000000000000300000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000000000000000"))
        assertThat(transferCallData.replacementPattern).isEqualTo(Binary.apply("0x000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"))
    }

    @Test
    fun `should decode sell ERC1155 callData`() {
        val transfer = Transfer.Erc1155Transfer(
            from = Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
            to = Address.ZERO(),
            tokenId = BigInteger.valueOf(110249),
            value = BigInteger.valueOf(3),
            data = Binary.apply(ByteArray(0))
        )
        val decodedTransfer = callDataEncoder.decodeTransfer(Binary.apply("0xf242432a00000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b60000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001aea9000000000000000000000000000000000000000000000000000000000000000300000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000000000000000"))
        assertThat(decodedTransfer).isEqualTo(transfer)
    }

    @Test
    fun `should encode buy ERC1155 callData`() {
        val transfer = Transfer.Erc1155Transfer(
            from = Address.ZERO(),
            to = Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
            tokenId = BigInteger.valueOf(110249),
            value = BigInteger.valueOf(2),
            data = Binary.apply(ByteArray(0))
        )
        val transferCallData = callDataEncoder.encodeTransferCallData(transfer)
        assertThat(transferCallData.callData).isEqualTo(Binary.apply("0xf242432a000000000000000000000000000000000000000000000000000000000000000000000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b6000000000000000000000000000000000000000000000000000000000001aea9000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000000000000000"))
        assertThat(transferCallData.replacementPattern).isEqualTo(Binary.apply("0x00000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"))
    }

    @Test
    fun `should decode buy ERC1155 callData`() {
        val transfer = Transfer.Erc1155Transfer(
            from = Address.ZERO(),
            to = Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
            tokenId = BigInteger.valueOf(110249),
            value = BigInteger.valueOf(2),
            data = Binary.apply(ByteArray(0))
        )
        val decodedTransfer = callDataEncoder.decodeTransfer(Binary.apply("0xf242432a000000000000000000000000000000000000000000000000000000000000000000000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b6000000000000000000000000000000000000000000000000000000000001aea9000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000000000000000"))
        assertThat(decodedTransfer).isEqualTo(transfer)
    }
}
