package com.danzabot.stepcounter

object Fx {
    fun rnd(seed: Long): Float {
        var x = seed * -0x61c8864680b583ebL
        x = (x xor (x ushr 31)) * -0x4b47d5b1b9fc43b7L
        x = x xor (x ushr 29)
        return Math.floorMod(x, 10_000L) / 10_000f
    }
}

object Weather {
    fun kind(code: Int) = when (code) {
        0, 1          -> "sun"
        2             -> "partly"
        45, 48        -> "fog"
        in 51..67, in 80..82 -> "rain"
        in 71..77, 85, 86    -> "snow"
        in 95..99     -> "storm"
        else          -> "cloud"
    }
}
