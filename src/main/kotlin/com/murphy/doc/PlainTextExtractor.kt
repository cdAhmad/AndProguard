package com.murphy.doc

import java.util.regex.Pattern

object PlainTextExtractor {
    // 预编译正则表达式，避免重复编译
    private val STYLE_TAG_PATTERN: Pattern = Pattern.compile("<style[^>]*>.*?</style>", Pattern.DOTALL)
    private val HTML_TAG_PATTERN: Pattern = Pattern.compile("<[^>]+>")
    private val ICON_PATTERN: Pattern = Pattern.compile("<icon[^>]*>")
    private val WHITESPACE_PATTERN: Pattern = Pattern.compile("\\s+")

    // HTML实体映射
    private val HTML_ENTITIES = arrayOf<Array<String?>?>(
        arrayOf<String?>("&lt;", "<"),
        arrayOf<String?>("&gt;", ">"),
        arrayOf<String?>("&amp;", "&"),
        arrayOf<String?>("&nbsp;", " "),
        arrayOf<String?>("&quot;", "\""),
        arrayOf<String?>("&apos;", "'")
    )

    fun extractPlainText(htmlContent: String?): String {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return ""
        }

        var text: String? = htmlContent

        // 1. 移除style标签及其内容
        text = STYLE_TAG_PATTERN.matcher(text).replaceAll("")

        // 2. 移除icon标签
        text = ICON_PATTERN.matcher(text).replaceAll("")

        // 3. 移除所有HTML标签
        text = HTML_TAG_PATTERN.matcher(text).replaceAll(" ")

        // 4. 替换HTML实体
        for (entity in HTML_ENTITIES) {
            text = text!!.replace(entity!![0]!!, entity[1]!!)
        }

        // 5. 压缩空白字符
        text = WHITESPACE_PATTERN.matcher(text).replaceAll(" ")

        return text.trim { it <= ' ' }
    }

    // 极致性能：针对你的特定HTML结构优化的版本
    fun extractPlainTextOptimized(htmlContent: String?): String {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return ""
        }

        // 直接提取关键部分，避免处理整个HTML
        val result = StringBuilder()

        // 提取pre块内容（函数签名）
        val preStart = htmlContent.indexOf("<pre>")
        val preEnd = htmlContent.indexOf("</pre>", preStart + 5)
        if (preStart != -1 && preEnd != -1) {
            val preContent = htmlContent.substring(preStart + 5, preEnd)

            // 手动解析，避免正则开销
            var inTag = false
            for (i in 0..<preContent.length) {
                val c = preContent.get(i)
                if (c == '<') {
                    inTag = true
                } else if (c == '>') {
                    inTag = false
                } else if (!inTag) {
                    result.append(c)
                } else {
                    // 在标签内，跳过
                }
            }
            result.append("\n\n")
        }

        // 提取bottom块内容（包名和文件名）
        val bottomStart = htmlContent.indexOf("<div class='bottom'>")
        if (bottomStart != -1) {
            val bottomEnd = htmlContent.indexOf("</div>", bottomStart)
            if (bottomEnd != -1) {
                val bottomContent = htmlContent.substring(bottomStart + 20, bottomEnd)

                // 手动解析bottom内容
                var inTag = false
                for (i in 0..<bottomContent.length) {
                    val c = bottomContent.get(i)
                    if (c == '<') {
                        inTag = true
                    } else if (c == '>') {
                        inTag = false
                    } else if (!inTag) {
                        result.append(c)
                    }
                }
            }
        }

        // 压缩空白
        return result.toString().replace("\\s+".toRegex(), " ").trim { it <= ' ' }
    }
}