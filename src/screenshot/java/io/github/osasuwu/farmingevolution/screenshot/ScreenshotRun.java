package io.github.osasuwu.farmingevolution.screenshot;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * The client screenshot run of the check pipeline (issue #16). Not part of the mod a player
 * installs: this source set is a mod of its own, which only {@code gradlew runScreenshot}
 * puts into the mods folder of its game directory (build.gradle). There it creates a flat
 * world, builds the slice in it ({@link SliceScene}), takes the same picture F2 takes, writes
 * it to the file the system property names, and closes the game.
 *
 * <p>Inert without the system property, so the jar does nothing in any other run.
 */
@Mod(value = ScreenshotRun.MOD_ID, dist = Dist.CLIENT)
public final class ScreenshotRun {
    public static final String MOD_ID = "farmingevolution_screenshot";

    /** The PNG file to write. Set by the run in build.gradle. */
    private static final String OUTPUT_PROPERTY = "farmingevolution.screenshot";

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String WORLD_NAME = "screenshot-slice";

    /** Client ticks between the scene being built and the picture: lets chunks and the hotbar settle. */
    private static final int SETTLE_TICKS = 60;

    /** The run gives up after this long, so a stuck screen never leaves the game open. */
    private static final Duration GIVE_UP_AFTER = Duration.ofMinutes(5);

    private enum Stage { WAITING_FOR_MENU, JOINING, BUILDING, SETTLING, SHOOTING, QUITTING }

    private final Path output;
    private final long startNanos = System.nanoTime();
    private final AtomicBoolean sceneBuilt = new AtomicBoolean();
    private Stage stage = Stage.WAITING_FOR_MENU;
    private int settledTicks;

    public ScreenshotRun() {
        String output = System.getProperty(OUTPUT_PROPERTY);
        if (output == null) {
            this.output = null;
            return;
        }
        this.output = Path.of(output);
        LOGGER.info("Screenshot run: the picture goes to {}", this.output);
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (stage != Stage.QUITTING && System.nanoTime() - startNanos > GIVE_UP_AFTER.toNanos()) {
            LOGGER.error("Screenshot run: no picture after {} (stage {}); closing the game without one", GIVE_UP_AFTER, stage);
            quit(minecraft);
            return;
        }
        switch (stage) {
            case WAITING_FOR_MENU -> {
                // The loading overlay is gone and some first screen (title, onboarding) is open.
                if (minecraft.getOverlay() == null && minecraft.screen != null && minecraft.level == null) {
                    // A background window must not pause the integrated server or cover the picture.
                    minecraft.options.pauseOnLostFocus = false;
                    createWorld(minecraft);
                    stage = Stage.JOINING;
                }
            }
            case JOINING -> {
                var server = minecraft.getSingleplayerServer();
                if (minecraft.player != null && minecraft.level != null && server != null) {
                    var uuid = minecraft.player.getUUID();
                    server.execute(() -> {
                        SliceScene.build(server, server.getPlayerList().getPlayer(uuid));
                        sceneBuilt.set(true);
                    });
                    stage = Stage.BUILDING;
                }
            }
            case BUILDING -> {
                if (sceneBuilt.get()) {
                    stage = Stage.SETTLING;
                }
            }
            case SETTLING -> {
                if (minecraft.screen == null && ++settledTicks >= SETTLE_TICKS
                        && minecraft.levelRenderer.hasRenderedAllSections()) {
                    stage = Stage.SHOOTING;
                }
            }
            case SHOOTING -> {
                // The picture is taken after the next frame is rendered: onFrameRendered.
            }
            case QUITTING -> quit(minecraft);
        }
    }

    /** Takes the picture of a whole rendered frame, the hotbar included, as F2 does. */
    @SubscribeEvent
    public void onFrameRendered(RenderFrameEvent.Post event) {
        if (stage != Stage.SHOOTING) {
            return;
        }
        stage = Stage.QUITTING;
        Minecraft minecraft = Minecraft.getInstance();
        try (NativeImage image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            Files.createDirectories(output.toAbsolutePath().getParent());
            image.writeToFile(output);
            LOGGER.info("Screenshot run: wrote {} ({}x{})", output, image.getWidth(), image.getHeight());
        } catch (IOException e) {
            LOGGER.error("Screenshot run: could not write {}", output, e);
        }
    }

    /** A flat, peaceful survival world: the slice is built on flat grass and nothing walks into the picture. */
    private static void createWorld(Minecraft minecraft) {
        LOGGER.info("Screenshot run: creating world {}", WORLD_NAME);
        LevelSettings settings = new LevelSettings(WORLD_NAME, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                new GameRules(), WorldDataConfiguration.DEFAULT);
        minecraft.createWorldOpenFlows().createFreshLevel(WORLD_NAME, settings, new WorldOptions(0L, false, false),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                        .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                minecraft.screen);
    }

    /** Same as the title screen's Quit: the game saves the world, leaves it and exits. */
    private void quit(Minecraft minecraft) {
        NeoForge.EVENT_BUS.unregister(this);
        minecraft.stop();
    }
}
