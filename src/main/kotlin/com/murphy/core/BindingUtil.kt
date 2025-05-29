package com.murphy.core

import com.android.tools.idea.databinding.module.LayoutBindingModuleCache
import com.android.tools.idea.databinding.util.DataBindingUtil
import com.android.tools.idea.res.psi.ResourceReferencePsiElement
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.murphy.util.PLUGIN_NAME
import org.jetbrains.android.facet.AndroidFacet

fun ResourceReferencePsiElement.findIdReference(scope: SearchScope): List<PsiReference>? {
    val androidFacet = AndroidFacet.getInstance(delegate) ?: return null
    val bindingModuleCache = LayoutBindingModuleCache.getInstance(androidFacet)
    val groups = bindingModuleCache.bindingLayoutGroups
    val fieldName = DataBindingUtil.convertAndroidIdToJavaFieldName(resourceReference.name)
    return groups.flatMap { group -> bindingModuleCache.getLightBindingClasses { group == it } }
        .mapNotNull { it -> it.allFields.find { field -> field.name == fieldName } }
        .map { ReferencesSearch.search(it, scope).findAll() }
        .flatten()
}

fun ResourceReferencePsiElement.findLayoutReference(scope: SearchScope): List<PsiReference>? {
    val androidFacet = AndroidFacet.getInstance(delegate) ?: return null
    val bindingModuleCache = LayoutBindingModuleCache.getInstance(androidFacet)
    val groups = bindingModuleCache.bindingLayoutGroups
    val className = DataBindingUtil.convertFileNameToJavaClassName(resourceReference.name) + "Binding"
    val layoutGroup = groups.firstOrNull { it.mainLayout.className == className }
    return bindingModuleCache.getLightBindingClasses { it == layoutGroup }
        .map { ReferencesSearch.search(it, scope).findAll() }
        .flatten()
}

fun List<PsiReference>.handleReferenceRename(project: Project, newRefName: String) {
    WriteCommandAction.writeCommandAction(project)
        .withName(PLUGIN_NAME)
        .run<Throwable> {
            forEach { it.handleElementRename(newRefName) }
        }
}