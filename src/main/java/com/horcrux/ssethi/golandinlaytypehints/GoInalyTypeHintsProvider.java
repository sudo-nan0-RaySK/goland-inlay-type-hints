package com.horcrux.ssethi.golandinlaytypehints;

import com.goide.GoLanguage;
import com.goide.psi.GoFunctionType;
import com.goide.psi.GoType;
import com.goide.psi.GoVarDefinition;
import com.goide.psi.GoVarSpec;
import com.goide.psi.impl.GoLightType;
import com.google.common.collect.Streams;
import com.intellij.codeInsight.hints.declarative.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import groovy.util.logging.Slf4j;
import java.util.Arrays;
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
  public static final String PROVIDER_ID = "go.type.hints.provider";
  private static final Logger log = LoggerFactory.getLogger(GoInalyTypeHintsProvider.class);

  @Override
  public @Nullable InlayHintsCollector createCollector(
      @NotNull PsiFile psiFile, @NotNull Editor editor) {
    return new GoInlayTypeHintsCollector();
  }

  private static class GoInlayTypeHintsCollector implements SharedBypassCollector {

    private static Stream<String> toTypeTextStream(GoType goType) {
      if (goType.getNavigationElement() instanceof Navigatable) {
        log.info(
            "GoInalyTypeHintsProvider goType {} is a navigatable", goType.getPresentationText());
      }
      if (goType instanceof GoFunctionType goFunctionType) {
        // Handle function types
        return Stream.of(goFunctionType.getText());
      }
      return Arrays.stream(goType.getText().split(","));
    }

    @Override
    public void collectFromElement(
        @NotNull PsiElement psiElement, @NotNull InlayTreeSink inlayTreeSink) {
      if (psiElement.getLanguage().is(GoLanguage.INSTANCE)
          && psiElement instanceof GoVarSpec goVarSpec) {

        List<GoType> resolvedGoTypes =
            goVarSpec.getExpressionList().stream()
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
        List<GoVarDefinition> varDefs = goVarSpec.getVarDefinitionList();

        if (resolvedGoTypes.size() != varDefs.size()) {
          GoInalyTypeHintsProvider.log.warn(
              "Type names count {} does not match variable definitions count {} in GoVarSpec: {}",
              resolvedGoTypes.size(),
              varDefs.size(),
              goVarSpec.getText());
          return; // Skip if counts do not match
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
                    InlayActionData actionData =
                        switch (goType) {
                          case Navigatable navigableElement ->
                              new InlayActionData(
                                  new PsiPointerInlayActionPayload(
                                      SmartPointerManager.getInstance(psiElement.getProject())
                                          .createSmartPsiElementPointer(
                                              goType
                                                  .getContextlessUnderlyingType()
                                                  .getNavigationElement())),
                                  PsiPointerInlayActionNavigationHandler.HANDLER_ID);
                          default -> null;
                        };
                    ptb.text(goType.getPresentationText(), actionData);
                    return Unit.INSTANCE;
                  });
            });
      }
    }
  }
}
