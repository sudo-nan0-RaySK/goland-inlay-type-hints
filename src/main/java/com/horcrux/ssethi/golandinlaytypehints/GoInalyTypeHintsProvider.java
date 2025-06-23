package com.horcrux.ssethi.golandinlaytypehints;

import com.goide.GoLanguage;
import com.goide.psi.GoFunctionType;
import com.goide.psi.GoType;
import com.goide.psi.GoVarDefinition;
import com.goide.psi.GoVarSpec;
import com.google.common.collect.Streams;
import com.intellij.codeInsight.hints.declarative.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import groovy.util.logging.Slf4j;
import kotlin.Unit;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
public class GoInalyTypeHintsProvider implements InlayHintsProvider {
    public static final String PROVIDER_ID = "go.type.hints.provider";
    private static final Logger log = LoggerFactory.getLogger(GoInalyTypeHintsProvider.class);

    @Override
    public @Nullable InlayHintsCollector createCollector(@NotNull PsiFile psiFile, @NotNull Editor editor) {
        return new GoInlayTypeHintsCollector();
    }

    private static class GoInlayTypeHintsCollector implements SharedBypassCollector {

        @Override
        public void collectFromElement(@NotNull PsiElement psiElement, @NotNull InlayTreeSink inlayTreeSink) {
            if (psiElement.getLanguage().is(GoLanguage.INSTANCE) && psiElement instanceof GoVarSpec goVarSpec) {

                List<String> typeNames = goVarSpec.getExpressionList().stream()
                        .map(exper -> exper.getGoType(null))
                        .filter(Objects::nonNull)
                        .flatMap(GoInlayTypeHintsCollector::toTypeTextStream)
                        .map(String::trim)
                        .toList();
                List<GoVarDefinition> varDefs = goVarSpec.getVarDefinitionList();

                if (typeNames.size() != varDefs.size()) {
                    GoInalyTypeHintsProvider.log.warn("Type names count {} does not match variable definitions count {} in GoVarSpec: {}",
                            typeNames.size(), varDefs.size(), goVarSpec.getText());
                    return; // Skip if counts do not match
                }

                Stream<Pair<GoVarDefinition, String>> zippedVarDefAndTypes = Streams.zip(varDefs.stream(), typeNames.stream(), Pair::of);

                zippedVarDefAndTypes.forEach(pair -> {
                    final GoVarDefinition varDef = pair.getLeft();
                    final String typeName = pair.getRight();
                    inlayTreeSink.addPresentation(new InlineInlayPosition(varDef.getTextRange().getEndOffset(), true, 0), null, null, HintFormat.Companion.getDefault(), ptb -> {
                        ptb.text(": ", null);
                        ptb.text(typeName, null);
                        return Unit.INSTANCE;
                    });
                });
            }
        }

        private static Stream<String> toTypeTextStream(GoType goType) {
            if (goType instanceof GoFunctionType goFunctionType) {
                // Handle function types
                return Stream.of(goFunctionType.getText());
            }
            return Arrays.stream(goType.getText().split(","));
        }
    }
}
