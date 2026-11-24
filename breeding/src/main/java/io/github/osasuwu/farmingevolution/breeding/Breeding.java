package io.github.osasuwu.farmingevolution.breeding;

import java.util.EnumSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

/**
 * The inheritance engine: crosses two individuals by the rules in pack data.
 *
 * <p>Each parent passes one gamete: one allele of every chromosome. The two gametes
 * pair up into the offspring's chromosomes.
 */
public final class Breeding {
    /** The id of the chromosome that holds the species. */
    public static final String SPECIES = "species";

    /** Stands for "no hybrid" among the species a hybridization may give. */
    private static final String NO_HYBRID = "";

    private static final Distribution<Boolean> COIN = coin();

    private final PackData data;

    public Breeding(PackData data) {
        this.data = data;
    }

    /** A wild-type individual of the species: both alleles of every chromosome are the wild one. */
    public Individual wild(String speciesId, Sex sex) {
        Map<String, Chromosome> genome = new LinkedHashMap<>();
        haploid(data.species(speciesId)).forEach((type, allele) -> genome.put(type, Chromosome.pure(allele)));
        return new Individual(sex, genome);
    }

    /** One offspring of the pair. The same random state gives the same offspring. */
    public Individual cross(Individual first, Individual second, Environment environment, RandomGenerator random) {
        Species species = breedingSpecies(first, second);
        Map<String, Allele> fromFirst = roll(gametes(first, second, environment), random);
        Map<String, Allele> fromSecond = roll(gametes(second, first, environment), random);
        Map<String, Chromosome> genome = new LinkedHashMap<>();
        for (String type : types(first)) {
            genome.put(type, pairing(fromFirst.get(type), fromSecond.get(type)).sample(random));
        }
        return new Individual(sexes(species).sample(random), genome);
    }

    /**
     * The odds of everything {@link #cross} may give for this pair. Built from the same
     * distributions the cross samples, so an analyzer shows what the roll does.
     */
    public CrossOdds odds(Individual first, Individual second, Environment environment) {
        Species species = breedingSpecies(first, second);
        Gametes fromFirst = gametes(first, second, environment);
        Gametes fromSecond = gametes(second, first, environment);
        Map<String, Distribution<Chromosome>> chromosomes = new LinkedHashMap<>();
        for (String type : types(first)) {
            chromosomes.put(type,
                    Distribution.combine(carried(fromFirst, type), carried(fromSecond, type), Breeding::pairing));
        }
        return new CrossOdds(chromosomes, sexes(species));
    }

    /**
     * The variety whose product the individual gives: of the varieties of its species it fits,
     * the narrowest one, whose region lies inside the regions of all the others. Empty when it
     * fits none: the product is then the species' product of no variety.
     */
    public Optional<Variety> variety(Individual individual) {
        List<Variety> fitting = new ArrayList<>();
        for (Variety variety : data.varieties().values()) {
            if (variety.fits(individual)) {
                fitting.add(variety);
            }
        }
        for (Variety variety : fitting) {
            if (fitting.stream().allMatch(variety::inside)) {
                return Optional.of(variety);
            }
        }
        if (fitting.isEmpty()) {
            return Optional.empty();
        }
        // PackData.parse refuses such varieties; this is pack data built by hand.
        throw new IllegalStateException("no narrowest variety among "
                + fitting.stream().map(Variety::id).toList());
    }

