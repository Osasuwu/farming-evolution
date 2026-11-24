package io.github.osasuwu.farmingevolution.screenshot;

import com.mojang.blaze3d.platform.InputConstants;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.lwjgl.glfw.GLFW;

/**
 * The actions of the client control: each line a client sends is parsed into an {@link Action}
 * whose step runs on the game thread once a tick until it is done.
 *
 * <p>Input goes the way a player's does: key mappings are clicked or held and Minecraft's own
 * keybind handling turns them into {@code startUseItem} / {@code useItemOn}, {@code startAttack},
 * hotbar selection and so on; screens get the mouse press the window callback would deliver;
 * commands are sent as the chat screen sends them. Only Shift + click in a screen calls
 * {@code handleInventoryMouseClick} with {@code QUICK_MOVE} directly, because screens read Shift
 * from the real keyboard.
 */
final class ControlActions {
    /** The longest wait or hold an action may ask for: 5 minutes. */
    static final int MAX_TICKS = 6000;

    /** Ticks Shift is held before and after the click, so the server sees the player sneaking. */
    private static final int SHIFT_LEAD_TICKS = 3;

    private static final Field MOUSE_X = GameText.field(MouseHandler.class, "xpos");
    private static final Field MOUSE_Y = GameText.field(MouseHandler.class, "ypos");
    private static final Field MOUSE_GRABBED = GameText.field(MouseHandler.class, "mouseGrabbed");
    private static final Method MOUSE_PRESS = method(MouseHandler.class, "onPress", long.class, int.class, int.class, int.class);

    private ControlActions() {
    }

    /** An action cannot run as asked; the reply says why. */
    static final class ActionException extends RuntimeException {
        ActionException(String message) {
            super(message);
        }
    }

    /** One tick of an action. Returns true when the action is done. */
    interface Step {
        boolean tick(Minecraft minecraft, int tick);
    }

    /**
     * A parsed line. {@code shotName} names the picture taken after it; {@code extra} adds lines
     * to the reply (chat history, slot list); {@code quit} closes the game after the reply.
     */
    record Action(String line, Step step, String shotName, Extra extra, boolean quit, boolean picture) {
    }

    enum Extra { NONE, READ, SLOTS }

    /**
     * What lookat aims at: a block (whole-number coordinates) or a point. A block is aimed at
     * where it can be seen from the eyes: the centre of its outline, or just inside the top or
     * bottom face when the eyes are above or below it, so a ray from above a field hits the
     * farmland or the low crop it names, not the block in front of it.
     */
    private record Target(Vec3 point, BlockPos block) {
        Vec3 aimFrom(Minecraft mc) {
            if (block == null) {
                return point;
            }
            VoxelShape shape = mc.level.getBlockState(block).getShape(mc.level, block);
            if (shape.isEmpty()) {
                return Vec3.atCenterOf(block);
            }
            AABB box = shape.bounds().move(block);
            double eyes = mc.player.getEyeY();
            double y = eyes > box.maxY ? box.maxY - 0.01 : eyes < box.minY ? box.minY + 0.01 : box.getCenter().y;
            return new Vec3(box.getCenter().x, y, box.getCenter().z);
        }
    }

    /** The last lookat, so a Shift action can aim again from the lower eyes of a sneaking player. */
    private static Target aimTarget;

