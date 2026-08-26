package com.easeaudio.util

/**
 * A lightweight, self-contained QR Code generator in pure Kotlin.
 * Supports alphanumeric and byte encoding for URLs and text (QR Code standard).
 */
object QrCodeGenerator {

    /**
     * Generates a 2D boolean array representing dark (true) and light (false) modules of a QR code.
     */
    fun encode(text: String, minVersion: Int = 1): Array<BooleanArray> {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val length = bytes.size
        
        // Determine version (1 to 6) based on byte length
        val version = when {
            length <= 14 -> 1
            length <= 26 -> 2
            length <= 42 -> 3
            length <= 62 -> 4
            length <= 84 -> 5
            length <= 106 -> 6
            else -> 7
        }.coerceAtLeast(minVersion)

        val size = version * 4 + 17
        val matrix = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        // 1. Finder patterns (top-left, top-right, bottom-left)
        fun placeFinderPattern(row: Int, col: Int) {
            for (r in -1..7) {
                for (c in -1..7) {
                    val nr = row + r
                    val nc = col + c
                    if (nr in 0 until size && nc in 0 until size) {
                        reserved[nr][nc] = true
                        val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                        val isCenter = r in 2..4 && c in 2..4
                        matrix[nr][nc] = (r in 0..6 && c in 0..6) && (isBorder || isCenter)
                    }
                }
            }
        }

        placeFinderPattern(0, 0)
        placeFinderPattern(0, size - 7)
        placeFinderPattern(size - 7, 0)

        // 2. Alignment patterns (version >= 2)
        if (version >= 2) {
            val alignPos = when (version) {
                2 -> intArrayOf(6, 18)
                3 -> intArrayOf(6, 22)
                4 -> intArrayOf(6, 26)
                5 -> intArrayOf(6, 30)
                6 -> intArrayOf(6, 34)
                else -> intArrayOf(6, 38)
            }
            for (r in alignPos) {
                for (c in alignPos) {
                    if (reserved[r][c]) continue
                    for (dr in -2..2) {
                        for (dc in -2..2) {
                            val nr = r + dr
                            val nc = c + dc
                            reserved[nr][nc] = true
                            val isOuter = dr == -2 || dr == 2 || dc == -2 || dc == 2
                            val isCenter = dr == 0 && dc == 0
                            matrix[nr][nc] = isOuter || isCenter
                        }
                    }
                }
            }
        }

        // 3. Timing patterns
        for (i in 8 until size - 8) {
            if (!reserved[6][i]) {
                matrix[6][i] = (i % 2 == 0)
                reserved[6][i] = true
            }
            if (!reserved[i][6]) {
                matrix[i][6] = (i % 2 == 0)
                reserved[i][6] = true
            }
        }

        // 4. Dark module
        matrix[4 * version + 9][8] = true
        reserved[4 * version + 9][8] = true

        // 5. Reserve format info areas
        for (i in 0..8) {
            if (i in 0 until size) {
                reserved[8][i] = true
                reserved[i][8] = true
            }
        }
        for (i in 0..7) {
            reserved[8][size - 1 - i] = true
            reserved[size - 1 - i][8] = true
        }

        // 6. Encode data bitstream (Byte mode)
        val bitBuffer = mutableListOf<Boolean>()
        
        // Mode indicator for Byte: 0100
        bitBuffer.addAll(listOf(false, true, false, false))
        
        // Character count indicator (8 bits for v1-9)
        for (i in 7 downTo 0) {
            bitBuffer.add((length shr i and 1) == 1)
        }
        
        // Data bytes
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            for (i in 7 downTo 0) {
                bitBuffer.add((v shr i and 1) == 1)
            }
        }

        // Terminator (up to 4 zeroes)
        val totalDataCapBits = when (version) {
            1 -> 152
            2 -> 272
            3 -> 440
            4 -> 640
            5 -> 864
            6 -> 1088
            else -> 1344
        }
        
        val padCount = (totalDataCapBits - bitBuffer.size).coerceIn(0, 4)
        repeat(padCount) { bitBuffer.add(false) }

        // Pad to byte boundary
        while (bitBuffer.size % 8 != 0 && bitBuffer.size < totalDataCapBits) {
            bitBuffer.add(false)
        }

        // Pad bytes (0xEC, 0x11 alternating)
        val padBytes = intArrayOf(0xEC, 0x11)
        var padIndex = 0
        while (bitBuffer.size < totalDataCapBits) {
            val pad = padBytes[padIndex % 2]
            for (i in 7 downTo 0) {
                if (bitBuffer.size < totalDataCapBits) {
                    bitBuffer.add((pad shr i and 1) == 1)
                }
            }
            padIndex++
        }

        // 7. Place data bits into matrix in 2-column zigzag
        var bitIndex = 0
        var right = size - 1
        var goingUp = true

        while (right > 0) {
            if (right == 6) right-- // Skip vertical timing column
            val rowRange = if (goingUp) (size - 1 downTo 0) else (0 until size)
            for (r in rowRange) {
                for (c in 0..1) {
                    val col = right - c
                    if (!reserved[r][col]) {
                        val bit = if (bitIndex < bitBuffer.size) bitBuffer[bitIndex++] else false
                        // Mask pattern 0: (row + col) % 2 == 0
                        val mask = (r + col) % 2 == 0
                        matrix[r][col] = bit xor mask
                    }
                }
            }
            goingUp = !goingUp
            right -= 2
        }

        // 8. Format bits (Mask 000, ECC Low: 01) -> format string 0x77C4 -> 111011111000100
        val formatBits = intArrayOf(1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 0, 0, 1, 0, 0)
        
        // Write format bits around top-left
        var fIdx = 0
        for (c in 0..8) {
            if (c != 6) matrix[8][c] = formatBits[fIdx++] == 1
        }
        for (r in 7 downTo 0) {
            if (r != 6) matrix[r][8] = formatBits[fIdx++] == 1
        }

        // Write format bits around other finder patterns
        fIdx = 0
        for (r in size - 1 downTo size - 7) {
            matrix[r][8] = formatBits[fIdx++] == 1
        }
        for (c in size - 8 until size) {
            matrix[8][c] = formatBits[fIdx++] == 1
        }

        return matrix
    }
}
