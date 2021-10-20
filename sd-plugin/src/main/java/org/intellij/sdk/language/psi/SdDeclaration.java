package org.intellij.sdk.language.psi;

import com.intellij.navigation.NavigationItem;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;

/**
 * This interface represents a declaration in the SD language.
 * @author shahariel
 */
public interface SdDeclaration extends PsiElement, PsiNamedElement, NavigationItem {
    String getName();
    
    String getTypeName();
    
    SdDeclarationType getType();

}