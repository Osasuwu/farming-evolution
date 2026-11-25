package io.github.osasuwu.farmingevolution.textures;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.osasuwu.farmingevolution.textures.TextureGenerator.Data;
import io.github.osasuwu.farmingevolution.textures.TextureGenerator.Item;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The texture generator of issue #19. The data file and the mod jar come from the build
 * (system properties set by the Gradle task texturesTest).
 */
class TextureGeneratorTest {

    private static Data data() throws IOException {
        return TextureGenerator.read(Path.of(System.getProperty("farmingevolution.textures")));
    }

    @Test
    void paintGivesEveryGreyThePaletteColourOfItsNumber() {
        BufferedImage image = TextureGenerator.paint(List.of(".12", "21."), new int[] {0x112233, 0xa0b0c0});

        assertEquals(3, image.getWidth());
        assertEquals(2, image.getHeight());
        assertEquals(0x00000000, image.getRGB(0, 0));
        assertEquals(0xff112233, image.getRGB(1, 0));
        assertEquals(0xffa0b0c0, image.getRGB(2, 0));
        assertEquals(0xffa0b0c0, image.getRGB(0, 1));
        assertEquals(0xff112233, image.getRGB(1, 1));
        assertEquals(0x00000000, image.getRGB(2, 1));
    }

    @Test
    void twoRunsWriteTheSameFiles(@TempDir Path first, @TempDir Path second) throws IOException {
        Data data = data();
        TextureGenerator.write(data, first);
        TextureGenerator.write(data, second);

        Map<String, byte[]> one = files(first);
        Map<String, byte[]> two = files(second);
        assertEquals(2 * data.items().size(), one.size(), "a texture and a model per item: " + one.keySet());
        assertEquals(one.keySet(), two.keySet());
        one.forEach((path, bytes) -> assertArrayEquals(bytes, two.get(path), path));
    }

    @Test
    void atLeastThreeItemsShareATemplateAndDifferInColourOnly() throws IOException {
        Data data = data();
        Map<String, List<Item>> byTemplate = data.items().stream()
                .collect(Collectors.groupingBy(Item::template, LinkedHashMap::new, Collectors.toList()));
        List<Item> shared = byTemplate.values().stream().max((a, b) -> Integer.compare(a.size(), b.size())).orElseThrow();
        assertTrue(shared.size() >= 3, "items of the largest template: " + shared);

        List<BufferedImage> images = new ArrayList<>();
        for (Item item : shared) {
            images.add(ImageIO.read(new ByteArrayInputStream(TextureGenerator.texture(data, item))));
        }
        for (int i = 0; i < shared.size(); i++) {
            for (int j = i + 1; j < shared.size(); j++) {
                String pair = shared.get(i).id() + " and " + shared.get(j).id();
                assertEquals(alphaMask(images.get(i)), alphaMask(images.get(j)), "shape of " + pair);
                assertFalse(Arrays.equals(argb(images.get(i)), argb(images.get(j))), "colours of " + pair + " are the same");
            }
        }
    }

    @Test
    void everyItemHasItsModelAndTextureInTheModJar() throws IOException {
        Data data = data();
        assertFalse(data.items().isEmpty());
        try (ZipFile jar = new ZipFile(System.getProperty("farmingevolution.modJar"))) {
            for (Item item : data.items()) {
                ZipEntry model = jar.getEntry("assets/farmingevolution/models/item/" + item.id() + ".json");
                assertNotNull(model, "no model of " + item.id() + " in " + jar.getName());
                JsonObject json = JsonParser.parseString(
                        new String(jar.getInputStream(model).readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                assertEquals("minecraft:item/generated", json.get("parent").getAsString(), item.id());
                assertEquals("farmingevolution:item/" + item.id(),
                        json.getAsJsonObject("textures").get("layer0").getAsString(), item.id());

                ZipEntry texture = jar.getEntry("assets/farmingevolution/textures/item/" + item.id() + ".png");
                assertNotNull(texture, "no texture of " + item.id() + " in " + jar.getName());
                assertArrayEquals(TextureGenerator.texture(data, item), jar.getInputStream(texture).readAllBytes(),
                        "texture of " + item.id() + " in the jar is not the generator's");
            }
        }
    }

    private static Map<String, byte[]> files(Path root) throws IOException {
        Map<String, byte[]> files = new TreeMap<>();
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                files.put(root.relativize(path).toString().replace('\\', '/'), Files.readAllBytes(path));
            }
        }
        return files;
    }

    private static int[] argb(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    private static List<Boolean> alphaMask(BufferedImage image) {
        return Arrays.stream(argb(image)).mapToObj(pixel -> (pixel >>> 24) != 0).toList();
    }
}
