package io.github.osasuwu.farmingevolution.screenshot;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Client control for agents (issue #74): a running client that performs one action at a time
 * from a local connection ({@link ControlServer}) and answers with a picture taken after the
 * action, the new chat and action bar lines, and the player's state. Actions:
 * {@link ControlActions}. The client is {@code tools/ClientControl.java}; how to use it:
 * README.md, "Управление клиентом".
 *
 * <p>Dev-only like {@link ScreenshotRun}: the jar of this source set goes only into the mods
 * folders of the screenshot and control runs, and this mod is inert without the port property,
 * which only {@code gradlew runControl} sets.
 */
@Mod(value = ControlRun.MOD_ID, dist = Dist.CLIENT)
public final class ControlRun {
    public static final String MOD_ID = "farmingevolution_control";

    private static final String PORT_PROPERTY = "farmingevolution.control.port";
    private static final String PICTURES_PROPERTY = "farmingevolution.control.screenshots";
    private static final String IDLE_PROPERTY = "farmingevolution.control.idleMinutes";

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String WORLD_NAME = "control-world";

    /** Client ticks after joining before the first action: chunks and the hotbar settle. */
    private static final int JOIN_SETTLE_TICKS = 40;

    /** Ticks between an action and its picture: the server answers and the screen redraws. */
    private static final int SETTLE_TICKS = 10;

    /** A picture that waits longer than this for a rendered frame fails the action (a minimized window renders none). */
    private static final int FRAME_WAIT_TICKS = 200;

    /** The game closes if it cannot reach the world in this time. */
    private static final Duration JOIN_GIVE_UP_AFTER = Duration.ofMinutes(5);

    private enum Stage { WAITING_FOR_MENU, JOINING, SETTLING, READY, QUITTING }

    private enum Phase { RUNNING, SETTLING, PICTURE }

    private final int port;
    private final Path pictures;
    private final Duration idleLimit;
    private final GameText text = new GameText();
    private final long startNanos = System.nanoTime();
    private Stage stage = Stage.WAITING_FOR_MENU;
    private int stageTicks;
    private ControlServer server;
    private long lastActionNanos;
    private int pictureCount;

    private ControlServer.Request request;
    private ControlActions.Action action;
    private Phase phase;
    private int actionTicks;
    private int quitCountdown = -1;

    public ControlRun() {
        String port = System.getProperty(PORT_PROPERTY);
        if (port == null) {
            this.port = 0;
            this.pictures = null;
            this.idleLimit = null;
            return;
        }
        this.port = Integer.parseInt(port);
        this.pictures = Path.of(System.getProperty(PICTURES_PROPERTY, "screenshots")).toAbsolutePath();
        this.idleLimit = Duration.ofMinutes(Long.parseLong(System.getProperty(IDLE_PROPERTY, "30")));
        LOGGER.info("Client control: will listen on 127.0.0.1:{}, pictures go to {}", this.port, this.pictures);
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        stageTicks++;
        switch (stage) {
            case WAITING_FOR_MENU -> {
                if (minecraft.getOverlay() == null && minecraft.screen != null && minecraft.level == null) {
                    minecraft.options.pauseOnLostFocus = false;
                    minecraft.options.tutorialStep = TutorialSteps.NONE;
                    ScreenshotRun.createWorld(minecraft, WORLD_NAME);
                    next(Stage.JOINING);
                }
                giveUpIfLate(minecraft);
            }
            case JOINING -> {
                if (minecraft.player != null && minecraft.level != null && minecraft.screen == null) {
                    next(Stage.SETTLING);
                }
                giveUpIfLate(minecraft);
            }
            case SETTLING -> {
                if (stageTicks >= JOIN_SETTLE_TICKS && minecraft.levelRenderer.hasRenderedAllSections()) {
                    listen(minecraft);
                }
            }
            case READY -> serve(minecraft);
            case QUITTING -> {
                if (quitCountdown-- <= 0) {
                    quit(minecraft);
                }
            }
        }
    }

    @SubscribeEvent
    public void onFrameRendered(RenderFrameEvent.Post event) {
        if (stage != Stage.READY || action == null || phase != Phase.PICTURE) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        String name = String.format(Locale.ROOT, "%04d-%s.png", ++pictureCount, action.shotName());
        Path file = pictures.resolve(name);
        try (NativeImage image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            Files.createDirectories(pictures);
            image.writeToFile(file);
            reply(minecraft, "ok " + action.line(), file);
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Client control: could not take the picture {}", file, e);
            reply(minecraft, "error could not take the picture " + file + ": " + e, null);
        }
    }

    private void listen(Minecraft minecraft) {
        try {
            server = ControlServer.start(port);
        } catch (IOException e) {
            LOGGER.error("Client control: cannot listen on 127.0.0.1:{}; closing the game", port, e);
            quit(minecraft);
            return;
        }
        lastActionNanos = System.nanoTime();
        next(Stage.READY);
        LOGGER.info("Client control: ready on 127.0.0.1:{}", port);
    }

    private void serve(Minecraft minecraft) {
        text.poll(minecraft);
        if (action == null) {
            if (System.nanoTime() - lastActionNanos > idleLimit.toNanos()) {
                LOGGER.warn("Client control: no action for {}; closing the game", idleLimit);
                next(Stage.QUITTING);
                quitCountdown = 0;
                return;
            }
            ControlServer.Request next = server.poll();
            if (next == null) {
                return;
            }
            lastActionNanos = System.nanoTime();
            request = next;
            try {
                if (request.line().isEmpty()) {
                    throw new ControlActions.ActionException("empty line");
                }
                action = ControlActions.parse(request.line());
            } catch (ControlActions.ActionException e) {
                request.reply().complete("error " + e.getMessage() + "\n");
                request = null;
                return;
            } catch (RuntimeException e) {
                // A bad request must never crash the game tick.
                LOGGER.error("Client control: could not read action {}", request.line(), e);
                request.reply().complete("error " + e + "\n");
                request = null;
                return;
            }
            if (minecraft.player == null) {
                fail(minecraft, "the player is not in a world (died or left?)");
                return;
            }
            phase = Phase.RUNNING;
            actionTicks = 0;
        }
        lastActionNanos = System.nanoTime();
        switch (phase) {
            case RUNNING -> {
                boolean done;
                try {
                    done = action.step().tick(minecraft, actionTicks++);
                } catch (ControlActions.ActionException e) {
                    fail(minecraft, e.getMessage());
                    return;
                } catch (RuntimeException e) {
                    LOGGER.error("Client control: action {} failed", action.line(), e);
                    fail(minecraft, e.toString());
                    return;
                }
                if (done) {
                    phase = Phase.SETTLING;
                    actionTicks = 0;
                }
            }
            case SETTLING -> {
                if (++actionTicks >= SETTLE_TICKS) {
                    // Text arrived in the last tick is read before the reply.
                    text.poll(minecraft);
                    if (action.picture()) {
                        phase = Phase.PICTURE;
                        actionTicks = 0;
                    } else {
                        reply(minecraft, "ok " + action.line(), null);
                    }
                }
            }
            case PICTURE -> {
                if (++actionTicks > FRAME_WAIT_TICKS) {
                    fail(minecraft, "no frame was rendered for the picture; is the game window minimized?");
                }
            }
        }
    }

    private void fail(Minecraft minecraft, String message) {
        ControlActions.releaseKeys(minecraft);
        request.reply().complete("error " + message + "\n");
        request = null;
        action = null;
    }

    private void reply(Minecraft minecraft, String head, Path picture) {
        try {
            replyUnchecked(minecraft, head, picture);
        } catch (ControlActions.ActionException e) {
            fail(minecraft, e.getMessage());
        } catch (RuntimeException e) {
            // The reply must reach the agent even if reading the game state failed.
            LOGGER.error("Client control: reply to {} failed", action.line(), e);
            fail(minecraft, "the action ran, but its reply failed: " + e);
        }
    }

    private void replyUnchecked(Minecraft minecraft, String head, Path picture) {
        // Read before the new chat lines are handed out, so an error here loses none of them.
        StringBuilder jei = new StringBuilder();
        if (action.extra() == ControlActions.Extra.JEI) {
            if (!ModList.get().isLoaded("jei")) {
                throw new ControlActions.ActionException("JEI is not in this run");
            }
            ControlJei.describe(ControlActions.jeiCheck(action.line()), jei);
        }
        StringBuilder out = new StringBuilder(head).append('\n');
        if (picture != null) {
            out.append("screenshot ").append(picture).append('\n');
        }
        GameText.Lines lines = text.drain();
        lines.chat().forEach(line -> out.append("chat ").append(line).append('\n'));
        lines.actionBar().forEach(line -> out.append("actionbar ").append(line).append('\n'));
        if (action.extra() == ControlActions.Extra.READ) {
            text.lastChat(minecraft, ControlActions.readCount(action.line()))
                    .forEach(line -> out.append("history ").append(line).append('\n'));
            String bar = text.currentActionBar(minecraft);
            out.append("actionbar-now ").append(bar == null ? "(none)" : bar).append('\n');
        }
        if (action.extra() == ControlActions.Extra.SLOTS) {
            slots(minecraft, out);
        }
        if (action.extra() == ControlActions.Extra.DROPS) {
            drops(minecraft, ControlActions.dropsRadius(action.line()), out);
        }
        out.append(jei);
        if (minecraft.player != null) {
            out.append("state ").append(state(minecraft)).append('\n');
        }
        request.reply().complete(out.toString());
        if (action.quit()) {
            next(Stage.QUITTING);
            // A few ticks so the reply leaves before the game closes.
            quitCountdown = 10;
        }
        request = null;
        action = null;
    }

    private static String state(Minecraft mc) {
        var player = mc.player;
        StringBuilder out = new StringBuilder();
        out.append(String.format(Locale.ROOT, "pos=%.2f %.2f %.2f yaw=%.1f pitch=%.1f",
                player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
        out.append(" target=").append(target(mc));
        out.append(" screen=").append(mc.screen == null ? "none" : mc.screen.getClass().getSimpleName()
                + " \"" + mc.screen.getTitle().getString() + "\"");
        out.append(" hotbar=").append(player.getInventory().selected + 1);
        out.append(" main=").append(item(player.getItemInHand(InteractionHand.MAIN_HAND)));
        out.append(" off=").append(item(player.getItemInHand(InteractionHand.OFF_HAND)));
        out.append(" mode=").append(mc.gameMode.getPlayerMode().getName());
        out.append(" sneaking=").append(player.isShiftKeyDown());
        out.append(String.format(Locale.ROOT, " health=%.1f food=%d saturation=%.1f",
                player.getHealth(), player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel()));
        return out.toString();
    }

    private static String target(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
            var pos = block.getBlockPos();
            BlockState state = mc.level.getBlockState(pos);
            String properties = state.getValues().entrySet().stream()
                    .map(e -> e.getKey().getName() + "=" + e.getValue())
                    .collect(Collectors.joining(",", "[", "]"));
            return BuiltInRegistries.BLOCK.getKey(state.getBlock()) + (state.getValues().isEmpty() ? "" : properties)
                    + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + ":" + block.getDirection().getName();
        }
        if (hit instanceof EntityHitResult entity) {
            return "entity:" + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getEntity().getType());
        }
        return "none";
    }

    private static String item(ItemStack stack) {
        if (stack.isEmpty()) {
            return "empty";
        }
        return stack.getCount() + "x" + BuiltInRegistries.ITEM.getKey(stack.getItem())
                + "\"" + stack.getHoverName().getString() + "\"";
    }

    /** The non-empty slots of the open screen (or of the player's inventory) and the item on the pointer. */
    private static void slots(Minecraft mc, StringBuilder out) {
        var menu = mc.screen instanceof AbstractContainerScreen<?> screen ? screen.getMenu() : mc.player.inventoryMenu;
        out.append("menu ").append(mc.screen instanceof AbstractContainerScreen<?> ? mc.screen.getClass().getSimpleName() : "player inventory (closed)")
                .append(", slots 0-").append(menu.slots.size() - 1).append('\n');
        for (var slot : menu.slots) {
            if (slot.hasItem()) {
                out.append("slot ").append(slot.index).append(' ').append(item(slot.getItem()))
                        .append(components(mc, slot.getItem())).append('\n');
            }
        }
        out.append("carried ").append(item(menu.getCarried())).append(components(mc, menu.getCarried())).append('\n');
    }

    /**
     * The components a stack has beyond its item's defaults, as /give writes them (the genome
     * of a seed, a custom name, damage), or nothing when it has none.
     */
    private static String components(Minecraft mc, ItemStack stack) {
        DataComponentPatch patch = stack.getComponentsPatch();
        if (stack.isEmpty() || patch.isEmpty()) {
            return "";
        }
        return DataComponentPatch.CODEC.encodeStart(mc.level.registryAccess().createSerializationContext(NbtOps.INSTANCE), patch)
                .mapOrElse(tag -> " components=" + tag, error -> " components=(" + error.message() + ")");
    }

    /** The items lying on the ground within the radius of the player, nearest first, and the total of each item. */
    private static void drops(Minecraft mc, double radius, StringBuilder out) {
        var player = mc.player;
        List<ItemEntity> items = new ArrayList<>(mc.level.getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(radius), entity -> entity.distanceTo(player) <= radius));
        items.sort(Comparator.comparingDouble(entity -> entity.distanceTo(player)));
        Map<String, Integer> totals = new TreeMap<>();
        for (ItemEntity entity : items) {
            ItemStack stack = entity.getItem();
            out.append(String.format(Locale.ROOT, "drop %s at %.2f %.2f %.2f", item(stack), entity.getX(), entity.getY(), entity.getZ()))
                    .append('\n');
            totals.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), Integer::sum);
        }
        out.append(String.format(Locale.ROOT, "drops %d on the ground within %.1f blocks", items.size(), radius)).append('\n');
        totals.forEach((id, count) -> out.append("drops-total ").append(count).append('x').append(id).append('\n'));
    }

    private void giveUpIfLate(Minecraft minecraft) {
        if (System.nanoTime() - startNanos > JOIN_GIVE_UP_AFTER.toNanos()) {
            LOGGER.error("Client control: not in the world after {} (stage {}); closing the game", JOIN_GIVE_UP_AFTER, stage);
            quit(minecraft);
        }
    }

    private void next(Stage stage) {
        this.stage = stage;
        stageTicks = 0;
    }

    private void quit(Minecraft minecraft) {
        NeoForge.EVENT_BUS.unregister(this);
        if (server != null) {
            server.close();
        }
        minecraft.stop();
    }
}
