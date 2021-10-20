package org.intellij.sdk.language.findUsages;

import com.intellij.lang.cacheBuilder.DefaultWordsScanner;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.tree.TokenSet;
import org.intellij.sdk.language.SdLexerAdapter;
import org.intellij.sdk.language.psi.SdDeclaration;
import org.intellij.sdk.language.psi.SdIdentifier;
import org.intellij.sdk.language.psi.SdIdentifierVal;
import org.intellij.sdk.language.psi.SdIdentifierWithDashVal;
import org.intellij.sdk.language.psi.SdTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class is used for the extension (in plugin.xml), to enable "find Usages" window using the plugin code.
 * @author shahariel
 */
public class SdFindUsagesProvider implements FindUsagesProvider {
    @Nullable
    @Override
    public WordsScanner getWordsScanner() {
        return new DefaultWordsScanner(new SdLexerAdapter(),
                                       TokenSet.create(SdTypes.ID_REG, SdTypes.ID_WITH_DASH_REG, SdTypes.IDENTIFIER_VAL,
                                                       SdTypes.IDENTIFIER_WITH_DASH_VAL),
                                       TokenSet.create(SdTypes.COMMENT),
                                       TokenSet.create(SdTypes.STRING, SdTypes.INTEGER_REG, SdTypes.FLOAT_REG));
    }

    @Override
    public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
        return psiElement instanceof PsiNamedElement;
    }

    @Nullable
    @Override
    public String getHelpId(@NotNull PsiElement psiElement) {
        return null;
    }

    @NotNull
    @Override
    public String getType(@NotNull PsiElement element) {
        if (element instanceof SdDeclaration) {
            return ((SdDeclaration) element).getTypeName();
        } else {
            return "";
        }
    }

    @NotNull
    @Override
    public String getDescriptiveName(@NotNull PsiElement element) {
        return "";
    }

    @NotNull
    @Override
    public String getNodeText(@NotNull PsiElement element, boolean useFullName) {
        if (element instanceof SdIdentifierVal || element instanceof SdIdentifierWithDashVal) {
            return ((SdIdentifier) element).getName();
        } else if (element instanceof SdDeclaration) {
            String fullText = element.getNode().getText();
            return fullText.substring(0, fullText.indexOf('{'));
        } else {
            return "";
        }
    }
}