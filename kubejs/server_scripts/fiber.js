// Milling plant fibre into string. Placeholder balance: the recipe exists so that the check
// pipeline can send a plain item of the mod through a Create machine
// (CreateMachineGameTests.millstoneTurnsFiberIntoString expects exactly 1 string per fibre).
ServerEvents.recipes(event => {
  event.custom({
    type: 'create:milling',
    ingredients: [{ item: 'farmingevolution:fiber' }],
    processing_time: 50,
    results: [{ id: 'minecraft:string' }]
  })
})
