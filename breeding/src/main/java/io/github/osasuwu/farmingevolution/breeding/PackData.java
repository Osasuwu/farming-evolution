package io.github.osasuwu.farmingevolution.breeding;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
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

    /**
     * Reads and checks pack data. Throws PackDataException naming the entry and its fault, so a
     * broken pack stops at startup, not in the middle of a cross.
     */
    public static PackData parse(Reader json) {
        try {
            JsonObject root = JsonParser.parseReader(json).getAsJsonObject();
            Map<String, ChromosomeType> chromosomes = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray("chromosomes")) {
                ChromosomeType type = chromosomeType(element.getAsJsonObject());
                add(chromosomes, "chromosome", type.id(), type);
            }
            Map<String, Species> species = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray("species")) {
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
            return new PackData(chromosomes, species, mutations, varieties);
        } catch (JsonParseException | IllegalStateException | NullPointerException | ClassCastException
                | NumberFormatException | UnsupportedOperationException | IndexOutOfBoundsException e) {
            // A missing key or a value of the wrong JSON type.
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
        String id = json.get("id").getAsString();
        if (id.equals(Breeding.SPECIES)) {
            throw new PackDataException("chromosome id \"" + id + "\" is reserved");
        }
        String entry = "chromosome " + id + ": ";
        String kind = json.get("kind").getAsString();
        switch (kind) {
            case "trait":
                int min = json.get("min").getAsInt();
                int max = json.get("max").getAsInt();
                if (min > max) {
                    throw new PackDataException(entry + "min " + min + " is above max " + max);
                }
                return new ChromosomeType.Trait(id, min, max, chance(json, "shift_chance", entry));
            case "alleles":
                Map<String, Allele.Named> alleles = new LinkedHashMap<>();
                for (JsonElement element : json.getAsJsonArray("alleles")) {
                    JsonObject allele = element.getAsJsonObject();
                    String alleleId = allele.get("id").getAsString();
                    alleles.put(alleleId, new Allele.Named(alleleId, flag(allele, "dominant")));
                }
                return new ChromosomeType.Categorical(id, alleles);
            default:
                throw new PackDataException(entry + "unknown kind " + kind);
        }
    }

    private static Species species(JsonObject json, Map<String, ChromosomeType> chromosomes) {
        String id = json.get("id").getAsString();
        String entry = "species " + id + ": ";
        Map<String, Allele> wild = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> value : json.getAsJsonObject("wild").entrySet()) {
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
        return new Species(id, flag(json, "two_sexed"), new Allele.Named(id, flag(json, "dominant")), wild);
    }

    private static Mutation mutation(JsonObject json, Map<String, Species> species) {
        JsonArray parents = json.getAsJsonArray("parents");
        String first = parents.get(0).getAsString();
        String second = parents.get(1).getAsString();
        String result = json.get("result").getAsString();
        String entry = "mutation " + first + " + " + second + " -> " + result + ": ";
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

        JsonObject conditions = json.has("conditions") ? json.getAsJsonObject("conditions") : new JsonObject();
        Set<String> soils = new LinkedHashSet<>();
        array(conditions, "soil").forEach(soil -> soils.add(soil.getAsString()));
        Map<String, Mutation.Range> climate = new LinkedHashMap<>();
        if (conditions.has("climate")) {
            conditions.getAsJsonObject("climate").entrySet().forEach(reading -> {
                JsonObject range = reading.getValue().getAsJsonObject();
                climate.put(reading.getKey(), new Mutation.Range(
                        range.has("min") ? range.get("min").getAsDouble() : Double.NEGATIVE_INFINITY,
                        range.has("max") ? range.get("max").getAsDouble() : Double.POSITIVE_INFINITY));
            });
        }
        return new Mutation(first, second, result, chance, new Mutation.Condition(soils, climate));
    }

    private static Variety variety(
            JsonObject json, Map<String, ChromosomeType> chromosomes, Map<String, Species> species) {
        String id = json.get("id").getAsString();
        String entry = "variety " + id + ": ";
        String speciesId = json.get("species").getAsString();
        Species of = species.get(speciesId);
        if (of == null) {
            throw new PackDataException(entry + "unknown species " + speciesId);
        }
        Map<String, Integer> thresholds = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> threshold : json.getAsJsonObject("thresholds").entrySet()) {
            String name = threshold.getKey();
            if (!of.wild().containsKey(name) || !(chromosomes.get(name) instanceof ChromosomeType.Trait trait)) {
                throw new PackDataException(entry + speciesId + " has no trait " + name);
            }
            int value = threshold.getValue().getAsInt();
            if (!trait.holds(value)) {
                throw new PackDataException(entry + name + " threshold " + value + " is outside "
                        + trait.min() + ".." + trait.max());
            }
            thresholds.put(name, value);
        }
        return new Variety(id, speciesId, thresholds);
    }

    private static <T> void add(Map<String, T> entries, String kind, String id, T entry) {
        if (entries.putIfAbsent(id, entry) != null) {
            throw new PackDataException("duplicate " + kind + " " + id);
        }
    }

    /** A chance written in pack data: a number from 0 to 1. */
    private static double chance(JsonObject json, String name, String entry) {
        double chance = json.get(name).getAsDouble();
        if (!(chance >= 0 && chance <= 1)) {
            throw new PackDataException(entry + name + " " + chance + " is outside 0..1");
        }
        return chance;
    }

    /** An optional list: a pack without mutations simply leaves the key out. */
    private static JsonArray array(JsonObject json, String name) {
        return json.has(name) ? json.getAsJsonArray(name) : new JsonArray();
    }

    private static boolean flag(JsonObject json, String name) {
        return json.has(name) && json.get(name).getAsBoolean();
    }
}