    /**
     * Every condition a machine may select individuals by, in a fixed order: hold, a variety
     * condition per variety, a requirement condition per requirement, for every numeric trait a
     * condition per threshold from one above the trait's minimum to its maximum, and all. See
     * {@link Selection} for what each passes.
     *
     * <p>An individual is first brought to pack data the way a harvest brings it
     * ({@link #restore}, without reporting): one set by other code has not been through reading.
     */
    public List<Selection> selections() {
        List<Selection> selections = new ArrayList<>();
        selections.add(new Selection("hold", individual -> false));
        for (Variety variety : data.varieties().values()) {
            selections.add(new Selection("variety/" + variety.id(),
                    individual -> variety(described(individual)).filter(variety::equals).isPresent()));
        }
        for (Requirement requirement : data.requirements().values()) {
            List<Variety> passing = data.passing(requirement);
            selections.add(new Selection("requirement/" + requirement.id(),
                    individual -> variety(described(individual)).filter(passing::contains).isPresent()));
        }
        for (ChromosomeType type : data.chromosomes().values()) {
            if (type instanceof ChromosomeType.Trait trait) {
                for (int threshold = trait.min() + 1; threshold <= trait.max(); threshold++) {
                    int lowest = threshold;
                    selections.add(new Selection("trait/" + trait.id() + "/" + threshold, individual -> {
                        Individual described = described(individual);
                        return described.genome().containsKey(trait.id()) && described.trait(trait.id()) >= lowest;
                    }));
                }
            }
        }
        selections.add(new Selection("all", individual -> true));
        return List.copyOf(selections);
    }

    /** The condition with this id among {@link #selections()}; empty when pack data has none such. */
    public Optional<Selection> selection(String id) {
        return selections().stream().filter(selection -> selection.id().equals(id)).findFirst();
    }

    private Individual described(Individual individual) {
        return restore(individual, replaced -> { });
    }

    /**
     * A genome read from a save, brought to what pack data describes now: the save may have
     * been written under other pack data. Each replacement is reported in one line.
     *
     * <ul>
     * <li>A species allele pack data does not know gives way to the other allele of the pair.
     * When neither is known, the individual becomes the wild type of the first species of
     * pack data: nothing in the save says which species it should be.
     * <li>The chromosomes become those of the active species: one the species does not have is
     * removed, a missing one is the wild one.
     * <li>An allele its chromosome type does not have, or a trait value outside the range, is
     * replaced with the wild allele of the active species.
     * </ul>
     *
     * A known named allele is taken from pack data, so its dominance is the current one.
     */
    public Individual restore(Individual saved, Consumer<String> replaced) {
        Chromosome savedSpecies = saved.genome().get(SPECIES);
        Species active = savedSpecies == null ? null : knownSpecies(savedSpecies.active());
        Species inactive = savedSpecies == null ? null : knownSpecies(savedSpecies.inactive());
        if (active == null && inactive == null) {
            if (data.species().isEmpty()) {
                throw new IllegalStateException("pack data has no species");
            }
            Species first = data.species().values().iterator().next();
            replaced.accept(SPECIES + ": " + (savedSpecies == null ? "missing"
                    : "unknown species " + text(savedSpecies.active()) + " and " + text(savedSpecies.inactive()))
                    + ", replaced with wild " + first.id());
            return wild(first.id(), saved.sex());
        }
        if (active == null || inactive == null) {
            Species known = active == null ? inactive : active;
            replaced.accept(SPECIES + ": unknown species "
                    + text(active == null ? savedSpecies.active() : savedSpecies.inactive())
                    + ", replaced with " + known.id());
            active = known;
            inactive = known;
        }
        Map<String, Chromosome> genome = new LinkedHashMap<>();
        genome.put(SPECIES, new Chromosome(active.allele(), inactive.allele()));
        for (String id : saved.genome().keySet()) {
            if (!id.equals(SPECIES) && !active.wild().containsKey(id)) {
                replaced.accept(id + ": " + active.id() + " has no such chromosome, removed");
            }
        }
        for (Map.Entry<String, Allele> wild : active.wild().entrySet()) {
            String id = wild.getKey();
            String wildType = ", replaced with " + text(wild.getValue()) + " of wild " + active.id();
            Chromosome chromosome = saved.genome().get(id);
            if (chromosome == null) {
                replaced.accept(id + ": missing" + wildType);
                genome.put(id, Chromosome.pure(wild.getValue()));
                continue;
            }
            List<Allele> alleles = new ArrayList<>();
            for (Allele allele : List.of(chromosome.active(), chromosome.inactive())) {
                try {
                    alleles.add(data.chromosomes().get(id).allele(text(allele)));
                } catch (IllegalArgumentException e) {
                    replaced.accept(id + ": " + e.getMessage() + wildType);
                    alleles.add(wild.getValue());
                }
            }
            genome.put(id, new Chromosome(alleles.get(0), alleles.get(1)));
        }
        return new Individual(saved.sex(), genome);
    }

