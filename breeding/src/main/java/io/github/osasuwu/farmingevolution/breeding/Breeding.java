package io.github.osasuwu.farmingevolution.breeding;

import java.util.EnumSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    /** Stands for "no mutation" among the species a mutation may give. */
    private static final String NO_MUTATION = "";

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

    /** The allele of one chromosome a gamete may carry, mutations counted in. */
    private Distribution<Allele> carried(Gametes gametes, String type) {
        return Distribution.combine(gametes.mutation(), Distribution.certain(type), (mutated, same) ->
                mutated.equals(NO_MUTATION)
                        ? gametes.inherited().get(type)
                        : Distribution.certain(haploid(data.species(mutated)).get(type)));
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
     * @param mutation  the species a mutation turns the gamete into, or NO_MUTATION
     * @param inherited for every chromosome type, the alleles an unmutated gamete may carry
     */
    private record Gametes(Distribution<String> mutation, Map<String, Distribution<Allele>> inherited) {
    }

    private Gametes gametes(Individual parent, Individual other, Environment environment) {
        // Mutations are tried in pack data order; the first that happens takes the gamete.
        Map<String, Double> results = new LinkedHashMap<>();
        double untouched = 1;
        for (Mutation mutation : data.mutations()) {
            if (mutation.applies(parent.species(), other.species(), environment)) {
                results.merge(mutation.result(), untouched * mutation.chance(), Double::sum);
                untouched *= 1 - mutation.chance();
            }
        }
        results.put(NO_MUTATION, untouched);
        Map<String, Distribution<Allele>> inherited = new LinkedHashMap<>();
        for (String type : types(parent)) {
            inherited.put(type, Distribution.combine(
                    Distribution.certain(parent.genome().get(type)), COIN,
                    (pair, heads) -> transmit(type, heads ? pair.active() : pair.inactive())));
        }
        return new Gametes(Distribution.of(results), inherited);
    }

    private Map<String, Allele> roll(Gametes gametes, RandomGenerator random) {
        String mutated = gametes.mutation().sample(random);
        if (!mutated.equals(NO_MUTATION)) {
            return haploid(data.species(mutated));
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
