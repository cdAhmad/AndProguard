package com.murphy.core

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringFactory
import com.murphy.config.AndConfigState
import com.murphy.util.LogUtil

interface RenamableElement<T : PsiElement> {
    val element: T
    val namingIndex: Int?
    val currentName: String?
    fun performRename(project: Project, name: AndConfigState)

    fun runRename(project: Project, psiElement: PsiElement, newName: String) {
        LogUtil.info(project, "[${psiElement.javaClass.simpleName}] $currentName >>> $newName")
        RefactoringFactory.getInstance(project)
            .createRename(psiElement, newName, false, false)
            .run()
    }
}