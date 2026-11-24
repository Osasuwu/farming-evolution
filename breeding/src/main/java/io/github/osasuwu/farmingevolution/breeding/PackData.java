package io.github.osasuwu.farmingevolution.breeding;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything the engine knows about the pack: read once at game startup from one JSON document.
 * Maps keep the order of the document, so a fixed seed gives the same rolls on every run.
 */
public record PackData(
        Map<String, ChromosomeType> chromosomes, Map<String, Species> species, List<Mutation> mutations,
        Map<String, Variety> varieties) {

    public PackData {
        chromosomes = Collections.unmodifiableMap(new LinkedHashMap<>(chromosomes));
        species = Collections.unmodifiableMap(new LinkedHashMap<>(species));
        mutations = List.copyOf(mutations);
        varieties = Collections.unmodifiableMap(new LinkedHashMap<>(varieties));
    }

    /**
     * Reads and checks pack data. Throws PackDataException naming the entry and its fault, so a
     * broken pack stops at startup, not in the middle of a cross.
     */
    public static PackData parse(Reader json) {
        try {
            JsonObject root = JsonParser.parseReader(json).getAsJsonObject();
            String entry = "pack data: ";
            known(root, entry, "chromosomes", "species", "mutations", "varieties");
            Map<String, ChromosomeType> chromosomes = new LinkedHashMap<>();
            for (JsonElement element : get(root, "chromosomes", entry).getAsJsonArray()) {
                ChromosomeType type = chromosomeType(element.getAsJsonObject());
                add(chromosomes, "chromosome", type.id(), type);
            }
            Map<String, Species> species = new LinkedHashMap<>();
            for (JsonElement element : get(root, "species", entry).getAsJsonArray()) {
                Species parsed = species(element.getAsJsonObject(), chromosomes);
                add(species, "species", parsed.id(), parsed);
            }
            List<Mutation> mutations = new ArrayList<>();
            for (JsonElement element : array(root, "mutations")) {
                mutations.add(mutation(element.getAsJsonObject(), species));
            }
            Map<String, Variety> varieties = new LinkedHashMap<>();
            for (JsonElement element : array(root, "varieties")) {
                Variety variety = variety(element.getAsJsonObject(), chromosomes, species);
                add(varieties, "variety", variety.id(), variety);
            }
            checkRegions(varieties);
            return new PackData(chromosomes, species, mutations, varieties);
        } catch (JsonParseException | IllegalStateException | NumberFormatException
                | UnsupportedOperationException | IndexOutOfBoundsException e) {
            // Not JSON, or a value of the wrong JSON type.
            throw new PackDataException("malformed pack data: " + e);
        }
    }

    /** The species with this id; throws IllegalArgumentException when pack data has none. */
    public Species species(String id) {
        Species found = species.get(id);
        if (found == null) {
            throw new IllegalArgumentException("unknown species: " + id);
        }
        return found;
    }

    private static ChromosomeType chromosomeType(JsonObject json) {
        String id = id(json, "chromosome");
        if (id.equals(Breeding.SPECIES)) {
            throw new PackDataException("chromosome id \"" + id + "\" is reserved");
        }
        String entry = "chromosome " + id + ": ";
        String kind = get(json, "kind", entry).getAsString();
        switch (kind) {
            case "trait":
                known(json, entry, "id", "kind", "min", "max", "shift_chance");
                int min = whole(get(json, "min", entry), entry + "min");
                int max = whole(get(json, "max", entry), entry + "max");
                if (min > max) {
                    throw new PackDataException(entry + "min " + min + " is above max " + max);
                }
                return new ChromosomeType.Trait(id, min, max, chance(json, "shift_chance", entry));
            case "alleles":
                known(json, entry, "id", "kind", "alleles");
                Map<String, Allele.Named> alleles = new LinkedHashMap<>();
                for (JsonElement element : get(json, "alleles", entry).getAsJsonArray()) {
                    JsonObject allele = element.getAsJsonObject();
                    known(allele, entry, "id", "dominant");
                    String alleleId = get(allele, "id", entry).getAsString();
                    if (alleles.put(alleleId, new Allele.Named(alleleId, flag(allele, "dominant", entry))) != null) {
                        throw new PackDataException(entry + "duplicate allele " + alleleId);
                    }
                }
                return new ChromosomeType.Categorical(id, alleles);
            default:
                throw new PackDataException(entry + "unknown kind " + kind);
        }
    }

    private static Species species(JsonObject json, Map<String, ChromosomeType> chromosomes) {
        String id = id(json, "species");
        String entry = "species " + id + ": ";
        known(json, entry, "id", "two_sexed", "dominant", "wild");
        Map<String, Allele> wild = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> value : get(json, "wild", entry).getAsJsonObject().entrySet()) {
            ChromosomeType type = chromosomes.get(value.getKey());
            if (type == null) {
                throw new PackDataException(entry + "unknown chromosome " + value.getKey());
            }
            try {
                wild.put(value.getKey(), type.allele(value.getValue().getAsString()));
            } catch (IllegalArgumentException e) {
                throw new PackDataException(entry + value.getKey() + " " + e.getMessage());
            }
        }
        return new Species(id, flag(json, "two_sexed", entry),
                new Allele.Named(id, flag(json, "dominant", entry)), wild);
    }

    private static Mutation mutation(JsonObject json, Map<String, Species> species) {
        JsonArray parents = get(json, "parents", "mutation: ").getAsJsonArray();
        String first = parents.get(0).getAsString();
        String second = parents.get(1).getAsString();
        String result = get(json, "result", "mutation: ").getAsString();
        String entry = "mutation " + first + " + " + second + " -> " + result + ": ";
        known(json, entry, "parents", "result", "chance", "conditions");
        for (String id : List.of(first, second, result)) {
            if (!species.containsKey(id)) {
                throw new PackDataException(entry + "unknown species " + id);
            }
        }
        double chance = chance(json, "chance", entry);
        // A gamete of the result species pairs with a gamete of either parent, so all three
        // must carry the same chromosomes and agree on having sexes.
        Species base = species.get(first);
        for (String id : List.of(second, result)) {
            Species other = species.get(id);
            if (!base.wild().keySet().equals(other.wild().keySet())) {
                throw new PackDataException(entry + first + " and " + id + " have different chromosome sets");
            }
            if (base.twoSexed() != other.twoSexed()) {
                throw new PackDataException(entry + first + " and " + id + " differ in two_sexed");
            }
        }

        JsonObject conditions = json.has("conditions") ? json.get("conditions").getAsJsonObject() : new JsonObject();
        known(conditions, entry + "conditions: ", "soil", "climate");
        Set<String> soils = new LinkedHashSet<>();
        array(conditions, "soil").forEach(soil -> soils.add(soil.getAsString()));
        Map<String, Mutation.Range> climate = new LinkedHashMap<>();
        JsonObject readings = conditions.has("climate") ? conditions.get("climate").getAsJsonObject() : new JsonObject();
        for (Map.Entry<String, JsonElement> reading : readings.entrySet()) {
            JsonObject range = reading.getValue().getAsJsonObject();
            known(range, entry + reading.getKey() + ": ", "min", "max");
            double min = range.has("min") ? range.get("min").getAsDouble() : Double.NEGATIVE_INFINITY;
            double max = range.has("max") ? range.get("max").getAsDouble() : Double.POSITIVE_INFINITY;
            // Written so that a NaN bound is refused too.
            if (!(min <= max)) {
                throw new PackDataException(entry + reading.getKey() + " range " + min + ".." + max + " is empty");
            }
            climate.put(reading.getKey(), new Mutation.Range(min, max));
        }
        return new Mutation(first, second, result, chance, new Mutation.Condition(soils, climate));
    }

    private static Variety variety(
            JsonObject json, Map<String, ChromosomeType> chromosomes, Map<String, Species> species) {
        String id = id(json, "variety");
        String entry = "variety " + id + ": ";
        known(json, entry, "id", "species", "thresholds");
        String speciesId = get(json, "species", entry).getAsString();
        Species of = species.get(speciesId);
        if (of == null) {
            throw new PackDataException(entry + "unknown species " + speciesId);
        }
        Map<String, Integer> thresholds = new LinkedHashMap<>();
        Map<String, Integer> ceilings = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> threshold : get(json, "thresholds", entry).getAsJsonObject().entrySet()) {
            String name = threshold.getKey();
            if (!of.wild().containsKey(name) || !(chromosomes.get(name) instanceof ChromosomeType.Trait trait)) {
                throw new PackDataException(entry + speciesId + " has no trait " + name);
            }
            // A bare number is "not below"; an object sets "min" (not below), "max" (not above) or both.
            if (!threshold.getValue().isJsonObject()) {
                thresholds.put(name, bound(threshold.getValue(), entry + name + " threshold", trait, trait.min()));
                continue;
            }
            JsonObject bounds = threshold.getValue().getAsJsonObject();
            known(bounds, entry + name + ": ", "min", "max");
            if (bounds.has("min")) {
                thresholds.put(name, bound(bounds.get("min"), entry + name + " min", trait, trait.min()));
            }
            if (bounds.has("max")) {
                ceilings.put(name, bound(bounds.get("max"), entry + name + " max", trait, trait.max()));
            }
            if (!bounds.has("min") && !bounds.has("max")) {
                throw new PackDataException(entry + name + " sets neither min nor max");
            }
            if (bounds.has("min") && bounds.has("max") && thresholds.get(name) > ceilings.get(name)) {
                throw new PackDataException(entry + name + " min " + thresholds.get(name)
                        + " is above max " + ceilings.get(name));
            }
        }
        return new Variety(id, speciesId, thresholds, ceilings);
    }

    /**
     * One bound of a variety. It must narrow the trait's range: a bound on the open edge of the
     * range (the range's min for "not below", its max for "not above") bounds nothing, and two
     * entries that differ only in such a bound would be one region written twice.
     */
    private static int bound(JsonElement json, String what, ChromosomeType.Trait trait, int openEdge) {
        int value = whole(json, what);
        String range = trait.min() + ".." + trait.max();
        if (!trait.holds(value)) {
            throw new PackDataException(what + " " + value + " is outside " + range);
        }
        if (value == openEdge) {
            throw new PackDataException(what + " " + value + " does not narrow " + range);
        }
        return value;
    }

    /**
     * Makes sure a plant never fits two varieties neither of which is the narrower one: any two
     * varieties of one species are apart, nested, or a third variety has exactly the region
     * they share. Two varieties with the same region are refused outright.
     */
    private static void checkRegions(Map<String, Variety> varieties) {
        List<Variety> all = List.copyOf(varieties.values());
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                Variety first = all.get(i);
                Variety second = all.get(j);
                if (!first.species().equals(second.species()) || !first.overlaps(second)) {
                    continue;
                }
                String pair = "varieties " + first.id() + " and " + second.id() + " of " + first.species();
                boolean firstInside = first.inside(second);
                boolean secondInside = second.inside(first);
                if (firstInside && secondInside) {
                    throw new PackDataException(pair + " have the same region");
                }
                if (!firstInside && !secondInside && all.stream().noneMatch(third ->
                        third.species().equals(first.species()) && third.isSharedRegionOf(first, second))) {
                    throw new PackDataException(pair + " overlap, neither lies inside the other, and no variety of "
                            + first.species() + " has the region they share");
                }
            }
        }
    }

    private static <T> void add(Map<String, T> entries, String kind, String id, T entry) {
        if (entries.putIfAbsent(id, entry) != null) {
            throw new PackDataException("duplicate " + kind + " " + id);
        }
    }

    /** The id of an entry. An empty one is refused: nothing could refer to it. */
    private static String id(JsonObject json, String kind) {
        String id = get(json, "id", kind + ": ").getAsString();
        if (id.isEmpty()) {
            throw new PackDataException(kind + ": empty id");
        }
        return id;
    }

    private static JsonElement get(JsonObject json, String name, String entry) {
        JsonElement value = json.get(name);
        if (value == null) {
            throw new PackDataException(entry + "missing key " + name);
        }
        return value;
    }

    /** Refuses a key the entry does not have: a misspelled optional key would otherwise load as different rules. */
    private static void known(JsonObject json, String entry, String... keys) {
        for (String key : json.keySet()) {
            if (!List.of(keys).contains(key)) {
                throw new PackDataException(entry + "unknown key " + key);
            }
        }
    }

    /** A whole number; 1.9 is refused, not cut down to 1. */
    private static int whole(JsonElement value, String what) {
        try {
            return value.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException e) {
            throw new PackDataException(what + " " + value.getAsString() + " is not a whole number");
        }
    }

    /** A chance written in pack data: a number from 0 to 1. */
    private static double chance(JsonObject json, String name, String entry) {
        double chance = get(json, name, entry).getAsDouble();
        if (!(chance >= 0 && chance <= 1)) {
            throw new PackDataException(entry + name + " " + chance + " is outside 0..1");
        }
        return chance;
    }

    /** An optional list: a pack without mutations simply leaves the key out. */
    private static JsonArray array(JsonObject json, String name) {
        return json.has(name) ? json.get(name).getAsJsonArray() : new JsonArray();
    }

    /** An optional true-or-false key, false when left out. */
    private static boolean flag(JsonObject json, String name, String entry) {
        JsonElement value = json.get(name);
        if (value == null) {
            return false;
        }
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new PackDataException(entry + name + " must be true or false");
        }
        return value.getAsBoolean();
    }
}
