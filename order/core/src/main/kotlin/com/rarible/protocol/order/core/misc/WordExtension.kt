package com.rarible.protocol.order.core.misc

import io.daonomic.rpc.domain.Word
import scalether.abi.Uint256Type
import java.math.BigInteger

fun BigInteger.toWord(): Word = Word(Uint256Type.encode(this).bytes())

fun zeroWord(): Word = Word(ByteArray(32))
