package com.horcrux.ssethi.golandinlaytypehints;

import com.goide.GoLanguage;
import com.goide.psi.*;
import com.goide.psi.impl.GoLightType;
import com.google.common.collect.Streams;
import com.intellij.codeInsight.hints.declarative.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import groovy.util.logging.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import kotlin.Unit;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class GoInalyTypeHintsProvider implements InlayHintsProvider {
  @SuppressWarnings("unused")
  public static final String PROVIDER_ID = "go.type.hints.provider";

  private static final Logger log = LoggerFactory.getLogger(GoInalyTypeHintsProvider.class);

  @Override
  public @Nullable InlayHintsCollector createCollector(
      @NotNull PsiFile psiFile, @NotNull Editor editor) {
    return new GoInlayTypeHintsCollector();
  }

  private static class GoInlayTypeHintsCollector implements SharedBypassCollector {

    private static @NotNull GoType getRefinedUnderlyingIndirectType(GoType goType) {
      return getGoTypeRecursive(goType);
    }

    private static @NotNull GoType getGoTypeRecursive(@NotNull GoType goType) {
      return switch (goType) {
        case GoArrayOrSliceType arrayType -> getGoTypeRecursive(arrayType.getType());
        case GoPointerType pointerType ->
            Objects.nonNull(pointerType.getType())
                ? getGoTypeRecursive(pointerType.getType())
                : goType;
        default -> goType;
      };
    }

    private static @NotNull List<GoType> resolveGoTypesFromGoTypeOwnerStream(
        Stream<? extends GoTypeOwner> goTypeInfoStream) {
      return goTypeInfoStream
          .map(exper -> exper.getGoType(null))
          .filter(Objects::nonNull)
          .flatMap(
              goType -> {
                if (goType
                    instanceof
                    GoLightType.LightTypeList goLightTypeList) { // Handle light type lists
                  return goLightTypeList.getTypeList().stream();
                } // Handle other types
                return Stream.of(goType);
              })
          .toList();
    }

    @Override
    public void collectFromElement(
        @NotNull final PsiElement psiElement, @NotNull InlayTreeSink inlayTreeSink) {
      if (!psiElement.getLanguage().is(GoLanguage.INSTANCE)) {
        return; // Only process Go language elements
      }
      switch (psiElement) {
        case GoRangeClause rangeClause -> {
          final List<GoType> resolvedGoTypes =
              resolveGoTypesFromGoTypeOwnerStream(rangeClause.getVarDefinitionList().stream());
          final List<GoVarDefinition> varDefs = rangeClause.getVarDefinitionList();
          deduceInlayTypeHintsRenderInfo(
              psiElement, inlayTreeSink, resolvedGoTypes, varDefs, psiElement.getText());
        }
        case GoVarSpec goVarSpec -> {
          final List<GoType> resolvedGoTypes =
              resolveGoTypesFromGoTypeOwnerStream(goVarSpec.getExpressionList().stream());
          final List<GoVarDefinition> varDefs = goVarSpec.getVarDefinitionList();
          deduceInlayTypeHintsRenderInfo(
              psiElement, inlayTreeSink, resolvedGoTypes, varDefs, goVarSpec.getText());
        }
        default -> {}
      }
    }

    private void deduceInlayTypeHintsRenderInfo(
        @NotNull PsiElement psiElement,
        @NotNull InlayTreeSink inlayTreeSink,
        final List<GoType> resolvedGoTypes,
        final List<GoVarDefinition> varDefs,
        final String psiElementText) {

      if (resolvedGoTypes.size() != varDefs.size()) {
        GoInalyTypeHintsProvider.log.warn(
            "Type names count {} does not match variable definitions count {} in GoVarSpec: {}",
            resolvedGoTypes.size(),
            varDefs.size(),
            psiElementText);
        return;
      }

      Stream<Pair<GoVarDefinition, GoType>> zippedVarDefAndTypes =
          Streams.zip(varDefs.stream(), resolvedGoTypes.stream(), Pair::of);

      zippedVarDefAndTypes.forEach(
          pair -> {
            final GoVarDefinition varDef = pair.getLeft();
            final GoType goType = pair.getRight();
            inlayTreeSink.addPresentation(
                new InlineInlayPosition(varDef.getTextRange().getEndOffset(), true, 0),
                null,
                goType.getText(),
                HintFormat.Companion.getDefault(),
                ptb -> {
                  ptb.text(": ", null);
                  if (goType instanceof GoMapType goMapType) {
                    ptb.text("map[", null);
                    embedClickableTypeHintString(psiElement, ptb, goMapType.getKeyType());
                    ptb.text("]", null);
                    embedClickableTypeHintString(psiElement, ptb, goMapType.getValueType());
                  } else {
                    embedClickableTypeHintString(psiElement, ptb, goType);
                  }
                  return Unit.INSTANCE;
                });
          });
    }

    private void embedClickableTypeHintString(
        @NotNull final PsiElement psiElement, PresentationTreeBuilder ptb, GoType goType) {
      GoType refinedUnderlyingIndirectType = getRefinedUnderlyingIndirectType(goType);
      InlayActionData actionData =
          new InlayActionData(
              new PsiPointerInlayActionPayload(
                  SmartPointerManager.getInstance(psiElement.getProject())
                      .createSmartPsiElementPointer(
                          refinedUnderlyingIndirectType
                              .getContextlessUnderlyingType()
                              .getNavigationElement())),
              PsiPointerInlayActionNavigationHandler.HANDLER_ID);
      ptb.text(goType.getPresentationText(), actionData);
    }
  }
}
