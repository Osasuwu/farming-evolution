// The mechanical belt of Create is made only of fibre that passes the raw-material requirement
// strong_fiber (CONCEPT.md, "Вертикальный срез", AC3). Every recipe of the belt that Create or
// another mod ships is removed; the one added here keeps the shape of Create's own, six items
// in two rows of three for one belt, so it also fits a 3x2 block of mechanical crafters. The
// ingredient is the item tag the mod builds for the requirement: it holds the products of the
// varieties that pass it, and the fibre of a plant of no variety is in no such tag.
// BeltGameTests expects this recipe id, this shape and exactly 1 belt.
ServerEvents.recipes(event => {
  event.remove({ output: 'create:belt_connector' })
  event.shaped('create:belt_connector', ['FFF', 'FFF'], {
    F: '#farmingevolution:requirement/strong_fiber'
  }).id('farmingevolution:belt_connector')
})