    static Action parse(String line) {
        String[] words = line.strip().split("\\s+");
        String verb = words[0].toLowerCase(Locale.ROOT);
        String[] args = Arrays.copyOfRange(words, 1, words.length);
        String rest = line.strip().substring(words[0].length()).strip();
        String shot = verb;
        return switch (verb) {
            case "look" -> {
                float yaw = number(args, 0, "yaw");
                float pitch = number(args, 1, "pitch");
                count(args, 2);
                yield new Action(line, once(mc -> {
                    aimTarget = null;
                    turn(mc, yaw, pitch);
                }), shot, Extra.NONE, false, true);
            }
            case "turn" -> {
                float yaw = number(args, 0, "yaw change");
                float pitch = number(args, 1, "pitch change");
                count(args, 2);
                yield new Action(line, once(mc -> {
                    aimTarget = null;
                    turn(mc, mc.player.getYRot() + yaw, mc.player.getXRot() + pitch);
                }), shot, Extra.NONE, false, true);
            }
            case "lookat" -> {
                Target target = target(args);
                yield new Action(line, once(mc -> {
                    aimTarget = target;
                    aim(mc, target.aimFrom(mc));
                }), shot, Extra.NONE, false, true);
            }
            case "tp" -> {
                if (args.length != 3 && args.length != 5) {
                    throw new ActionException("tp takes x y z [yaw pitch]");
                }
                for (int i = 0; i < args.length; i++) {
                    number(args, i, "coordinate");
                }
                String command = "tp @s " + String.join(" ", args);
                yield new Action(line, once(mc -> {
                    aimTarget = null;
                    mc.player.connection.sendCommand(command);
                }), shot, Extra.NONE, false, true);
            }
            case "move" -> {
                if (args.length != 2) {
                    throw new ActionException("move takes forward|back|left|right|jump|sneak and a number of ticks");
                }
                int ticks = ticks(args, 1);
                yield new Action(line, hold(movementKey(args[0]), ticks), shot, Extra.NONE, false, true);
            }
            case "use", "attack" -> {
                boolean shift = flag(args, "shift");
                yield new Action(line, click(verb.equals("use"), shift), shot, Extra.NONE, false, true);
            }
            case "mine" -> {
                count(args, 1);
                yield new Action(line, mine(ticks(args, 0)), shot, Extra.NONE, false, true);
            }
            case "slot" -> {
                count(args, 1);
                int slot = (int) number(args, 0, "hotbar slot");
                if (slot < 1 || slot > 9) {
                    throw new ActionException("hotbar slots are 1 to 9");
                }
                yield new Action(line, inWorld(mc -> KeyMapping.click(mc.options.keyHotbarSlots[slot - 1].getKey())),
                        shot, Extra.NONE, false, true);
            }
            case "swap" -> {
                count(args, 0);
                yield new Action(line, inWorld(mc -> KeyMapping.click(mc.options.keySwapOffhand.getKey())), shot, Extra.NONE, false, true);
            }
            case "inventory" -> {
                count(args, 0);
                yield new Action(line, inWorld(mc -> KeyMapping.click(mc.options.keyInventory.getKey())), shot, Extra.NONE, false, true);
            }
            case "close" -> {
                count(args, 0);
                yield new Action(line, once(mc -> {
                    if (mc.screen == null) {
                        throw new ActionException("no screen is open");
                    }
                    press(mc, GLFW.GLFW_KEY_ESCAPE);
                }), shot, Extra.NONE, false, true);
            }
            case "key" -> {
                count(args, 1);
                InputConstants.Key key;
                try {
                    // An unknown name throws here (NumberFormatException or IllegalArgumentException).
                    key = InputConstants.getKey("key.keyboard." + args[0].toLowerCase(Locale.ROOT));
                } catch (RuntimeException e) {
                    key = InputConstants.UNKNOWN;
                }
                if (key == InputConstants.UNKNOWN || key.getValue() < 0) {
                    throw new ActionException("unknown key " + args[0] + "; use GLFW names: e, r, escape, enter, tab, f3, left.shift, ...");
                }
                InputConstants.Key pressed = key;
                if (key.getValue() == GLFW.GLFW_KEY_F11) {
                    throw new ActionException("f11 would make the game full screen");
                }
                yield new Action(line, once(mc -> press(mc, pressed.getValue())), shot, Extra.NONE, false, true);
            }
            case "click" -> {
                if (args.length < 1 || args.length > 2) {
                    throw new ActionException("click takes a slot number and optionally right or shift");
                }
                int slot = (int) number(args, 0, "slot");
                String how = args.length == 2 ? args[1].toLowerCase(Locale.ROOT) : "left";
                if (!how.equals("left") && !how.equals("right") && !how.equals("shift")) {
                    throw new ActionException("click takes left, right or shift, not " + how);
                }
                yield new Action(line, once(mc -> clickSlot(mc, slot, how)), shot, Extra.NONE, false, true);
            }
            case "hover" -> {
                count(args, 1);
                int slot = (int) number(args, 0, "slot");
                yield new Action(line, once(mc -> {
                    Slot target = slot(mc, slot);
                    var screen = (AbstractContainerScreen<?>) mc.screen;
                    pointAt(mc, screen.getGuiLeft() + target.x + 8, screen.getGuiTop() + target.y + 8);
                }), shot, Extra.NONE, false, true);
            }
            case "clickat", "hoverat" -> {
                if (args.length < 2 || args.length > 3 || verb.equals("hoverat") && args.length != 2) {
                    throw new ActionException(verb + " takes x y in pixels of the picture" + (verb.equals("clickat") ? " and optionally right" : ""));
                }
                double x = number(args, 0, "x");
                double y = number(args, 1, "y");
                boolean right = args.length == 3 && args[2].equalsIgnoreCase("right");
                if (args.length == 3 && !right) {
                    throw new ActionException("clickat takes right as its third word, not " + args[2]);
                }
                yield new Action(line, once(mc -> {
                    if (mc.screen == null) {
                        throw new ActionException("no screen is open");
                    }
                    pointAtPixel(mc, x, y);
                    if (verb.equals("clickat")) {
                        mouseClick(mc, right ? GLFW.GLFW_MOUSE_BUTTON_RIGHT : GLFW.GLFW_MOUSE_BUTTON_LEFT);
                    }
                }), shot, Extra.NONE, false, true);
            }
            case "command" -> {
                if (rest.isEmpty()) {
                    throw new ActionException("command takes the command text, e.g. command /time set day");
                }
                String command = rest.startsWith("/") ? rest.substring(1) : rest;
                yield new Action(line, once(mc -> mc.player.connection.sendCommand(command)), shot, Extra.NONE, false, true);
            }
            case "say" -> {
                if (rest.isEmpty()) {
                    throw new ActionException("say takes the chat message");
                }
                yield new Action(line, once(mc -> mc.player.connection.sendChat(rest)), shot, Extra.NONE, false, true);
            }
            case "wait" -> {
                count(args, 1);
                int ticks = ticks(args, 0);
                yield new Action(line, (mc, tick) -> tick >= ticks - 1, shot, Extra.NONE, false, true);
            }
            case "shot" -> {
                if (args.length > 1 || args.length == 1 && !args[0].matches("[A-Za-z0-9_.-]+")) {
                    throw new ActionException("shot takes an optional file name of letters, digits, - _ .");
                }
                yield new Action(line, once(mc -> { }), args.length == 1 ? args[0] : shot, Extra.NONE, false, true);
            }
            case "read" -> {
                if (args.length > 1) {
                    throw new ActionException("read takes an optional number of lines");
                }
                readCount(line); // a bad number fails here, not after the action ran

                yield new Action(line, once(mc -> { }), shot, Extra.READ, false, false);
            }
            case "slots" -> {
                count(args, 0);
                yield new Action(line, once(mc -> { }), shot, Extra.SLOTS, false, false);
            }
            case "state" -> {
                count(args, 0);
                yield new Action(line, once(mc -> { }), shot, Extra.NONE, false, false);
            }
            case "quit" -> {
                count(args, 0);
                yield new Action(line, once(mc -> { }), shot, Extra.NONE, true, false);
            }
            default -> throw new ActionException("unknown action " + words[0] + "; see README.md, \"Управление клиентом\"");
        };
    }

