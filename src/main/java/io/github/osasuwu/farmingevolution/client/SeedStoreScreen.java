package io.github.osasuwu.farmingevolution.client;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import io.github.osasuwu.farmingevolution.breeding.Selection;
import io.github.osasuwu.farmingevolution.plant.BreederGogglesItem;
import io.github.osasuwu.farmingevolution.plant.Readout;
import io.github.osasuwu.farmingevolution.store.SeedStore;
import io.github.osasuwu.farmingevolution.store.SeedStoreBlock;
import io.github.osasuwu.farmingevolution.store.SeedStoreBlockEntity;
import io.github.osasuwu.farmingevolution.store.SeedStoreMenu;
import io.github.osasuwu.farmingevolution.store.SeedStoreView;
import io.github.osasuwu.farmingevolution.store.SeedStoreView.Compare;
import io.github.osasuwu.farmingevolution.store.SeedStoreView.Entry;
import io.github.osasuwu.farmingevolution.store.SeedStoreView.Filter;
import io.github.osasuwu.farmingevolution.store.SeedStoreView.Key;
import io.github.osasuwu.farmingevolution.store.SeedStoreView.Sort;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * The window of the seed store (issue #91). Client only: the dedicated server never loads it.
 *
 * <p>From the top: the condition automation follows, with buttons to the previous and the next
 * one; a row of icons, the filter (species, trait, how to compare, value) on the left and the
 * sort (key, direction) on the right, reset at the end; the seeds, one cell per genome, 9 by 4
 * with a scrollbar; the player's inventory. The filter and the sort are the window's alone:
 * they stay while the game runs, for every store, and never reach the server.
 *
 * <p>A cell shows the seed and how many of its genome the store holds; with a trait filter or a
 * trait sort, the trait's expressed value in the top left; a green corner when the seeds pass
 * the condition. Its tooltip shows the genome, as the goggles do, and the count.
 *
 * <p>Every texture is drawn here with flat fills or is a stub sprite of {@code stub-assets.gradle}.
 */
@EventBusSubscriber(modid = FarmingEvolution.MOD_ID, value = Dist.CLIENT)
public final class SeedStoreScreen extends AbstractContainerScreen<SeedStoreMenu> {
    private static final String KEY = "screen." + FarmingEvolution.MOD_ID + ".seed_store.";

    private static final int WIDTH = 194;
    private static final int HEIGHT = 222;
    private static final int CONDITION_Y = 16;
    private static final int TOOLS_Y = 35;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 56;
    private static final int COLUMNS = 9;
    private static final int ROWS = 4;
    private static final int CELL = 18;
    private static final int SCROLL_X = 174;
    private static final int SCROLL_WIDTH = 12;
    private static final int THUMB_HEIGHT = 15;

    private static final int PANEL = 0xFFC6C6C6;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SHADE = 0xFF555555;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int SLOT_SHADE = 0xFF373737;
    private static final int BUTTON = 0xFFDADADA;
    private static final int BUTTON_HOVER = 0xFFEDEDED;
    private static final int BUTTON_OFF = 0xFFA8A8A8;
    private static final int LIT = 0xFF3F8F2B;
    private static final int PASS = 0xFF3FBF3F;
    private static final int TEXT = 0xFF404040;
    private static final int VALUE = 0xFFFFFF55;

    // The filter and the sort, the window's alone: kept while the game runs.
    @Nullable
    private static String species;
    @Nullable
    private static String filterTrait;
    private static Compare compare = Compare.AT_LEAST;
    private static int value;
    private static Key sortKey = Key.SPECIES;
    @Nullable
    private static String sortTrait;
    private static boolean descending;

    private final List<Icon> icons = new ArrayList<>();
    private List<Entry> entries = List.of();
    private List<Entry> shown = List.of();
    private int seenVersion = -1;
    private String seenView = "";
    private int scroll;
    private boolean dragging;

    public SeedStoreScreen(SeedStoreMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelY = SeedStoreMenu.INVENTORY_Y - 10;
    }

    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        event.register(SeedStore.MENU.get(), SeedStoreScreen::new);
    }

    @Override
    protected void init() {
        super.init();
        icons.clear();
        icons.add(new Icon(8, CONDITION_Y, 12, 18, () -> sprite("previous"), () -> "", () -> true, () -> false,
                () -> List.of(text("previous", "Previous condition"), hint("previous_hint", "Shift: the previous kind")),
                (button, shift) -> select(shift ? kindStep(-1) : step(-1))));
        icons.add(new Icon(SCROLL_X, CONDITION_Y, 12, 18, () -> sprite("next"), () -> "", () -> true, () -> false,
                () -> List.of(text("next", "Next condition"), hint("next_hint", "Shift: the next kind")),
                (button, shift) -> select(shift ? kindStep(1) : step(1))));

        icons.add(new Icon(8, TOOLS_Y, 18, 18, () -> sprite("species"), () -> initials(species), () -> true,
                () -> species != null,
                () -> List.of(text("species", "Species: %s", species == null ? text("any", "any") : speciesName(species)),
                        hint("cycle", "Click: next · Right-click: back")),
                (button, shift) -> species = cycle(withNone(SeedStoreView.species()), species, button)));
        icons.add(new Icon(26, TOOLS_Y, 18, 18, () -> sprite("trait"), () -> initials(filterTrait), () -> true,
                () -> filterTrait != null,
                () -> List.of(text("filter_trait", "Filter by trait: %s",
                        filterTrait == null ? text("none", "none") : traitName(filterTrait)),
                        hint("cycle", "Click: next · Right-click: back")),
                (button, shift) -> {
                    filterTrait = cycle(withNone(SeedStoreView.traits()), filterTrait, button);
                    if (filterTrait != null) {
                        int[] range = SeedStoreView.range(filterTrait);
                        value = Mth.clamp(value, range[0], range[1]);
                    }
                }));
        icons.add(new Icon(44, TOOLS_Y, 18, 18, () -> sprite(compare.name().toLowerCase(Locale.ROOT)), () -> "",
                () -> filterTrait != null, () -> false,
                () -> List.of(text("compare", "Compare: %s", compareName(compare)),
                        hint("cycle", "Click: next · Right-click: back")),
                (button, shift) -> compare = Compare.values()[Math.floorMod(compare.ordinal() + (button == 1 ? -1 : 1),
                        Compare.values().length)]));
        icons.add(new Icon(62, TOOLS_Y, 18, 18, null, () -> filterTrait == null ? "-" : String.valueOf(value),
                () -> filterTrait != null, () -> false,
                () -> List.of(text("value", "Value: %s", filterTrait == null ? "-" : value),
                        hint("value_hint", "Click: up · Right-click: down · Scroll")),
                (button, shift) -> changeValue(button == 1 ? -1 : 1)));

        icons.add(new Icon(98, TOOLS_Y, 18, 18, () -> sprite(switch (sortKey) {
            case SPECIES -> "species";
            case COUNT -> "count";
            case TRAIT -> "trait";
        }), () -> sortKey == Key.TRAIT ? initials(sortTrait) : "", () -> true, () -> sortKey != Key.SPECIES,
                () -> List.of(text("sort", "Sort by: %s", sortName()), hint("cycle", "Click: next · Right-click: back")),
                (button, shift) -> cycleSort(button == 1 ? -1 : 1)));
        icons.add(new Icon(116, TOOLS_Y, 18, 18, () -> sprite(descending ? "descending" : "ascending"), () -> "",
                () -> true, () -> descending,
                () -> List.of(descending ? text("descending", "Descending") : text("ascending", "Ascending")),
                (button, shift) -> descending = !descending));
        icons.add(new Icon(SCROLL_X - 6, TOOLS_Y, 18, 18, () -> sprite("reset"), () -> "", () -> true, () -> false,
                () -> List.of(text("reset", "Reset filter and sort")),
                (button, shift) -> {
                    species = null;
                    filterTrait = null;
                    compare = Compare.AT_LEAST;
                    value = 0;
                    sortKey = Key.SPECIES;
                    sortTrait = null;
                    descending = false;
                }));
    }

    // ---- What is shown ----

    private void refresh() {
        String view = species + "|" + filterTrait + "|" + compare + "|" + value + "|" + sortKey + "|" + sortTrait + "|"
                + descending;
        if (menu.version() != seenVersion) {
            seenVersion = menu.version();
            entries = SeedStoreView.entries(menu.storeStacks());
            seenView = "";
        }
        if (!view.equals(seenView)) {
            seenView = view;
            Filter filter = new Filter(species, filterTrait, compare, value);
            Sort sort = new Sort(sortKey, sortKey == Key.TRAIT ? sortTrait : null, descending);
            shown = SeedStoreView.view(entries, filter, sort);
        }
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    private int maxScroll() {
        return Math.max(0, (shown.size() + COLUMNS - 1) / COLUMNS - ROWS);
    }

    @Nullable
    private Entry entryAt(double mouseX, double mouseY) {
        int column = (int) Math.floor((mouseX - leftPos - GRID_X) / CELL);
        int row = (int) Math.floor((mouseY - topPos - GRID_Y) / CELL);
        if (column < 0 || column >= COLUMNS || row < 0 || row >= ROWS) {
            return null;
        }
        int index = (scroll + row) * COLUMNS + column;
        return index < shown.size() ? shown.get(index) : null;
    }

    private boolean overGrid(double mouseX, double mouseY) {
        return inside(mouseX, mouseY, GRID_X, GRID_Y, COLUMNS * CELL, ROWS * CELL);
    }

    private boolean overScrollbar(double mouseX, double mouseY) {
        return inside(mouseX, mouseY, SCROLL_X, GRID_Y, SCROLL_WIDTH, ROWS * CELL);
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        double relX = mouseX - leftPos;
        double relY = mouseY - topPos;
        return relX >= x && relX < x + width && relY >= y && relY < y + height;
    }

    private Selection selection() {
        List<Selection> selections = SeedStoreBlockEntity.selections();
        int index = menu.selectionIndex();
        return selections.get(index >= 0 && index < selections.size() ? index : 0);
    }

    // ---- Drawing ----

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refresh();
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        for (Icon icon : icons) {
            if (icon.over(mouseX - leftPos, mouseY - topPos)) {
                graphics.renderComponentTooltip(font, icon.tooltip.get(), mouseX, mouseY);
                return;
            }
        }
        if (inside(mouseX, mouseY, 22, CONDITION_Y, SCROLL_X - 24, 18)) {
            graphics.renderComponentTooltip(font, List.of(
                    text("condition", "Condition: %s", SeedStoreBlock.describe(selection()).copy()
                            .withStyle(ChatFormatting.WHITE)),
                    hint("condition_hint", "A hopper or a Create funnel takes only the seeds passing it")),
                    mouseX, mouseY);
            return;
        }
        Entry entry = entryAt(mouseX, mouseY);
        if (entry != null) {
            graphics.renderComponentTooltip(font, entryTooltip(entry), mouseX, mouseY);
        }
    }

    private List<Component> entryTooltip(Entry entry) {
        List<Component> lines = new ArrayList<>(getTooltipFromItem(minecraft, entry.stack()));
        Individual genome = entry.genome();
        // With the goggles on, the tooltip event has added the genome already.
        if (genome != null && (minecraft.player == null || !BreederGogglesItem.worn(minecraft.player))) {
            lines.addAll(Math.min(1, lines.size()), Readout.genome(genome));
        }
        lines.add(text("stored", "Stored: %s", Component.literal(String.valueOf(entry.count()))
                .withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
        lines.add(SeedStoreBlockEntity.passes(selection(), entry.stack())
                ? text("passes", "Passes the condition").withStyle(ChatFormatting.GREEN)
                : text("held", "Held by the condition").withStyle(ChatFormatting.RED));
        lines.add(hint("take_hint", "Click: a stack · Right-click: half · Shift-click: to inventory"));
        return lines;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        bevel(graphics, x, y, WIDTH, HEIGHT, PANEL, LIGHT, SHADE);
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                slot(graphics, x + GRID_X + column * CELL, y + GRID_Y + row * CELL);
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                slot(graphics, x + SeedStoreMenu.INVENTORY_X + column * CELL, y + SeedStoreMenu.INVENTORY_Y + row * CELL);
            }
        }
        for (int column = 0; column < 9; column++) {
            slot(graphics, x + SeedStoreMenu.INVENTORY_X + column * CELL, y + SeedStoreMenu.INVENTORY_Y + 58);
        }
        // The condition's field and the scrollbar's track.
        bevel(graphics, x + 21, y + CONDITION_Y, SCROLL_X - 22, 18, SLOT, SLOT_SHADE, LIGHT);
        bevel(graphics, x + SCROLL_X, y + GRID_Y, SCROLL_WIDTH, ROWS * CELL, SLOT, SLOT_SHADE, LIGHT);
        int max = maxScroll();
        int thumbY = y + GRID_Y + 1 + (max == 0 ? 0 : scroll * (ROWS * CELL - 2 - THUMB_HEIGHT) / max);
        bevel(graphics, x + SCROLL_X + 1, thumbY, SCROLL_WIDTH - 2, THUMB_HEIGHT, max == 0 ? BUTTON_OFF : BUTTON,
                LIGHT, SHADE);

        for (Icon icon : icons) {
            drawIcon(graphics, icon, mouseX - leftPos, mouseY - topPos);
        }

        Selection selection = selection();
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int index = (scroll + row) * COLUMNS + column;
                if (index < shown.size()) {
                    drawEntry(graphics, shown.get(index), selection, x + GRID_X + column * CELL + 1,
                            y + GRID_Y + row * CELL + 1);
                }
            }
        }
        if (overGrid(mouseX, mouseY)) {
            int column = (mouseX - x - GRID_X) / CELL;
            int row = (mouseY - y - GRID_Y) / CELL;
            int cellX = x + GRID_X + column * CELL + 1;
            int cellY = y + GRID_Y + row * CELL + 1;
            graphics.fillGradient(cellX, cellY, cellX + 16, cellY + 16, 300, 0x80FFFFFF, 0x80FFFFFF);
        }
    }

    private void drawEntry(GuiGraphics graphics, Entry entry, Selection selection, int x, int y) {
        graphics.renderItem(entry.stack(), x, y);
        graphics.renderItemDecorations(font, entry.stack(), x, y, count(entry.count()));
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        String trait = filterTrait != null ? filterTrait : sortKey == Key.TRAIT ? sortTrait : null;
        if (trait != null) {
            entry.trait(trait).ifPresent(shownValue ->
                    graphics.drawString(font, String.valueOf(shownValue), x, y, VALUE, true));
        }
        if (SeedStoreBlockEntity.passes(selection, entry.stack())) {
            graphics.fill(x + 13, y, x + 16, y + 3, PASS);
        }
        graphics.pose().popPose();
    }

    private void drawIcon(GuiGraphics graphics, Icon icon, int mouseX, int mouseY) {
        boolean enabled = icon.enabled.getAsBoolean();
        int face = !enabled ? BUTTON_OFF : icon.over(mouseX, mouseY) ? BUTTON_HOVER : BUTTON;
        int x = leftPos + icon.x;
        int y = topPos + icon.y;
        if (icon.lit.getAsBoolean()) {
            graphics.fill(x - 1, y - 1, x + icon.width + 1, y + icon.height + 1, LIT);
        }
        bevel(graphics, x, y, icon.width, icon.height, face, LIGHT, SHADE);
        if (icon.sprite != null) {
            graphics.blitSprite(icon.sprite.get(), x + (icon.width - 16) / 2, y + (icon.height - 16) / 2, 16, 16);
        }
        String label = icon.label.get();
        if (!label.isEmpty()) {
            if (icon.sprite == null) {
                graphics.drawString(font, label, x + (icon.width - font.width(label)) / 2 + 1, y + 5,
                        enabled ? TEXT : SHADE, false);
            } else {
                graphics.drawString(font, label, x + icon.width - font.width(label), y + icon.height - 8, LIGHT, true);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        Component condition = SeedStoreBlock.describe(selection());
        int room = SCROLL_X - 28;
        FormattedText fitted = font.width(condition) <= room ? condition
                : FormattedText.composite(font.substrByWidth(condition, room - font.width("...")), FormattedText.of("..."));
        graphics.drawString(font, Language.getInstance().getVisualOrder(fitted), 25, CONDITION_Y + 5, 0xFFFFFFFF, true);
        Component count = text("shown", "%s of %s shown", shown.size(), entries.size());
        graphics.drawString(font, count, WIDTH - 8 - font.width(count), inventoryLabelY, TEXT, false);
    }

    private static void slot(GuiGraphics graphics, int x, int y) {
        bevel(graphics, x - 1, y - 1, CELL, CELL, SLOT, SLOT_SHADE, LIGHT);
    }

    /** A raised or sunk box: the face, a light edge top and left, a dark one bottom and right. */
    private static void bevel(GuiGraphics graphics, int x, int y, int width, int height, int face, int topLeft,
            int bottomRight) {
        graphics.fill(x, y, x + width, y + height, face);
        graphics.fill(x, y, x + width - 1, y + 1, topLeft);
        graphics.fill(x, y, x + 1, y + height - 1, topLeft);
        graphics.fill(x + 1, y + height - 1, x + width, y + height, bottomRight);
        graphics.fill(x + width - 1, y + 1, x + width, y + height, bottomRight);
    }

    /** A count that fits a cell: up to 999 as is, then thousands with one decimal, 1.5k. */
    static String count(int count) {
        if (count < 1000) {
            return String.valueOf(count);
        }
        String thousands = String.format(Locale.ROOT, "%.1f", count / 1000.0);
        return (thousands.endsWith(".0") ? thousands.substring(0, thousands.length() - 2) : thousands) + "k";
    }

    // ---- Input ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        for (Icon icon : icons) {
            if (icon.over(mouseX - leftPos, mouseY - topPos)) {
                if (icon.enabled.getAsBoolean()) {
                    icon.click.click(button, hasShiftDown());
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true;
            }
        }
        if (overScrollbar(mouseX, mouseY)) {
            dragging = true;
            dragTo(mouseY);
            return true;
        }
        if (overGrid(mouseX, mouseY)) {
            clickGrid(mouseX, mouseY, button);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void clickGrid(double mouseX, double mouseY, int button) {
        if (!menu.getCarried().isEmpty()) {
            press(button == 1 ? SeedStoreMenu.PUT_ONE : SeedStoreMenu.PUT_ALL);
            return;
        }
        Entry entry = entryAt(mouseX, mouseY);
        if (entry == null) {
            return;
        }
        if (button == 1) {
            press(SeedStoreMenu.takeHalf(entry.slot()));
        } else if (hasShiftDown()) {
            press(SeedStoreMenu.takeToInventory(entry.slot()));
        } else {
            press(SeedStoreMenu.takeStack(entry.slot()));
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            dragTo(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void dragTo(double mouseY) {
        int max = maxScroll();
        double travel = ROWS * CELL - 2 - THUMB_HEIGHT;
        double at = (mouseY - topPos - GRID_Y - 1 - THUMB_HEIGHT / 2.0) / travel;
        scroll = Mth.clamp((int) Math.round(at * max), 0, max);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int step = scrollY > 0 ? 1 : -1;
        Icon valueIcon = icons.get(5);
        if (valueIcon.over(mouseX - leftPos, mouseY - topPos)) {
            changeValue(step);
            return true;
        }
        if (overGrid(mouseX, mouseY) || overScrollbar(mouseX, mouseY)) {
            scroll = Mth.clamp(scroll - step, 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void press(int button) {
        if (minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    private void select(int index) {
        press(SeedStoreMenu.select(index));
    }

    private int step(int direction) {
        return Math.floorMod(menu.selectionIndex() + direction, SeedStoreBlockEntity.selections().size());
    }

    /**
     * The first condition of the next kind, or of the previous one: hold, a variety, a
     * requirement, a trait, all. Each trait is a kind of its own.
     */
    private int kindStep(int direction) {
        List<Selection> selections = SeedStoreBlockEntity.selections();
        int size = selections.size();
        int current = Math.floorMod(menu.selectionIndex(), size);
        String kind = kind(selections.get(current));
        int index = current;
        if (direction < 0) {
            // Back to the first of the current kind, then one more back.
            while (kind(selections.get(Math.floorMod(index - 1, size))).equals(kind) && Math.floorMod(index - 1, size) != current) {
                index = Math.floorMod(index - 1, size);
            }
            index = Math.floorMod(index - 1, size);
            String previous = kind(selections.get(index));
            while (kind(selections.get(Math.floorMod(index - 1, size))).equals(previous)
                    && Math.floorMod(index - 1, size) != current) {
                index = Math.floorMod(index - 1, size);
            }
            return index;
        }
        for (int i = 1; i < size; i++) {
            int next = Math.floorMod(current + i, size);
            if (!kind(selections.get(next)).equals(kind)) {
                return next;
            }
        }
        return current;
    }

    /** {@code hold}, {@code variety}, {@code requirement}, {@code trait/<trait>} or {@code all}. */
    private static String kind(Selection selection) {
        String[] parts = selection.id().split("/");
        return parts[0].equals("trait") && parts.length > 1 ? parts[0] + "/" + parts[1] : parts[0];
    }

    private static void changeValue(int by) {
        if (filterTrait == null) {
            return;
        }
        int[] range = SeedStoreView.range(filterTrait);
        value = Mth.clamp(value + by, range[0], range[1]);
    }

    /** The sort keys in turn: species, count, then each trait. */
    private static void cycleSort(int direction) {
        List<String> keys = new ArrayList<>(List.of("species", "count"));
        for (String trait : SeedStoreView.traits()) {
            keys.add("trait/" + trait);
        }
        String current = sortKey == Key.TRAIT ? "trait/" + sortTrait : sortKey.name().toLowerCase(Locale.ROOT);
        int index = Math.max(0, keys.indexOf(current));
        String next = keys.get(Math.floorMod(index + direction, keys.size()));
        if (next.startsWith("trait/")) {
            sortKey = Key.TRAIT;
            sortTrait = next.substring("trait/".length());
        } else {
            sortKey = Key.valueOf(next.toUpperCase(Locale.ROOT));
            sortTrait = null;
        }
    }

    private static List<String> withNone(List<String> values) {
        List<String> list = new ArrayList<>();
        list.add(null);
        list.addAll(values);
        return list;
    }

    @Nullable
    private static String cycle(List<String> values, @Nullable String current, int button) {
        int index = Math.max(0, values.indexOf(current));
        return values.get(Math.floorMod(index + (button == 1 ? -1 : 1), values.size()));
    }

    // ---- Words ----

    private Component sortName() {
        return switch (sortKey) {
            case SPECIES -> text("sort_species", "species");
            case COUNT -> text("sort_count", "count");
            case TRAIT -> sortTrait == null ? text("sort_trait", "trait") : traitName(sortTrait);
        };
    }

    private static Component compareName(Compare compare) {
        return switch (compare) {
            case AT_LEAST -> text("at_least", "at least");
            case EXACTLY -> text("exactly", "exactly");
            case AT_MOST -> text("at_most", "at most");
        };
    }

    private static Component speciesName(String species) {
        return Component.literal(species).withStyle(Readout.EXPRESSED);
    }

    private static Component traitName(String trait) {
        return Component.translatableWithFallback("chromosome." + FarmingEvolution.MOD_ID + "." + trait, trait)
                .withStyle(Readout.EXPRESSED);
    }

    /** The first letter of each word of an id: fiber_flax is FF. */
    private static String initials(@Nullable String id) {
        if (id == null) {
            return "";
        }
        StringBuilder initials = new StringBuilder();
        for (String word : id.split("_")) {
            if (!word.isEmpty()) {
                initials.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        return initials.toString();
    }

    private static MutableComponent text(String key, String fallback, Object... args) {
        return Component.translatableWithFallback(KEY + key, fallback, args);
    }

    private static Component hint(String key, String fallback) {
        return text(key, fallback).withStyle(ChatFormatting.DARK_GRAY);
    }

    private static ResourceLocation sprite(String name) {
        return ResourceLocation.fromNamespaceAndPath(FarmingEvolution.MOD_ID, "seed_store/" + name);
    }

    /** What a button does: the mouse button, 0 left and 1 right, and whether Shift is held. */
    @FunctionalInterface
    private interface Click {
        void click(int button, boolean shift);
    }

    /**
     * A button of the window, drawn and clicked by the screen itself: a sprite, or a label
     * when there is none; with a sprite, the label is a short tag in its corner.
     */
    private record Icon(int x, int y, int width, int height, @Nullable Supplier<ResourceLocation> sprite,
            Supplier<String> label, BooleanSupplier enabled, BooleanSupplier lit, Supplier<List<Component>> tooltip,
            Click click) {
        boolean over(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
