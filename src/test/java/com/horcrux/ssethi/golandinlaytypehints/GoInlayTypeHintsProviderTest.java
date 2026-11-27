package com.horcrux.ssethi.golandinlaytypehints;

import com.goide.project.GoModuleSettings;
import com.goide.sdk.GoSdk;
import com.goide.sdk.GoSdkService;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.testFramework.utils.inlays.declarative.DeclarativeInlayHintsProviderTestCase;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class GoInlayTypeHintsProviderTest extends DeclarativeInlayHintsProviderTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Process goEnv = new ProcessBuilder("go", "env", "GOROOT").redirectErrorStream(true).start();
    String goRoot = new String(goEnv.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
    assertEquals("go env GOROOT failed: " + goRoot, 0, goEnv.waitFor());
    assertFalse("go env GOROOT returned an empty path", goRoot.isEmpty());
    VfsRootAccess.allowRootAccess(getTestRootDisposable(), goRoot);
    GoModuleSettings.getInstance(getModule()).setGoSupportEnabled(true);
    GoSdkService.getInstance(getProject()).setSdk(GoSdk.fromHomePath(goRoot));
  }

  public void testShouldShowIntWhenIntegerLiteralInitializesInferredVariable() {
    assertHints(
        """
        package main

        func main() {
          count/*<# : int #>*/ := 0
          _ = count
        }
        """);
  }

  public void testShouldShowDeclaredTypeWhenIntegerLiteralInitializesExplicitlyTypedVariable() {
    assertHints(
        """
        package main

        func main() {
          var count/*<# : int64 #>*/ int64 = 0
          _ = count
        }
        """);
  }

  private void assertHints(String expected) {
    doTestProvider(
        "main.go",
        expected,
        new GoInalyTypeHintsProvider(),
        Map.of(),
        null,
        true,
        ProviderTestMode.SIMPLE);
  }
}
