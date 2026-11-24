// The analyzer, the equipment of crossing (CONCEPT.md, "Методы селекции"), is made of Create's
// cogwheels and shafts and of no belt: the belt comes later, from fibre of a variety that
// crossing helps to reach. No recipe of Create is changed.
// CrossingGameTests expects this recipe id, this shape and exactly 1 analyzer.
ServerEvents.recipes(event => {
  event.shaped('farmingevolution:analyzer', ['CSC', ' S '], {
    C: 'create:cogwheel',
    S: 'create:shaft'
  }).id('farmingevolution:analyzer')
})
