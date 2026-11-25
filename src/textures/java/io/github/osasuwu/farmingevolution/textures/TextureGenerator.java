package io.github.osasuwu.farmingevolution.textures;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

/**
 * Item textures from grey templates and palettes (issue #19). A template is rows of keys:
 * {@code '.'} is a transparent pixel, a digit {@code n} is the n-th grey of the template. A
 * palette lists colours; its n-th colour replaces the n-th grey. Every pixel comes from the
 * data file (src/textures/textures.json): no image file is read.
 *
 * <p>Run at build time by the task {@code generateTextures}, which puts the output into the
 * mod's resources: {@code java TextureGenerator <textures.json> <resources root>}.
 */
public final class TextureGenerator {
    static final String NAMESPACE = "farmingevolution";

    /** One item: the template it is drawn from and the palette that colours it. */
    public record Item(String id, String template, String palette) { }

    /** The parsed data file. */
    public record Data(Map<String, List<String>> templates, Map<String, int[]> palettes, List<Item> items) { }

    private TextureGenerator() { }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: TextureGenerator <textures.json> <resources root>");
        }
        write(read(Path.of(args[0])), Path.of(args[1]));
    }

    /** Reads the data file; a key, template or palette it does not define stops here. */
    public static Data read(Path file) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        Map<String, List<String>> templates = new LinkedHashMap<>();
        root.getAsJsonObject("templates").entrySet().forEach(entry -> {
            List<String> rows = new ArrayList<>();
            entry.getValue().getAsJsonArray().forEach(row -> rows.add(row.getAsString()));
            templates.put(entry.getKey(), List.copyOf(rows));
        });
        Map<String, int[]> palettes = new LinkedHashMap<>();
        root.getAsJsonObject("palettes").entrySet().forEach(entry -> {
            JsonArray colours = entry.getValue().getAsJsonArray();
            int[] palette = new int[colours.size()];
            for (int i = 0; i < palette.length; i++) {
                String hex = colours.get(i).getAsString();
                if (!hex.matches("[0-9a-fA-F]{6}")) {
                    throw new IllegalArgumentException("palette " + entry.getKey() + ": colour " + hex + " is not six hex digits");
                }
                palette[i] = Integer.parseInt(hex, 16);
            }
            palettes.put(entry.getKey(), palette);
        });
        List<Item> items = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("items")) {
            JsonObject item = element.getAsJsonObject();
            Item parsed = new Item(item.get("item").getAsString(), item.get("template").getAsString(), item.get("palette").getAsString());
            if (!templates.containsKey(parsed.template())) {
                throw new IllegalArgumentException("item " + parsed.id() + ": no template " + parsed.template());
            }
            if (!palettes.containsKey(parsed.palette())) {
                throw new IllegalArgumentException("item " + parsed.id() + ": no palette " + parsed.palette());
            }
            items.add(parsed);
        }
        return new Data(templates, palettes, List.copyOf(items));
    }

    /**
     * The template in the palette: every pixel of grey n gets the palette's n-th colour, fully
     * opaque; {@code '.'} stays transparent. A key the palette has no colour for stops here.
     */
    public static BufferedImage paint(List<String> template, int[] palette) {
        int height = template.size();
        int width = height == 0 ? 0 : template.get(0).length();
        if (width == 0) {
            throw new IllegalArgumentException("empty template");
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            String row = template.get(y);
            if (row.length() != width) {
                throw new IllegalArgumentException("template row " + y + " is " + row.length() + " keys long, not " + width);
            }
            for (int x = 0; x < width; x++) {
                char key = row.charAt(x);
                if (key == '.') {
                    continue;
                }
                if (key < '1' || key > '9') {
                    throw new IllegalArgumentException("template row " + y + ": key '" + key + "' is neither '.' nor a digit 1-9");
                }
                int grey = key - '0';
                if (grey > palette.length) {
                    throw new IllegalArgumentException("template grey " + grey + " has no colour in a palette of " + palette.length);
                }
                image.setRGB(x, y, 0xff000000 | palette[grey - 1]);
            }
        }
        return image;
    }

    /** The PNG bytes of an image. */
    public static byte[] png(BufferedImage image) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            if (!ImageIO.write(image, "png", bytes)) {
                throw new IllegalStateException("no PNG writer");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    /** The texture of an item, as PNG bytes. */
    public static byte[] texture(Data data, Item item) {
        return png(paint(data.templates().get(item.template()), data.palettes().get(item.palette())));
    }

    /** The item model: a flat item with the generated texture. */
    public static String model(Item item) {
        return "{\n  \"parent\": \"minecraft:item/generated\",\n  \"textures\": {\n    \"layer0\": \""
                + NAMESPACE + ":item/" + item.id() + "\"\n  }\n}\n";
    }

    /** Path of the item's texture inside a resources root. */
    public static String texturePath(Item item) {
        return "assets/" + NAMESPACE + "/textures/item/" + item.id() + ".png";
    }

    /** Path of the item's model inside a resources root. */
    public static String modelPath(Item item) {
        return "assets/" + NAMESPACE + "/models/item/" + item.id() + ".json";
    }

    /** Writes the texture and model of every item under the resources root, emptied first. */
    public static void write(Data data, Path root) throws IOException {
        if (Files.exists(root)) {
            try (Stream<Path> old = Files.walk(root)) {
                for (Path path : old.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
        for (Item item : data.items()) {
            Path texture = root.resolve(texturePath(item));
            Files.createDirectories(texture.getParent());
            Files.write(texture, texture(data, item));
            Path model = root.resolve(modelPath(item));
            Files.createDirectories(model.getParent());
            Files.writeString(model, model(item), StandardCharsets.UTF_8);
        }
    }
}
