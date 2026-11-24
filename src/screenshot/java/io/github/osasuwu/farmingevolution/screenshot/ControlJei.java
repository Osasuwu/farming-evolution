package io.github.osasuwu.farmingevolution.screenshot;

import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * What JEI's item list shows, for the {@code jei} action of the client control (issue #78): the
 * text in its search field and the items that pass it. JEI finds this plugin by its annotation
 * when JEI is loaded (the client runs get JEI, issue #76); without JEI the class is never
 * loaded, and {@link ControlRun} checks for JEI before calling it.
 */
@JeiPlugin
public final class ControlJei implements IModPlugin {
    /** Items listed by name in a reply; the count covers all of them. */
    private static final int LISTED = 20;

    private static volatile IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ControlRun.MOD_ID, "jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    /**
     * Reply lines: the search text, how many items pass it, and the first of them; then the
     * result of the check, which fails the action when it does not hold.
     */
    static void describe(ControlActions.JeiCheck check, StringBuilder out) {
        IJeiRuntime current = runtime;
        if (current == null) {
            throw new ControlActions.ActionException("JEI has not started yet");
        }
        var filter = current.getGuiRuntime().getIngredientFilter();
        List<ItemStack> found = filter.getFilteredItemStacks();
        String search = "\"" + filter.getFilterText() + "\"";
        out.append("jei-search ").append(search).append('\n');
        out.append("jei-found ").append(found.size()).append('\n');
        for (ItemStack stack : found.subList(0, Math.min(LISTED, found.size()))) {
            out.append("jei-item ").append(BuiltInRegistries.ITEM.getKey(stack.getItem()))
                    .append(" \"").append(stack.getHoverName().getString()).append("\"\n");
        }
        switch (check.kind()) {
            case "none" -> {
                if (!found.isEmpty()) {
                    throw new ControlActions.ActionException("JEI finds " + found.size() + " item(s) for " + search
                            + ", the first " + BuiltInRegistries.ITEM.getKey(found.getFirst().getItem()));
                }
                out.append("jei-check none: nothing found\n");
            }
            case "has", "lacks" -> {
                boolean has = found.stream().anyMatch(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(check.item()));
                if (has != check.kind().equals("has")) {
                    throw new ControlActions.ActionException("JEI " + (has ? "finds " : "does not find ") + check.item() + " for " + search);
                }
                out.append("jei-check ").append(check.kind()).append(' ').append(check.item()).append(": ")
                        .append(has ? "found" : "not found").append('\n');
            }
            default -> {
                // list only
            }
        }
    }
}
