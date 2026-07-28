package io.github.gev414.rotwire.lostcities;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import mcjty.lostcities.worldgen.lost.regassets.BuildingPartRE;
import mcjty.lostcities.worldgen.lost.regassets.BuildingRE;
import mcjty.lostcities.worldgen.lost.regassets.PaletteRE;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StairRetrofitResourceTest {

    private record Family(
            int partCount,
            int stairX,
            int stairZ,
            int minFloors,
            int maxFloors,
            int handcraftedMarkers,
            int lootChests
    ) {
    }

    private static final Map<String, Family> FAMILIES = new LinkedHashMap<>();

    static {
        FAMILIES.put("building1", new Family(9, 10, 7, 5, 8, 76, 2));
        FAMILIES.put("building2", new Family(4, 11, 5, 5, 7, 34, 2));
        FAMILIES.put("building3", new Family(4, 11, 6, 5, 8, 37, 1));
        FAMILIES.put("building4", new Family(4, 10, 8, 5, 7, 32, 2));
        FAMILIES.put("building5", new Family(4, 4, 3, 5, 7, 49, 0));
        FAMILIES.put("building6", new Family(4, 5, 3, 5, 7, 55, 2));
        FAMILIES.put("building7", new Family(5, 10, 8, 5, 8, 32, 3));
        FAMILIES.put("building8", new Family(8, 12, 7, 6, 9, 82, 7));
    }

    @Test
    void everyFormerLadderFloorHasAContinuousTwoWideStairFlight() {
        for (Map.Entry<String, Family> entry : FAMILIES.entrySet()) {
            String name = entry.getKey();
            Family family = entry.getValue();

            assertGroundFlight(name, family);
            for (int index = 1; index <= family.partCount(); index++) {
                JsonObject part = readFamilyPart(name, index);
                assertUpperFlight(
                        part,
                        name + "_" + index,
                        family
                );
                assertClearVestibules(
                        part,
                        name + "_" + index,
                        family
                );
            }
            assertClearVestibules(
                    readRetrofitPart(name + "_ground"),
                    name + "_ground",
                    family
            );
            assertRoofLanding(name, family);
        }
    }

    @Test
    void activeRetrofitPartsContainNoLadderMarkers() {
        for (Map.Entry<String, Family> entry : FAMILIES.entrySet()) {
            String name = entry.getKey();
            Family family = entry.getValue();

            assertNoLadders(readRetrofitPart(name + "_ground"), name + "_ground");
            assertNoLadders(readRetrofitPart(name + "_roof"), name + "_roof");
            for (int index = 1; index <= family.partCount(); index++) {
                assertNoLadders(
                        readFamilyPart(name, index),
                        name + "_" + index
                );
            }
        }
    }

    @Test
    void retrofitsPreserveHandcraftedFurnitureAndLootContainers() {
        Set<Character> handcraftedSymbols = readHandcraftedCharacters();
        for (Map.Entry<String, Family> entry : FAMILIES.entrySet()) {
            String name = entry.getKey();
            Family family = entry.getValue();
            int handcrafted = 0;
            int lootChests = 0;
            Set<Character> storage = readHandcraftedStorageCharacters();

            for (int index = 1; index <= family.partCount(); index++) {
                JsonArray slices = readFamilyPart(name, index)
                        .getAsJsonArray("slices");
                int floorStorage = 0;
                for (JsonElement slice : slices) {
                    for (JsonElement row : slice.getAsJsonArray()) {
                        String value = row.getAsString();
                        lootChests += count(value, 'C');
                        for (char symbol : value.toCharArray()) {
                            if (handcraftedSymbols.contains(symbol)) {
                                handcrafted++;
                            }
                            if (storage.contains(symbol)) {
                                floorStorage++;
                            }
                        }
                    }
                }
                int partIndex = index;
                int storageCount = floorStorage;
                assertTrue(
                        storageCount >= 3,
                        () -> name + "_" + partIndex + " has only "
                                + storageCount + " Handcrafted storage markers"
                );
            }

            int handcraftedCount = handcrafted;
            assertTrue(
                    handcraftedCount >= family.handcraftedMarkers(),
                    () -> name
                            + " dropped below its Handcrafted furnishing baseline: "
                            + handcraftedCount + " < "
                            + family.handcraftedMarkers()
            );
            assertEquals(
                    family.lootChests(),
                    lootChests,
                    () -> name + " lost vanilla handcrafted loot chests"
            );
        }
    }

    @Test
    void baseAndLcmtDefinitionsCanOnlySelectStairConnectedParts() {
        for (String namespace : new String[]{"lostcities", "lcmt"}) {
            for (Map.Entry<String, Family> entry : FAMILIES.entrySet()) {
                String name = entry.getKey();
                Family family = entry.getValue();
                String resource = "/data/" + namespace
                        + "/lostcities/buildings/" + name + ".json";
                JsonObject definition = readJson(resource);

                assertTrue(definition.get("overrideFloors").getAsBoolean());
                assertEquals(family.minFloors(), definition.get("minfloors").getAsInt());
                assertEquals(family.maxFloors(), definition.get("maxfloors").getAsInt());
                assertEquals(0, definition.get("mincellars").getAsInt());
                assertEquals(0, definition.get("maxcellars").getAsInt());

                JsonArray parts = definition.getAsJsonArray("parts");
                assertEquals(family.partCount() + 2, parts.size());
                assertEquals(
                        "rotwire:stair_retrofits/" + name + "_ground",
                        parts.get(0).getAsJsonObject().get("part").getAsString()
                );
                assertEquals(0, parts.get(0).getAsJsonObject().get("floor").getAsInt());

                for (int index = 1; index <= family.partCount(); index++) {
                    JsonObject part = parts.get(index).getAsJsonObject();
                    assertFalse(part.get("top").getAsBoolean());
                    assertEquals("1,100", part.get("range").getAsString());
                    assertEquals(
                            "rotwire:" + name + "/" + name + "_" + index,
                            part.get("part").getAsString()
                    );
                }

                JsonObject roof = parts.get(parts.size() - 1).getAsJsonObject();
                assertTrue(roof.get("top").getAsBoolean());
                assertEquals(
                        "rotwire:stair_retrofits/" + name + "_roof",
                        roof.get("part").getAsString()
                );
                for (JsonElement partElement : parts) {
                    String part = partElement.getAsJsonObject()
                            .get("part")
                            .getAsString();
                    assertTrue(
                            part.startsWith("rotwire:"),
                            () -> resource + " still selects an inherited part: " + part
                    );
                }
            }
        }
    }

    @Test
    void pinnedLostCitiesCodecsAcceptAllRetrofitResources() {
        assertCodecParses(
                PaletteRE.CODEC,
                "/data/rotwire/lostcities/palettes/furnished_facades.json"
        );
        for (Map.Entry<String, Family> entry : FAMILIES.entrySet()) {
            String name = entry.getKey();
            Family family = entry.getValue();
            assertCodecParses(
                    BuildingPartRE.CODEC,
                    retrofitPartResource(name + "_ground")
            );
            assertCodecParses(
                    BuildingPartRE.CODEC,
                    retrofitPartResource(name + "_roof")
            );
            for (int index = 1; index <= family.partCount(); index++) {
                assertCodecParses(
                        BuildingPartRE.CODEC,
                        familyPartResource(name, index)
                );
            }
            for (String namespace : new String[]{"lostcities", "lcmt"}) {
                assertCodecParses(
                        BuildingRE.CODEC,
                        "/data/" + namespace
                                + "/lostcities/buildings/" + name + ".json"
                );
            }
        }
    }

    @Test
    void everyFamilyHasARecessedMaterialSpecificFacadeAndOpenEntrance() {
        Map<String, String> expectedMaterials = Map.of(
                "building1", "διη",
                "building2", "βμθ",
                "building3", "γαη",
                "building4", "οεθ",
                "building5", "δαη",
                "building6", "γνη",
                "building7", "πζθ",
                "building8", "αξη"
        );

        for (Map.Entry<String, String> entry : expectedMaterials.entrySet()) {
            String name = entry.getKey();
            JsonObject floor = readFamilyPart(name, 1);
            JsonArray slices = floor.getAsJsonArray("slices");
            String serialized = slices.toString();
            for (char material : entry.getValue().toCharArray()) {
                assertTrue(
                        serialized.indexOf(material) >= 0,
                        () -> name + " is missing façade material " + material
                );
            }
            assertTrue(
                    boundaryAirCount(slices, 2) > 0,
                    () -> name + " still has a completely rectangular façade"
            );

            JsonArray ground = readRetrofitPart(name + "_ground")
                    .getAsJsonArray("slices");
            for (int y = 1; y <= 3; y++) {
                for (int z : new int[]{0, 1}) {
                    String row = ground.get(y).getAsJsonArray()
                            .get(z).getAsString();
                    assertEquals(' ', row.charAt(7), name + " entrance");
                    assertEquals(' ', row.charAt(8), name + " entrance");
                }
            }
        }
    }

    private static void assertGroundFlight(String name, Family family) {
        JsonObject part = readRetrofitPart(name + "_ground");
        assertPartShape(part, name + "_ground");
        JsonArray slices = part.getAsJsonArray("slices");
        int endZ = family.stairZ() + 5;

        for (int z = family.stairZ(); z <= endZ; z++) {
            assertPair(slices, 0, family.stairX(), z, '#', name + "_ground");
        }
        assertFlightSteps(slices, name + "_ground", family);
        assertEquals(20, stairCount(slices));
    }

    private static void assertUpperFlight(
            JsonObject part,
            String partName,
            Family family
    ) {
        assertPartShape(part, partName);
        JsonArray slices = part.getAsJsonArray("slices");
        int endZ = family.stairZ() + 5;

        for (int z = family.stairZ() + 2; z < endZ; z++) {
            assertPair(slices, 0, family.stairX(), z, ' ', partName);
        }
        assertPair(slices, 0, family.stairX(), endZ, 'c', partName);
        assertFlightSteps(slices, partName, family);
        assertEquals(22, stairCount(slices));
    }

    private static void assertRoofLanding(String name, Family family) {
        JsonObject part = readRetrofitPart(name + "_roof");
        assertPartShape(part, name + "_roof");
        JsonArray slices = part.getAsJsonArray("slices");
        int endZ = family.stairZ() + 5;

        for (int z = family.stairZ() + 2; z < endZ; z++) {
            assertPair(slices, 0, family.stairX(), z, ' ', name + "_roof");
        }
        assertPair(slices, 0, family.stairX(), endZ, 'c', name + "_roof");
        assertEquals(2, stairCount(slices));
    }

    private static void assertFlightSteps(
            JsonArray slices,
            String partName,
            Family family
    ) {
        for (int y = 1; y < 6; y++) {
            int lowerZ = family.stairZ() + y - 1;
            assertPair(slices, y, family.stairX(), lowerZ, 'c', partName);
            assertPair(slices, y, family.stairX(), lowerZ + 1, 'd', partName);
        }
    }

    private static void assertClearVestibules(
            JsonObject part,
            String partName,
            Family family
    ) {
        JsonArray slices = part.getAsJsonArray("slices");
        int endZ = family.stairZ() + 5;
        for (int y = 1; y <= 3; y++) {
            for (int z : new int[]{family.stairZ() - 1, endZ + 1}) {
                String row = slices.get(y).getAsJsonArray()
                        .get(z)
                        .getAsString();
                for (int x = family.stairX() - 1;
                     x <= family.stairX() + 2;
                     x++) {
                    assertEquals(
                            ' ',
                            row.charAt(x),
                            partName + " blocks its stair vestibule at "
                                    + "x=" + x + ", y=" + y + ", z=" + z
                    );
                }
            }
        }
        assertTrue(
                hasWalkableEgress(
                        slices,
                        family.stairX(),
                        family.stairZ() - 1,
                        -1
                ),
                partName + " lower stair vestibule is sealed"
        );
        assertTrue(
                hasWalkableEgress(
                        slices,
                        family.stairX(),
                        endZ + 1,
                        1
                ),
                partName + " upper stair vestibule is sealed"
        );
    }

    private static boolean hasWalkableEgress(
            JsonArray slices,
            int stairX,
            int vestibuleZ,
            int outwardZ
    ) {
        int targetZ = vestibuleZ + outwardZ;
        for (int x = stairX - 1; x <= stairX + 2; x++) {
            if (hasBodySpace(slices, x, targetZ)) {
                return true;
            }
        }
        return hasBodySpace(slices, stairX - 2, vestibuleZ)
                || hasBodySpace(slices, stairX + 3, vestibuleZ);
    }

    private static boolean hasBodySpace(JsonArray slices, int x, int z) {
        if (x < 0 || x >= 16 || z < 0 || z >= 16) {
            return false;
        }
        String floor = slices.get(0).getAsJsonArray().get(z).getAsString();
        String feet = slices.get(1).getAsJsonArray().get(z).getAsString();
        String head = slices.get(2).getAsJsonArray().get(z).getAsString();
        return floor.charAt(x) != ' '
                && feet.charAt(x) == ' '
                && head.charAt(x) == ' ';
    }

    private static void assertPair(
            JsonArray slices,
            int y,
            int x,
            int z,
            char expected,
            String partName
    ) {
        String row = slices.get(y).getAsJsonArray().get(z).getAsString();
        assertEquals(expected, row.charAt(x), partName + " left stair at y=" + y);
        assertEquals(expected, row.charAt(x + 1), partName + " right stair at y=" + y);
    }

    private static void assertPartShape(JsonObject part, String partName) {
        assertEquals(16, part.get("xsize").getAsInt());
        assertEquals(16, part.get("zsize").getAsInt());
        assertEquals(
                "rotwire:furnished_facades",
                part.get("refpalette").getAsString()
        );
        JsonArray slices = part.getAsJsonArray("slices");
        assertEquals(6, slices.size(), partName);
        for (JsonElement slice : slices) {
            JsonArray rows = slice.getAsJsonArray();
            assertEquals(16, rows.size(), partName);
            for (JsonElement row : rows) {
                assertEquals(16, row.getAsString().length(), partName);
            }
        }
    }

    private static void assertNoLadders(JsonObject part, String partName) {
        assertPartShape(part, partName);
        for (JsonElement slice : part.getAsJsonArray("slices")) {
            for (JsonElement row : slice.getAsJsonArray()) {
                assertFalse(
                        row.getAsString().contains("l"),
                        () -> "Ladder marker remains in " + partName
                );
            }
        }
    }

    private static int stairCount(JsonArray slices) {
        int count = 0;
        for (JsonElement slice : slices) {
            for (JsonElement row : slice.getAsJsonArray()) {
                String value = row.getAsString();
                count += count(value, 'c');
                count += count(value, 'd');
            }
        }
        return count;
    }

    private static int count(String value, char target) {
        int count = 0;
        for (char symbol : value.toCharArray()) {
            if (symbol == target) {
                count++;
            }
        }
        return count;
    }

    private static Set<Character> readHandcraftedStorageCharacters() {
        JsonObject palette = readJson(
                "/data/rotwire/lostcities/palettes/"
                        + "handcrafted_furnishings.json"
        );
        Set<Character> characters = new java.util.HashSet<>();
        for (JsonElement entry : palette.getAsJsonArray("palette")) {
            JsonObject value = entry.getAsJsonObject();
            String block = value.get("block").getAsString();
            if (block.contains("cupboard")
                    || block.contains("drawer")
                    || block.contains("shelf")
                    || block.contains("nightstand")
                    || block.contains("desk")) {
                characters.add(value.get("char").getAsString().charAt(0));
            }
        }
        assertFalse(characters.isEmpty());
        return characters;
    }

    private static Set<Character> readHandcraftedCharacters() {
        JsonObject palette = readJson(
                "/data/rotwire/lostcities/palettes/"
                        + "handcrafted_furnishings.json"
        );
        Set<Character> characters = new java.util.HashSet<>();
        for (JsonElement entry : palette.getAsJsonArray("palette")) {
            characters.add(
                    entry.getAsJsonObject().get("char")
                            .getAsString().charAt(0)
            );
        }
        return characters;
    }

    private static int boundaryAirCount(JsonArray slices, int y) {
        JsonArray rows = slices.get(y).getAsJsonArray();
        int count = 0;
        for (int position = 0; position < 16; position++) {
            if (rows.get(0).getAsString().charAt(position) == ' ') {
                count++;
            }
            if (rows.get(15).getAsString().charAt(position) == ' ') {
                count++;
            }
            if (rows.get(position).getAsString().charAt(0) == ' ') {
                count++;
            }
            if (rows.get(position).getAsString().charAt(15) == ' ') {
                count++;
            }
        }
        return count;
    }

    private static JsonObject readFamilyPart(String name, int index) {
        return readJson(familyPartResource(name, index));
    }

    private static String familyPartResource(String name, int index) {
        return "/data/rotwire/lostcities/parts/"
                + name + "/" + name + "_" + index + ".json";
    }

    private static JsonObject readRetrofitPart(String name) {
        return readJson(retrofitPartResource(name));
    }

    private static String retrofitPartResource(String name) {
        return "/data/rotwire/lostcities/parts/stair_retrofits/"
                + name + ".json";
    }

    private static <T> void assertCodecParses(Codec<T> codec, String resource) {
        DataResult<T> result = codec.parse(JsonOps.INSTANCE, readJson(resource));
        assertTrue(
                result.result().isPresent(),
                () -> "Lost Cities codec rejected " + resource + ": " + result
        );
    }

    private static JsonObject readJson(String resource) {
        InputStream stream = StairRetrofitResourceTest.class
                .getResourceAsStream(resource);
        assertNotNull(stream, () -> "Missing bundled resource " + resource);
        try (InputStream input = stream;
             InputStreamReader reader = new InputStreamReader(
                     input,
                     StandardCharsets.UTF_8
             )) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception exception) {
            throw new AssertionError("Could not read " + resource, exception);
        }
    }
}