    /** The number of chat lines {@code read} asks for, 10 by default. */
    static int readCount(String line) {
        String[] words = line.strip().split("\\s+");
        return words.length > 1 ? Math.max(1, Math.min(100, (int) number(words, 1, "line count"))) : 10;
    }

    // ---------------------------------------------------------------- steps

    private static Step once(java.util.function.Consumer<Minecraft> body) {
        return (mc, tick) -> {
            body.accept(mc);
            return true;
        };
    }

    /** A key mapping click handled by Minecraft's keybinds, which only run with no screen open. */
    private static Step inWorld(java.util.function.Consumer<Minecraft> body) {
        return once(mc -> {
            requireNoScreen(mc);
            body.accept(mc);
        });
    }

    /** Holds a key mapping down for the given number of ticks, as a held key does. */
    private static Step hold(java.util.function.Function<Minecraft, KeyMapping> key, int ticks) {
        return (mc, tick) -> {
            if (tick == 0) {
                requireNoScreen(mc);
            }
            KeyMapping mapping = key.apply(mc);
            boolean done = tick >= ticks;
            mapping.setDown(!done);
            return done;
        };
    }

    /**
     * Right (use) or left (attack) click at the crosshair: a block, an entity or the air. With
     * Shift the player sneaks first, so the server sees {@code isShiftKeyDown}; a lookat target
     * is aimed at again from the lower eyes.
     */
    private static Step click(boolean use, boolean shift) {
        int clickTick = shift ? SHIFT_LEAD_TICKS : 0;
        int lastTick = shift ? 2 * SHIFT_LEAD_TICKS : 0;
        return (mc, tick) -> {
            if (tick == 0) {
                requireNoScreen(mc);
            }
            if (shift) {
                mc.options.keyShift.setDown(tick < lastTick);
            }
            if (tick == clickTick) {
                if (shift && aimTarget != null) {
                    aim(mc, aimTarget.aimFrom(mc));
                }
                mc.gameRenderer.pick(1.0F);
                KeyMapping.click((use ? mc.options.keyUse : mc.options.keyAttack).getKey());
            }
            return tick >= lastTick;
        };
    }

