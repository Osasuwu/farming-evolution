package io.github.osasuwu.farmingevolution.plant;

import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Environment;
import io.github.osasuwu.farmingevolution.breeding.Individual;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The analyzer, the equipment of the second selection method, crossing (CONCEPT.md, "Методы
 * селекции"). It shows both alleles of a plant or a seed and the odds of the offspring of a
 * pair, and it carries pollen from one plant to another: a plant pollinated with it gives
 * seeds that are crosses with the pollen's plant.
 *
 * <ul>
 * <li>Use on a plant: its alleles, whose pollen it has, the odds of its seeds; with pollen in
 * the analyzer, also the odds of its seeds if pollinated with it.
 * <li>Sneak and use on a plant: take its pollen when the analyzer has none, otherwise
 * pollinate the plant with the analyzer's pollen. The pollen stays in the analyzer.
 * <li>Use with a seed in the other hand: the seed's alleles, and the odds with the pollen.
 * <li>Sneak and use in the air: drop the pollen.
 * </ul>
 *
 * The lines go to the player's chat. They are made on the server, which holds the genomes.
 */
public final class AnalyzerItem extends Item {
    public AnalyzerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof PlantBlockEntity plant)) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (level.isClientSide() || player == null) {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        ItemStack analyzer = context.getItemInHand();
        Optional<PlantBlockEntity.Parents> parents = plant.parents();
        if (parents.isEmpty()) {
            send(player, List.of(Analysis.text("no_genome", "This plant has no genome: it gives no product and no offspring.")));
            return InteractionResult.CONSUME;
        }
        Individual genome = parents.get().first();
        Individual pollen = analyzer.get(FarmingEvolution.POLLEN);
        List<Component> lines = new ArrayList<>();
        if (player.isShiftKeyDown() && pollen == null) {
            analyzer.set(FarmingEvolution.POLLEN, genome);
            lines.add(Analysis.text("took", "Took the pollen of %s", Analysis.brief(genome)));
        } else if (player.isShiftKeyDown()) {
            PlantBlockEntity.Parents pair = PlantBlockEntity.Parents.of(genome, pollen, plant.environment());
            try {
                pair.odds();
                plant.setPollen(pollen);
                lines.add(Analysis.text("pollinate", "Pollinated with %s", Analysis.brief(pollen)));
                lines.addAll(Analysis.odds("seeds", "Odds of the seeds of this plant:", plant.parents().orElseThrow().odds()));
            } catch (IllegalArgumentException e) {
                lines.add(refusal(e));
            }
        } else {
            lines.addAll(Analysis.individual("plant", genome));
            lines.add(Analysis.pollinatedBy(parents.get()));
            lines.addAll(Analysis.odds("seeds", "Odds of the seeds of this plant:", parents.get().odds()));
            if (pollen != null && !plant.pollen().filter(pollen::equals).isPresent()) {
                lines.addAll(withPollen(genome, pollen, plant.environment()));
            }
        }
        send(player, lines);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack analyzer = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        Individual seed = other.is(FarmingEvolution.SEEDS) ? other.get(FarmingEvolution.GENOME) : null;
        Individual pollen = analyzer.get(FarmingEvolution.POLLEN);
        boolean drop = player.isShiftKeyDown() && pollen != null;
        if (!drop && seed == null) {
            return InteractionResultHolder.pass(analyzer);
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.success(analyzer);
        }
        List<Component> lines = new ArrayList<>();
        if (drop) {
            analyzer.remove(FarmingEvolution.POLLEN);
            lines.add(Analysis.text("dropped", "Dropped the pollen"));
        } else {
            lines.addAll(Analysis.individual("seed", seed));
            if (pollen != null) {
                lines.addAll(withPollen(seed, pollen, PlantBlock.environmentAt(level, player.blockPosition())));
            }
        }
        send(player, lines);
        return InteractionResultHolder.consume(analyzer);
    }

    /** A loaded analyzer glints, so the player sees it carries pollen. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(FarmingEvolution.POLLEN);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Individual pollen = stack.get(FarmingEvolution.POLLEN);
        if (pollen != null) {
            tooltip.add(Analysis.text("tooltip", "Pollen: %s", Analysis.brief(pollen)));
        }
    }

    /**
     * The odds of the offspring of the individual pollinated with the pollen under the
     * conditions, or why there are none. A seed in the hand is not planted yet: its odds are
     * shown in the climate where the player stands.
     */
    private static List<Component> withPollen(Individual individual, Individual pollen, Environment environment) {
        try {
            return Analysis.odds("with_pollen", "If pollinated with the analyzer's pollen:",
                    PlantBlockEntity.Parents.of(individual, pollen, environment).odds());
        } catch (IllegalArgumentException e) {
            return List.of(refusal(e));
        }
    }

    private static Component refusal(IllegalArgumentException e) {
        return Analysis.text("refused", "These two cannot be crossed: %s", e.getMessage());
    }

    private static void send(Player player, List<Component> lines) {
        lines.forEach(line -> player.displayClientMessage(line, false));
    }
}
