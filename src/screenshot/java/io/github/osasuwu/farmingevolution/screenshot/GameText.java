package io.github.osasuwu.farmingevolution.screenshot;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;

/**
 * What the player reads on screen: the chat and the line above the hotbar (action bar). Read
 * from the client's own chat list and overlay message, so every way a line gets there counts:
 * server messages, command feedback, {@code displayClientMessage} on either side, /title
 * actionbar. Polled on the game thread once a tick; lines that appeared since the last
 * {@link #drain} are handed out once.
 *
 * <p>The fields are private in Minecraft and read by reflection under their Mojang names, which
 * NeoForge 1.21.1 keeps at run time.
 */
final class GameText {
    private static final Field ALL_MESSAGES = field(ChatComponent.class, "allMessages");
    private static final Field OVERLAY_MESSAGE = field(Gui.class, "overlayMessageString");
    private static final Field OVERLAY_TIME = field(Gui.class, "overlayMessageTime");

    private GuiMessage newestSeen;
    private Component overlaySeen;
    private int overlayTimeSeen;
    private final List<String> chat = new ArrayList<>();
    private final List<String> actionBar = new ArrayList<>();

    /** New chat lines and action bar texts since the last drain, oldest first. */
    record Lines(List<String> chat, List<String> actionBar) {
    }

    void poll(Minecraft minecraft) {
        List<GuiMessage> all = messages(minecraft);
        // allMessages is newest first; the lines added since the last poll come before the one seen then.
        List<String> fresh = new ArrayList<>();
        for (GuiMessage message : all) {
            if (message == newestSeen) {
                break;
            }
            fresh.add(message.content().getString());
        }
        chat.addAll(fresh.reversed());
        newestSeen = all.isEmpty() ? null : all.getFirst();

        Component overlay = get(OVERLAY_MESSAGE, minecraft.gui, Component.class);
        int time = get(OVERLAY_TIME, minecraft.gui, Integer.class);
        // Every new overlay message restarts its timer; the timer only counts down otherwise.
        if (overlay != null && time > 0 && (overlay != overlaySeen || time > overlayTimeSeen)) {
            actionBar.add(overlay.getString());
        }
        overlaySeen = overlay;
        overlayTimeSeen = time;
    }

    Lines drain() {
        Lines lines = new Lines(List.copyOf(chat), List.copyOf(actionBar));
        chat.clear();
        actionBar.clear();
        return lines;
    }

    /** The last {@code count} chat lines, oldest first, whether handed out before or not. */
    List<String> lastChat(Minecraft minecraft, int count) {
        List<GuiMessage> all = messages(minecraft);
        List<String> lines = new ArrayList<>();
        for (int i = Math.min(count, all.size()) - 1; i >= 0; i--) {
            lines.add(all.get(i).content().getString());
        }
        return lines;
    }

    /** The action bar text on screen now, or null when none is shown. */
    String currentActionBar(Minecraft minecraft) {
        Component overlay = get(OVERLAY_MESSAGE, minecraft.gui, Component.class);
        int time = get(OVERLAY_TIME, minecraft.gui, Integer.class);
        return overlay != null && time > 0 ? overlay.getString() : null;
    }

    @SuppressWarnings("unchecked")
    private static List<GuiMessage> messages(Minecraft minecraft) {
        return get(ALL_MESSAGES, minecraft.gui.getChat(), List.class);
    }

    private static <T> T get(Field field, Object owner, Class<T> type) {
        try {
            return type.cast(field.get(owner));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    static Field field(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Client control: " + owner.getName() + "." + name + " not found", e);
        }
    }
}