    /** The species a species allele names, null when pack data has none. */
    private Species knownSpecies(Allele allele) {
        return allele instanceof Allele.Named named ? data.species().get(named.id()) : null;
    }

    /** An allele the way pack data writes it. */
    private static String text(Allele allele) {
        return allele instanceof Allele.Named named ? named.id() : String.valueOf(((Allele.Numeric) allele).value());
    }

    /** The allele of one chromosome a gamete may carry, hybridizations counted in. */
    private Distribution<Allele> carried(Gametes gametes, String type) {
        return Distribution.combine(gametes.hybrid(), Distribution.certain(type), (hybrid, same) ->
                hybrid.equals(NO_HYBRID)
                        ? gametes.inherited().get(type)
                        : Distribution.certain(haploid(data.species(hybrid)).get(type)));
    }

    /**
     * The species that sets the rules of the cross, the first parent's active one.
     * Throws IllegalArgumentException for a pair that cannot breed.
     */
    private Species breedingSpecies(Individual first, Individual second) {
        check("first parent", first);
        check("second parent", second);
        if (!first.genome().keySet().equals(second.genome().keySet())) {
            throw new IllegalArgumentException("parents have different chromosome sets: "
                    + first.genome().keySet() + " and " + second.genome().keySet());
        }
        Species species = data.species(first.species());
        String got = ", got " + first.sex() + " and " + second.sex();
        if (species.twoSexed() && !EnumSet.of(first.sex(), second.sex()).equals(EnumSet.of(Sex.FEMALE, Sex.MALE))) {
            throw new IllegalArgumentException(
                    "a cross of two-sexed " + species.id() + " needs one female and one male" + got);
        }
        if (!species.twoSexed() && (first.sex() != Sex.NONE || second.sex() != Sex.NONE)) {
            throw new IllegalArgumentException(species.id() + " has no sexes" + got);
        }
        return species;
    }

    /**
     * Refuses an individual pack data does not describe: one built by hand, or saved under
     * other pack data. Throws IllegalArgumentException naming the parent and the fault.
     */
    private void check(String who, Individual individual) {
        if (!individual.genome().containsKey(SPECIES)) {
            throw new IllegalArgumentException(who + ": no species chromosome");
        }
        individual.genome().forEach((id, chromosome) -> {
            for (Allele allele : List.of(chromosome.active(), chromosome.inactive())) {
                String text = allele instanceof Allele.Named named
                        ? named.id() : String.valueOf(((Allele.Numeric) allele).value());
                try {
                    if (id.equals(SPECIES)) {
                        data.species(text);
                        continue;
                    }
                    ChromosomeType type = data.chromosomes().get(id);
                    if (type == null) {
                        throw new IllegalArgumentException("unknown chromosome " + id);
                    }
                    try {
                        type.allele(text);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(id + " " + e.getMessage());
                    }
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(who + ": " + e.getMessage());
                }
            }
        });
        // Both species alleles, so a hybrid is held to each of its species.
        Chromosome species = individual.genome().get(SPECIES);
        for (Allele allele : List.of(species.active(), species.inactive())) {
            String id = ((Allele.Named) allele).id();
            Set<String> own = haploid(data.species(id)).keySet();
            if (!individual.genome().keySet().equals(own)) {
                throw new IllegalArgumentException(who + ": chromosomes " + individual.genome().keySet()
                        + " are not those of " + id + " " + own);
            }
        }
    }

