package com.murphy.doc


import com.android.tools.r8.R8
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.lang.java.JavaDocumentationProvider
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.murphy.config.AndConfigState
import com.murphy.core.obfuscator
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.idea.KotlinDocumentationProvider
import org.jetbrains.kotlin.psi.KtElement
import java.util.function.Predicate


class AppendDocProvider : DocumentationProvider {
    val config = AndConfigState.getInstance()
    val kotlinDocumentationProvider = KotlinDocumentationProvider()
    override fun generateDoc(element: PsiElement, @Nullable originalElement: PsiElement?): String? {
        println("generateDoc ${element.javaClass.name}")
        val base = kotlinDocumentationProvider.generateDoc(element, originalElement)
        if (base.isNullOrEmpty()) return null
        // 为Java方法追加文档
        val stringBuilder = StringBuilder()
        val list = PlainTextExtractor.extractPlainText(base).split(" ").filter { it.length > 2 }.mapNotNull {
            if (obfuscator(config).isObfuscated(it)) {
                it to obfuscator(config).deobfuscate(it)
            } else {
                null
            }
        }.map { item ->
            "<tr><td>${item.first}</td><td>${item.second}<td></tr>"
        }
        if (list.isNotEmpty()) {
            stringBuilder.append("<table> <tr><td>混淆值</td> <td>原值</td></tr>")
            list.forEach {
                stringBuilder.append(it)
            }
            stringBuilder.append("</table><br>")
        }
        stringBuilder.append(base)
        return stringBuilder.toString()
    }

    override fun getQuickNavigateInfo(element: PsiElement, @Nullable originalElement: PsiElement?): String? {
        println("getQuickNavigateInfo ${element.javaClass.name}")
        return null // 使用默认
    }

    override fun generateHoverDoc(element: PsiElement, originalElement: PsiElement?): String? {
        println("generateHoverDoc ${element.javaClass.name}")
        return super.generateHoverDoc(element, originalElement)
    }

    override fun generateRenderedDoc(comment: PsiDocCommentBase): String? {
        println("generateRenderedDoc ${comment.javaClass.name}")
        return super.generateRenderedDoc(comment)
    }

}