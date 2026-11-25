package io.github.osasuwu.farmingevolution.client;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.plant.BreederGogglesItem;
import io.github.osasuwu.farmingevolution.plant.PlantBlockEntity;
import io.github.osasuwu.farmingevolution.plant.Readout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The HUD of the breeder's goggles (issue #90). While the player wears them it shows two
 * panels in the top left corner: the climate of the player's place and, under it, the plant of
 * the mod the player looks at. The lines are {@link Readout}'s; this class only places them.
 * Client only: the dedicated server never loads it.
 *
 * <p>Both panels together are at most 9 lines, about 90 pixels of the GUI. The GUI is at least
 * 240 pixels high at every scale, and the chat, closed, reaches up at most 130 pixels from the
 * bottom (40 for the hotbar, 90 for ten lines), so the panels touch neither the chat nor the
 * hotbar. They hide with the debug screen, with F1 and under any open screen.
 */
@EventBusSubscriber(modid = FarmingEvolution.MOD_ID, value = Dist.CLIENT)
public final class GogglesHud {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(FarmingEvolution.MOD_ID, "breeder_goggles");

    private static final int MARGIN = 4;
    private static final int PADDING = 3;
    private static final int LINE_HEIGHT = 10;
    /** Dark enough that the grey labels read against the sky. */
    private static final int BACKGROUND = 0xC0000000;
    private static final int TEXT = 0xFFFFFFFF;

    private GogglesHud() {
    }

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, ID, GogglesHud::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        // A layer of a mod is drawn even with the HUD hidden (F1); the vanilla ones are not.
        // An open screen (inventory, chat) covers the world, and the readout of the world with it.
        if (player == null || minecraft.level == null || minecraft.options.hideGui || minecraft.screen != null
                || minecraft.getDebugOverlay().showDebugScreen() || !BreederGogglesItem.worn(player)) {
            return;
        }
        Font font = minecraft.font;

        List<Component> climate = new ArrayList<>();
        climate.add(Component.translatableWithFallback("goggles." + FarmingEvolution.MOD_ID + ".climate_header", "Climate here")
                .withStyle(Readout.HEADER));
        climate.addAll(Readout.climate(FarmingEvolution.CLIMATE.at(minecraft.level, player.blockPosition())));
        panel(graphics, font, climate, MARGIN, MARGIN);

        HitResult hit = minecraft.hitResult;
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
                && minecraft.level.getBlockEntity(block.getBlockPos()) instanceof PlantBlockEntity plant) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatableWithFallback("goggles." + FarmingEvolution.MOD_ID + ".plant_header", "Plant")
                    .withStyle(Readout.HEADER));
            lines.addAll(Readout.plant(plant));
            panel(graphics, font, lines, MARGIN, MARGIN + panelHeight(climate) + MARGIN);
        }
    }

    private static void panel(GuiGraphics graphics, Font font, List<Component> lines, int x, int y) {
        graphics.fill(x, y, x + panelWidth(font, lines), y + panelHeight(lines), BACKGROUND);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(font, lines.get(i), x + PADDING, y + PADDING + i * LINE_HEIGHT, TEXT, true);
        }
    }

    private static int panelWidth(Font font, List<Component> lines) {
        return lines.stream().mapToInt(font::width).max().orElse(0) + 2 * PADDING;
    }

    private static int panelHeight(List<Component> lines) {
        return lines.size() * LINE_HEIGHT + 2 * PADDING - 1;
    }
}