    /**
     * The chromosome types of an individual in one fixed order: the species chromosome, then
     * pack data order. Rolls go in this order, so the order a genome was built in changes nothing.
     */
    private List<String> types(Individual individual) {
        List<String> types = new ArrayList<>(List.of(SPECIES));
        for (String id : data.chromosomes().keySet()) {
            if (individual.genome().containsKey(id)) {
                types.add(id);
            }
        }
        return types;
    }

    private static Distribution<Sex> sexes(Species species) {
        return species.twoSexed()
                ? Distribution.combine(Distribution.certain(species), COIN,
                        (any, heads) -> Distribution.certain(heads ? Sex.FEMALE : Sex.MALE))
                : Distribution.certain(Sex.NONE);
    }

    /**
     * What one parent may pass on in a cross with the other.
     *
     * @param hybrid    the species a hybridization turns the gamete into, or NO_HYBRID
     * @param inherited for every chromosome type, the alleles a gamete no hybridization took may carry
     */
    private record Gametes(Distribution<String> hybrid, Map<String, Distribution<Allele>> inherited) {
    }

    private Gametes gametes(Individual parent, Individual other, Environment environment) {
        // Hybridizations are tried in pack data order; the first that happens takes the gamete.
        Map<String, Double> results = new LinkedHashMap<>();
        double untouched = 1;
        for (Hybridization hybridization : data.hybridizations()) {
            if (hybridization.applies(parent.species(), other.species(), environment)) {
                results.merge(hybridization.result(), untouched * hybridization.chance(), Double::sum);
                untouched *= 1 - hybridization.chance();
            }
        }
        results.put(NO_HYBRID, untouched);
        Map<String, Distribution<Allele>> inherited = new LinkedHashMap<>();
        for (String type : types(parent)) {
            inherited.put(type, Distribution.combine(
                    Distribution.certain(parent.genome().get(type)), COIN,
                    (pair, heads) -> transmit(type, heads ? pair.active() : pair.inactive())));
        }
        return new Gametes(Distribution.of(results), inherited);
    }

    private Map<String, Allele> roll(Gametes gametes, RandomGenerator random) {
        String hybrid = gametes.hybrid().sample(random);
        if (!hybrid.equals(NO_HYBRID)) {
            return haploid(data.species(hybrid));
        }
        Map<String, Allele> gamete = new LinkedHashMap<>();
        gametes.inherited().forEach((type, alleles) -> gamete.put(type, alleles.sample(random)));
        return gamete;
    }

    private Distribution<Allele> transmit(String type, Allele allele) {
        return type.equals(SPECIES) ? Distribution.certain(allele) : data.chromosomes().get(type).transmit(allele);
    }

    /**
     * Which of two received alleles becomes the active one: the dominant of a dominant
     * and a recessive, otherwise either with equal odds.
     */
    private static Distribution<Chromosome> pairing(Allele first, Allele second) {
        if (first.equals(second)) {
            return Distribution.certain(Chromosome.pure(first));
        }
        if (first instanceof Allele.Named a && second instanceof Allele.Named b && a.dominant() != b.dominant()) {
            return Distribution.certain(a.dominant() ? new Chromosome(a, b) : new Chromosome(b, a));
        }
        return Distribution.combine(Distribution.certain(first), COIN, (allele, heads) ->
                Distribution.certain(heads ? new Chromosome(first, second) : new Chromosome(second, first)));
    }

    private static Distribution<Boolean> coin() {
        Map<Boolean, Double> sides = new LinkedHashMap<>();
        sides.put(true, 0.5);
        sides.put(false, 0.5);
        return Distribution.of(sides);
    }

    /** The wild-type allele of every chromosome of the species, the species chromosome first. */
    private static Map<String, Allele> haploid(Species species) {
        Map<String, Allele> alleles = new LinkedHashMap<>();
        alleles.put(SPECIES, species.allele());
        alleles.putAll(species.wild());
        return alleles;
    }
}