    /** Holds the attack key for the given ticks, as holding the left button breaks a block. */
    private static Step mine(int ticks) {
        boolean[] grabbedBefore = new boolean[1];
        return (mc, tick) -> {
            if (tick == 0) {
                requireNoScreen(mc);
                grabbedBefore[0] = mc.mouseHandler.isMouseGrabbed();
                mc.gameRenderer.pick(1.0F);
                KeyMapping.click(mc.options.keyAttack.getKey());
            }
            boolean done = tick >= ticks;
            // Minecraft keeps breaking only while it owns the mouse. The flag is set without
            // grabbing the desktop cursor, and put back as it was.
            set(MOUSE_GRABBED, mc.mouseHandler, done ? grabbedBefore[0] : true);
            mc.options.keyAttack.setDown(!done);
            return done;
        };
    }

    private static java.util.function.Function<Minecraft, KeyMapping> movementKey(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "forward" -> mc -> mc.options.keyUp;
            case "back" -> mc -> mc.options.keyDown;
            case "left" -> mc -> mc.options.keyLeft;
            case "right" -> mc -> mc.options.keyRight;
            case "jump" -> mc -> mc.options.keyJump;
            case "sneak" -> mc -> mc.options.keyShift;
            default -> throw new ActionException("move takes forward, back, left, right, jump or sneak, not " + name);
        };
    }

    /** Lets go of every key an action may hold, after an action failed half way. */
    static void releaseKeys(Minecraft mc) {
        for (KeyMapping key : new KeyMapping[] {mc.options.keyShift, mc.options.keyAttack, mc.options.keyUse,
                mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight, mc.options.keyJump}) {
            key.setDown(false);
        }
    }

    // ---------------------------------------------------------------- input

    private static void turn(Minecraft mc, float yaw, float pitch) {
        var player = mc.player;
        float clamped = Mth.clamp(pitch, -90.0F, 90.0F);
        player.setYRot(yaw);
        player.setXRot(clamped);
        player.yRotO = yaw;
        player.xRotO = clamped;
        player.setYHeadRot(yaw);
        player.yHeadRotO = yaw;
    }

    private static void aim(Minecraft mc, Vec3 point) {
        Vec3 eyes = mc.player.getEyePosition();
        double dx = point.x - eyes.x;
        double dy = point.y - eyes.y;
        double dz = point.z - eyes.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        turn(mc, yaw, pitch);
    }

    /** A key press and release through the window's keyboard callback. */
    private static void press(Minecraft mc, int key) {
        long window = mc.getWindow().getWindow();
        int scancode = GLFW.glfwGetKeyScancode(key);
        mc.keyboardHandler.keyPress(window, key, scancode, GLFW.GLFW_PRESS, 0);
        mc.keyboardHandler.keyPress(window, key, scancode, GLFW.GLFW_RELEASE, 0);
    }

    /** Moves Minecraft's idea of the pointer to screen (GUI-scaled) coordinates; the desktop cursor stays. */
    private static void pointAt(Minecraft mc, double x, double y) {
        var window = mc.getWindow();
        set(MOUSE_X, mc.mouseHandler, x * window.getScreenWidth() / window.getGuiScaledWidth());
        set(MOUSE_Y, mc.mouseHandler, y * window.getScreenHeight() / window.getGuiScaledHeight());
    }

    /** Moves Minecraft's idea of the pointer to a pixel of the picture (the framebuffer). */
    private static void pointAtPixel(Minecraft mc, double x, double y) {
        var window = mc.getWindow();
        if (x < 0 || y < 0 || x >= window.getWidth() || y >= window.getHeight()) {
            throw new ActionException("the picture is " + window.getWidth() + "x" + window.getHeight() + " pixels; " + x + " " + y + " is outside");
        }
        set(MOUSE_X, mc.mouseHandler, x * window.getScreenWidth() / window.getWidth());
        set(MOUSE_Y, mc.mouseHandler, y * window.getScreenHeight() / window.getHeight());
    }

    /** A press and release at the pointer through the window's mouse button callback. */
    private static void mouseClick(Minecraft mc, int button) {
        long window = mc.getWindow().getWindow();
        try {
            MOUSE_PRESS.invoke(mc.mouseHandler, window, button, GLFW.GLFW_PRESS, 0);
            MOUSE_PRESS.invoke(mc.mouseHandler, window, button, GLFW.GLFW_RELEASE, 0);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    private static void clickSlot(Minecraft mc, int index, String how) {
        Slot target = slot(mc, index);
        var screen = (AbstractContainerScreen<?>) mc.screen;
        if (how.equals("shift")) {
            mc.gameMode.handleInventoryMouseClick(screen.getMenu().containerId, target.index, 0, ClickType.QUICK_MOVE, mc.player);
            return;
        }
        pointAt(mc, screen.getGuiLeft() + target.x + 8, screen.getGuiTop() + target.y + 8);
        mouseClick(mc, how.equals("right") ? GLFW.GLFW_MOUSE_BUTTON_RIGHT : GLFW.GLFW_MOUSE_BUTTON_LEFT);
    }

    private static Slot slot(Minecraft mc, int index) {
        if (!(mc.screen instanceof AbstractContainerScreen<?> screen)) {
            throw new ActionException("no inventory or container screen is open");
        }
        var slots = screen.getMenu().slots;
        if (index < 0 || index >= slots.size()) {
            throw new ActionException("this screen has slots 0 to " + (slots.size() - 1));
        }
        return slots.get(index);
    }

    private static void requireNoScreen(Minecraft mc) {
        if (mc.screen != null) {
            throw new ActionException("a screen is open (" + mc.screen.getClass().getSimpleName() + "); close it first");
        }
    }

    // ---------------------------------------------------------------- parsing

    /** Three whole numbers name a block; any decimal makes the three a point. */
    private static Target target(String[] args) {
        count(args, 3);
        double[] xyz = new double[3];
        boolean block = true;
        for (int i = 0; i < 3; i++) {
            xyz[i] = number(args, i, "coordinate");
            block &= args[i].matches("-?\\d+");
        }
        return block
                ? new Target(null, new BlockPos((int) xyz[0], (int) xyz[1], (int) xyz[2]))
                : new Target(new Vec3(xyz[0], xyz[1], xyz[2]), null);
    }

    private static int ticks(String[] args, int index) {
        int ticks = (int) number(args, index, "ticks");
        if (ticks < 1 || ticks > MAX_TICKS) {
            throw new ActionException("ticks must be 1 to " + MAX_TICKS);
        }
        return ticks;
    }

    private static float number(String[] args, int index, String what) {
        if (index >= args.length) {
            throw new ActionException("missing " + what);
        }
        try {
            return Float.parseFloat(args[index]);
        } catch (NumberFormatException e) {
            throw new ActionException(what + " is not a number: " + args[index]);
        }
    }

    private static void count(String[] args, int expected) {
        if (args.length != expected) {
            throw new ActionException("expected " + expected + " argument(s), got " + args.length);
        }
    }

    private static boolean flag(String[] args, String name) {
        if (args.length == 0) {
            return false;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase(name)) {
            return true;
        }
        throw new ActionException("the only option here is " + name);
    }

    private static void set(Field field, Object owner, Object value) {
        try {
            field.set(owner, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Method method(Class<?> owner, String name, Class<?>... parameters) {
        try {
            Method method = owner.getDeclaredMethod(name, parameters);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Client control: " + owner.getName() + "." + name + " not found", e);
        }
    }
}
