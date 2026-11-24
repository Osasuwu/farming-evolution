// The pack has no vanilla food and no vanilla crops (CONCEPT.md, "Игра"): every recipe of any
// mod that gives one of them is removed. Loot, trades and dropped items are the mod's part
// (VanillaRemoval); the list of ways in is docs/vanilla-removal-paths.md.
//
// The list repeats the mod's rule (VanillaRemoval.isRemoved): everything of Minecraft a player
// can eat, the cake, vanilla crops and their seeds. The check pipeline holds the two together:
// VanillaRemovalGameTests.noRecipeGivesVanillaFoodOrCrop reads every loaded recipe.
ServerEvents.recipes(event => {
  const removed = [
    'apple', 'baked_potato', 'beef', 'beetroot', 'beetroot_soup', 'bread', 'cake', 'carrot',
    'chicken', 'chorus_fruit', 'cod', 'cooked_beef', 'cooked_chicken', 'cooked_cod',
    'cooked_mutton', 'cooked_porkchop', 'cooked_rabbit', 'cooked_salmon', 'cookie', 'dried_kelp',
    'enchanted_golden_apple', 'glow_berries', 'golden_apple', 'golden_carrot', 'honey_bottle',
    'melon_slice', 'mushroom_stew', 'mutton', 'ominous_bottle', 'poisonous_potato', 'porkchop',
    'potato', 'pufferfish', 'pumpkin_pie', 'rabbit', 'rabbit_stew', 'rotten_flesh', 'salmon',
    'spider_eye', 'suspicious_stew', 'sweet_berries', 'tropical_fish',
    'wheat', 'wheat_seeds', 'beetroot_seeds', 'melon_seeds', 'pumpkin_seeds', 'cocoa_beans',
    'nether_wart', 'torchflower_seeds', 'pitcher_pod'
  ].map(path => 'minecraft:' + path)

  event.remove({ output: removed })
  // A special recipe names no result, so the filter above does not see it.
  event.remove({ id: 'minecraft:suspicious_stew' })
  // Neither does it see a second or later result of a Create processing recipe: these mill
  // into something else first and give wheat or beetroot seeds on the side.
  for (const id of ['create:milling/beetroot', 'create:milling/fern', 'create:milling/large_fern', 'create:milling/wheat']) {
    event.remove({ id: id })
  }
})
