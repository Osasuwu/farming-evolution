package io.github.osasuwu.farmingevolution.checks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class TrackedFilesCheckTest {
    @Test
    void projectSourcesAreAllowed() {
        assertEquals(List.of(), TrackedFilesCheck.forbidden(List.of(
                "build.gradle",
                "checks/build.gradle",
                "breeding/build.gradle",
                "gradle/wrapper/gradle-wrapper.jar",
                "src/main/java/io/github/osasuwu/farmingevolution/FarmingEvolution.java",
                "src/main/java/io/github/osasuwu/farmingevolution/run/Runner.java",
                "docs/build/pipeline.md",
                "scripts/run/start.sh",
                "docs/research/minecraft-builds.md")));
    }

    @Test
    void decompiledMinecraftSourcesAreForbiddenWhereverTheyLie() {
        List<String> paths = List.of(
                "build/moddev/artifacts/neoforge-21.1.267-sources.jar",
                "checks/build/classes/java/main/A.class",
                "breeding/build/classes/java/main/B.class",
                "decompiled/net/minecraft/world/level/Level.java",
                "net/minecraft/server/MinecraftServer.java",
                "libs/com/mojang/brigadier/CommandDispatcher.java",
                "tools/minecraft-merged.jar");
        assertEquals(paths, TrackedFilesCheck.forbidden(paths));
    }

    @Test
    void gradleCacheAndRunDirectoriesAreForbidden() {
        List<String> paths = List.of(".gradle/8.0/fileHashes.bin", "run/smoke/world/level.dat");
        assertEquals(paths, TrackedFilesCheck.forbidden(paths));
    }

    @Test
    void windowsSeparatorsAreUnderstood() {
        assertEquals(List.of("decompiled\\net\\minecraft\\Foo.java"),
                TrackedFilesCheck.forbidden(List.of("decompiled\\net\\minecraft\\Foo.java")));
    }
}
