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
 * panels: the climate of the player's place in the top left corner, and the plant of the mod
 * the player looks at to the right of the crosshair. The lines are {@link Readout}'s; this
 * class only places them. Client only: the dedicated server never loads it.
 *
 * <p>The plant panel stays inside the screen and above the hotbar, the hearts and the
 * experience bar at every GUI scale; the climate panel is in a corner nothing of the vanilla
 * HUD uses while the debug screen is closed, and both hide with the debug screen and F1.
 */
@EventBusSubscriber(modid = FarmingEvolution.MOD_ID, value = Dist.CLIENT)
public final class GogglesHud {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(FarmingEvolution.MOD_ID, "breeder_goggles");

    /** The height the hotbar, the hearts, the food and the experience bar take at the bottom. */
    private static final int BOTTOM_RESERVED = 40;

    /** The distance of the plant panel from the crosshair. */
    private static final int CROSSHAIR_GAP = 12;

    private static final int MARGIN = 4;
    private static final int PADDING = 3;
    private static final int LINE_HEIGHT = 10;
    private static final int BACKGROUND = 0x90000000;
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
        if (player == null || minecraft.level == null || minecraft.options.hideGui
                || minecraft.getDebugOverlay().showDebugScreen() || !BreederGogglesItem.worn(player)) {
            return;
        }
        Font font = minecraft.font;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

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
            int panelWidth = panelWidth(font, lines);
            int panelHeight = panelHeight(lines);
            int x = Math.max(MARGIN, Math.min(width / 2 + CROSSHAIR_GAP, width - MARGIN - panelWidth));
            int y = Math.max(MARGIN, Math.min(height / 2 - panelHeight / 2, height - BOTTOM_RESERVED - panelHeight));
            panel(graphics, font, lines, x, y);
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
