package com.murphy.core

import com.murphy.config.AndConfigState
import java.util.Random
import java.util.zip.CRC32
import kotlin.text.iterator


val map = mutableMapOf<String, AdaptiveRotatingObfuscator>()
fun obfuscator(config: AndConfigState): AdaptiveRotatingObfuscator {
    val seed=config.getSeed()
  return  map.getOrPut(seed) {
        AdaptiveRotatingObfuscator(seed)
    }
}

class AdaptiveRotatingObfuscator constructor(
    private val seed: String
) {
    // 字符集（不变）
    private companion object {
        val UPPER_CHARS = ('A'..'Z').toList()
        val LOWER_CHARS = ('a'..'z').toList()
        val DIGIT_CHARS = ('0'..'9').toList()

        const val DIGITS = "0123456789"
        const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
        const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

        // 预建索引映射（静态）
        val UPPER_INDEX_MAP = UPPER_CHARS.withIndex().associate { it.value to it.index }
        val LOWER_INDEX_MAP = LOWER_CHARS.withIndex().associate { it.value to it.index }
        val DIGIT_INDEX_MAP = DIGIT_CHARS.withIndex().associate { it.value to it.index }
    }

    // 根据 seed 动态生成初始映射表（每个实例独立）
    private val initialUpperMap = shuffleWithSeed(UPPER_CHARS, seed + "UPPER")
    private val initialLowerMap = shuffleWithSeed(LOWER_CHARS, seed + "LOWER")
    private val initialDigitMap = shuffleWithSeed(DIGIT_CHARS, seed + "DIGIT")

    // ===== 内部数据类 & 工具函数（保持不变）=====
    private data class CharProfile(
        val hasLower: Boolean,
        val hasUpper: Boolean,
        val hasDigit: Boolean
    )

    private fun analyze(input: String): CharProfile {
        var hasLower = false
        var hasUpper = false
        var hasDigit = false
        for (c in input) {
            when {
                c.isLowerCase() -> hasLower = true
                c.isUpperCase() -> hasUpper = true
                c.isDigit() -> hasDigit = true
            }
            if (hasLower && hasUpper && hasDigit) break
        }
        return CharProfile(hasLower, hasUpper, hasDigit)
    }

    private fun selectChecksumCharset(profile: CharProfile): String = when {
        profile.hasLower && !profile.hasUpper && !profile.hasDigit -> LOWERCASE
        profile.hasLower && !profile.hasUpper && profile.hasDigit -> "$LOWERCASE$DIGITS"
        profile.hasUpper && !profile.hasLower && !profile.hasDigit -> UPPERCASE
        profile.hasUpper && !profile.hasLower && profile.hasDigit -> "$UPPERCASE$DIGITS"
        else -> "$UPPERCASE$LOWERCASE$DIGITS"
    }

    private fun computeChecksum(plaintext: String, charset: String): String {
        val crc = CRC32()
        crc.update(plaintext.toByteArray(Charsets.UTF_8))
        val hash = crc.value.toInt()
        val base = charset.length
        val maxVal = base * base
        val value = ((hash % maxVal) + maxVal) % maxVal
        return buildString {
            append(charset[value / base])
            append(charset[value % base])
        }
    }

    // ===== 对外接口 =====
    fun obfuscate(plaintext: String): String {
        if (plaintext.isEmpty()) return plaintext
        val core = obfuscateCore(plaintext)
        val profile = analyze(plaintext)
        val checksumCharset = selectChecksumCharset(profile)
        val checksum = computeChecksum(plaintext, checksumCharset)
        return core + checksum
    }

    fun deobfuscate(maybeObfuscated: String): String {
        if (maybeObfuscated.length < 2) return maybeObfuscated

        val last2 = maybeObfuscated.takeLast(2)
        val core = maybeObfuscated.dropLast(2)

        return try {
            val restored = deobfuscateCore(core)
            val profile = analyze(restored)
            val expectedCharset = selectChecksumCharset(profile)
            if (last2.all { it in expectedCharset }) {
                val expectedChecksum = computeChecksum(restored, expectedCharset)
                if (last2 == expectedChecksum) restored else maybeObfuscated
            } else {
                maybeObfuscated
            }
        } catch (e: Exception) {
            maybeObfuscated
        }
    }

    // ===== 核心逻辑（使用实例成员 initialXxxMap）=====
    private fun obfuscateCore(input: String): String {
        var upperOffset = 0
        var lowerOffset = 0
        var digitOffset = 0
        val sb = StringBuilder(input.length)

        for (char in input) {
            if (char.isLetterOrDigit()) {
                val obfuscatedChar = when {
                    char.isUpperCase() -> {
                        val idx = UPPER_INDEX_MAP[char]!!
                        val mapped = initialUpperMap[(idx + upperOffset) % 26]
                        upperOffset = (upperOffset + UPPER_INDEX_MAP[mapped]!!) % 26
                        mapped
                    }

                    char.isLowerCase() -> {
                        val idx = LOWER_INDEX_MAP[char]!!
                        val mapped = initialLowerMap[(idx + lowerOffset) % 26]
                        lowerOffset = (lowerOffset + LOWER_INDEX_MAP[mapped]!!) % 26
                        mapped
                    }

                    char.isDigit() -> {
                        val idx = DIGIT_INDEX_MAP[char]!!
                        val mapped = initialDigitMap[(idx + digitOffset) % 10]
                        digitOffset = (digitOffset + DIGIT_INDEX_MAP[mapped]!!) % 10
                        mapped
                    }

                    else -> char
                }
                sb.append(obfuscatedChar)
            } else {
                sb.append(char)
            }
        }
        return sb.toString()
    }

    private fun deobfuscateCore(obfuscated: String): String {
        var upperOffset = 0
        var lowerOffset = 0
        var digitOffset = 0
        val sb = StringBuilder(obfuscated.length)

        for (char in obfuscated) {
            if (char.isLetterOrDigit()) {
                val originalChar = when {
                    char.isUpperCase() -> {
                        val currentMapIdx = (initialUpperMap.indexOf(char) - upperOffset + 26) % 26
                        val original = UPPER_CHARS[currentMapIdx]
                        upperOffset = (upperOffset + UPPER_INDEX_MAP[char]!!) % 26
                        original
                    }

                    char.isLowerCase() -> {
                        val currentMapIdx = (initialLowerMap.indexOf(char) - lowerOffset + 26) % 26
                        val original = LOWER_CHARS[currentMapIdx]
                        lowerOffset = (lowerOffset + LOWER_INDEX_MAP[char]!!) % 26
                        original
                    }

                    char.isDigit() -> {
                        val currentMapIdx = (initialDigitMap.indexOf(char) - digitOffset + 10) % 10
                        val original = DIGIT_CHARS[currentMapIdx]
                        digitOffset = (digitOffset + DIGIT_INDEX_MAP[char]!!) % 10
                        original
                    }

                    else -> char
                }
                sb.append(originalChar)
            } else {
                sb.append(char)
            }
        }
        return sb.toString()
    }
    /**
     * 启发式判断字符串是否是由当前 obfuscator 实例混淆过的。
     *
     * 判断逻辑：
     * 1. 长度至少为 2
     * 2. 尝试用当前 seed 解混淆
     * 3. 若解混淆成功且 checksum 验证通过，则认为是混淆串
     *
     * 注意：该方法可能误判（如巧合通过 checksum 的普通字符串），
     * 但在实践中，checksum 碰撞概率低（≤ 1/676），可接受。
     */
    fun isObfuscated(maybeObfuscated: String): Boolean {
        if (maybeObfuscated.length < 2) return false

        val last2 = maybeObfuscated.takeLast(2)
        val core = maybeObfuscated.dropLast(2)

        return try {
            // 尝试解混淆核心部分
            val restored = deobfuscateCore(core)
            val profile = analyze(restored)
            val expectedCharset = selectChecksumCharset(profile)

            // 检查最后两位是否在预期字符集中
            if (!last2.all { it in expectedCharset }) {
                return false
            }

            // 重新计算 checksum 并验证
            val expectedChecksum = computeChecksum(restored, expectedCharset)
            last2 == expectedChecksum
        } catch (e: Exception) {
            false // 解混淆失败，视为未混淆
        }
    }
    // ===== 工具函数 =====
    private fun <T> shuffleWithSeed(list: List<T>, seed: String): List<T> {
        val random = Random(seed.hashCode().toLong())
        val result = list.toMutableList()
        for (i in result.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            result[i] = result[j].also { result[j] = result[i] }
        }
        return result
    }


}
