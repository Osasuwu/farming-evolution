// Milling fibre that passes the raw-material requirement strong_fiber into string. The
// ingredient is the item tag the mod builds for the requirement, so the recipe takes the
// product of every variety that passes it and of no other. Placeholder balance: the recipe
// exists so that the check pipeline can send a requirement through a recipe
// (RequirementGameTests.recipeNamingRequirementTagTakesProductOfPassingVarietyOnly expects
// exactly 1 string per fibre).
ServerEvents.recipes(event => {
  event.custom({
    type: 'create:milling',
    ingredients: [{ tag: 'farmingevolution:requirement/strong_fiber' }],
    processing_time: 50,
    results: [{ id: 'minecraft:string' }]
  })
})
