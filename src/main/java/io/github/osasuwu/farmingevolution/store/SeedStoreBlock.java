package io.github.osasuwu.farmingevolution.store;

import com.mojang.serialization.MapCodec;
import io.github.osasuwu.farmingevolution.FarmingEvolution;
import io.github.osasuwu.farmingevolution.breeding.Selection;
import io.github.osasuwu.farmingevolution.plant.Products;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * The block of the seed store. What the player does with it:
 *
 * <ul>
 * <li>use with an empty hand, sneaking or not: opens the window ({@link SeedStoreMenu}, issue
 * #91), where the player sees the seeds, takes and puts them and sets the condition;
 * <li>use with seeds in the hand: puts the stack in, as much as fits, and shows a line above
 * the hotbar: the condition and how many seeds pass it;
 * <li>use with any other item: the item's own use, e.g. a hopper is placed against the store.
 * </ul>
 *
 * Automation takes seeds out through the item capability only, and only those that pass the
 * condition: a hopper or a Create funnel under or beside it. Breaking the block drops what it
 * holds.
 */
public final class SeedStoreBlock extends BaseEntityBlock {
    private static final MapCodec<SeedStoreBlock> CODEC = simpleCodec(SeedStoreBlock::new);

    public SeedStoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SeedStoreBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (held.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!held.is(FarmingEvolution.SEEDS.get())) {
            // Any other item keeps its own use, e.g. a hopper is placed against the store.
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SeedStoreBlockEntity store) {
            ItemStack left = ItemHandlerHelper.insertItemStacked(store.storage(), held.copy(), false);
            player.setItemInHand(hand, left);
            player.displayClientMessage(status(store), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SeedStoreBlockEntity store) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, opener) -> new SeedStoreMenu(id, inventory, store), getName()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SeedStoreBlockEntity store) {
            for (ItemStack stack : store.contents()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /** "Selection: … · stored N seeds (G genomes), K pass". */
    static Component status(SeedStoreBlockEntity store) {
        List<ItemStack> contents = store.contents();
        int seeds = 0;
        int passing = 0;
        for (ItemStack stack : contents) {
            seeds += stack.getCount();
            if (store.passes(stack)) {
                passing += stack.getCount();
            }
        }
        long genomes = contents.stream().map(stack -> stack.get(FarmingEvolution.GENOME)).distinct().count();
        return Component.translatable("block.farmingevolution.seed_store.status",
                describe(store.selection()), seeds, genomes, passing);
    }

    /** The condition in words: one translation key per kind, with the variety's product named. */
    public static Component describe(Selection selection) {
        String[] parts = selection.id().split("/");
        String key = "block.farmingevolution.seed_store.selection." + parts[0];
        return switch (parts[0]) {
            case "variety" -> Component.translatable(key, Component.translatable(
                    ResourceLocation.fromNamespaceAndPath(FarmingEvolution.MOD_ID, Products.varietyItem(parts[1]))
                            .toLanguageKey("item")));
            case "requirement" -> Component.translatable(key, parts[1]);
            case "trait" -> Component.translatable(key, parts[1], parts[2]);
            default -> Component.translatable(key);
        };
    }
}
