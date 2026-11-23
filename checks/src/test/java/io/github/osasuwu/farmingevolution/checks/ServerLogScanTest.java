package io.github.osasuwu.farmingevolution.checks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ServerLogScanTest {
    private static final String DONE =
            "[23Nov2026 12:00:05.120] [Server thread/INFO] [net.minecraft.server.dedicated.DedicatedServer/]: Done (4.211s)! For help, type \"help\"";
    private static final String INFO =
            "[23Nov2026 12:00:01.000] [main/INFO] [net.neoforged.fml.loading.FMLLoader/]: Loading 3 mods";

    @Test
    void cleanStartupHasNoProblems() {
        assertEquals(List.of(), ServerLogScan.problems(List.of(INFO, DONE)));
    }

    @Test
    void errorLineIsReportedWithItsText() {
        String error =
                "[23Nov2026 12:00:02.000] [main/ERROR] [net.neoforged.fml.ModLoader/]: Failed to load mod farmingevolution";
        assertEquals(List.of("error in log: " + error), ServerLogScan.problems(List.of(INFO, error, DONE)));
    }

    @Test
    void fatalLineIsReported() {
        String fatal =
                "[23Nov2026 12:00:02.000] [main/FATAL] [net.neoforged.fml.ModLoader/]: Mod loading has failed";
        assertEquals(List.of("error in log: " + fatal), ServerLogScan.problems(List.of(fatal, DONE)));
    }

    @Test
    void logWithoutCompletedStartupIsAProblem() {
        assertEquals(List.of("server never finished starting: no \"Done (...)!\" line"),
                ServerLogScan.problems(List.of(INFO)));
    }

    @Test
    void missingServerPropertiesOnAFirstBootIsNotAProblem() {
        // Vanilla logs this at ERROR in an empty game directory, then writes the file with defaults.
        String firstBoot =
                "[23Nov2026 12:00:00.500] [main/ERROR] [net.minecraft.server.dedicated.Settings/]: Failed to load properties from file: server.properties";
        String trace = "java.nio.file.NoSuchFileException: server.properties";
        assertEquals(List.of(), ServerLogScan.problems(List.of(firstBoot, trace, DONE)));
    }

    @Test
    void theWordErrorInsideAnInfoMessageIsNotAProblem() {
        String chatty =
                "[23Nov2026 12:00:03.000] [main/INFO] [net.minecraft.server.Main/]: ERROR reporting is enabled";
        assertTrue(ServerLogScan.problems(List.of(chatty, DONE)).isEmpty());
    }
}
