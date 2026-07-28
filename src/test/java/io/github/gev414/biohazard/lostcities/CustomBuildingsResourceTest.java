package io.github.gev414.biohazard.lostcities;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import mcjty.lostcities.worldgen.lost.regassets.BuildingPartRE;
import mcjty.lostcities.worldgen.lost.regassets.BuildingRE;
import mcjty.lostcities.worldgen.lost.regassets.CityStyleRE;
import mcjty.lostcities.worldgen.lost.regassets.MultiBuildingRE;
import mcjty.lostcities.worldgen.lost.regassets.PaletteRE;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomBuildingsResourceTest {

    private static final List<String> SOLO_BUILDINGS = List.of(
            "quarantine_tower",
            "response_office",
            "stairwell_apartments"
    );
    private static final List<String> HOSPITAL_QUADRANTS = List.of(
            "nw",
            "ne",
            "sw",
            "se"
    );
    private static final List<String> STAGES = List.of(
            "ground",
            "middle",
            "roof"
    );
    private static final Set<String> STAIR_HOSPITAL_QUADRANTS = Set.of(
            "nw",
            "se"
    );
    private static final Set<String> EXPECTED_BUILDINGS = Set.of(
            "biohazard:custom_buildings/quarantine_tower",
            "biohazard:custom_buildings/response_office",
            "biohazard:custom_buildings/stairwell_apartments"
    );
    private static final Set<String> EXPECTED_MULTIBUILDINGS = Set.of(
            "biohazard:custom_buildings/quarantine_hospital",
            "biohazard:custom_buildings/emergency_block"
    );

    @Test
    void generatedPartsAreSixBlockFloorsWithOnlyKnownPaletteSymbols() {
        Set<Character> palette = readPaletteCharacters();

        for (String building : SOLO_BUILDINGS) {
            for (String stage : STAGES) {
                assertValidPart(building + "_" + stage, palette);
            }
        }
        for (String quadrant : HOSPITAL_QUADRANTS) {
            for (String stage : STAGES) {
                assertValidPart(
                        "quarantine_hospital_" + quadrant + "_" + stage,
                        palette
                );
            }
        }
    }

    @Test
    void paletteAndPartsCannotIntroduceLadders() {
        JsonObject palette = readJson(
                "/data/biohazard/lostcities/palettes/custom_buildings.json"
        );
        for (JsonElement entry : palette.getAsJsonArray("palette")) {
            JsonObject value = entry.getAsJsonObject();
            assertFalse(
                    value.has("block")
                            && value.get("block").getAsString().contains("ladder"),
                    () -> "Ladder block in custom building palette: " + value
            );
            assertFalse(
                    value.get("char").getAsString().equals("l"),
                    "Lowercase l is reserved as a forbidden ladder marker"
            );
        }

        for (String building : SOLO_BUILDINGS) {
            for (String stage : STAGES) {
                assertPartContainsNoLadderMarker(building + "_" + stage);
            }
        }
        for (String quadrant : HOSPITAL_QUADRANTS) {
            for (String stage : STAGES) {
                assertPartContainsNoLadderMarker(
                        "quarantine_hospital_" + quadrant + "_" + stage
                );
            }
        }
    }

    @Test
    void everyTowerAndHospitalStairCoreHasTheExpectedContinuousFlights() {
        for (String building : SOLO_BUILDINGS) {
            assertStairCount(building + "_ground", 20);
            assertStairCount(building + "_middle", 22);
            assertStairCount(building + "_roof", 2);
        }

        for (String quadrant : HOSPITAL_QUADRANTS) {
            boolean hasStairs = STAIR_HOSPITAL_QUADRANTS.contains(quadrant);
            assertStairCount(
                    "quarantine_hospital_" + quadrant + "_ground",
                    hasStairs ? 20 : 0
            );
            assertStairCount(
                    "quarantine_hospital_" + quadrant + "_middle",
                    hasStairs ? 22 : 0
            );
            assertStairCount(
                    "quarantine_hospital_" + quadrant + "_roof",
                    hasStairs ? 2 : 0
            );
        }
    }

    @Test
    void everyCustomStairFlightHasClearWalkableVestibules() {
        for (String building : SOLO_BUILDINGS) {
            for (String stage : List.of("ground", "middle")) {
                assertClearCustomStairVestibules(
                        building + "_" + stage
                );
            }
        }
        for (String quadrant : STAIR_HOSPITAL_QUADRANTS) {
            for (String stage : List.of("ground", "middle")) {
                assertClearCustomStairVestibules(
                        "quarantine_hospital_" + quadrant + "_" + stage
                );
            }
        }
    }

    @Test
    void everyOccupiedCustomFloorHasHandcraftedStorage() {
        Set<Character> storage = readCustomStorageCharacters();

        for (String building : SOLO_BUILDINGS) {
            for (String stage : List.of("ground", "middle")) {
                assertMinimumStorage(
                        building + "_" + stage,
                        storage,
                        2
                );
            }
        }
        for (String quadrant : HOSPITAL_QUADRANTS) {
            for (String stage : List.of("ground", "middle")) {
                assertMinimumStorage(
                        "quarantine_hospital_" + quadrant + "_" + stage,
                        storage,
                        2
                );
            }
        }
    }

    @Test
    void hospitalQuadrantsKeepEveryInternalChunkSeamOpen() {
        for (String stage : STAGES) {
            assertOpenEdge("quarantine_hospital_nw_" + stage, "east");
            assertOpenEdge("quarantine_hospital_nw_" + stage, "south");
            assertOpenEdge("quarantine_hospital_ne_" + stage, "west");
            assertOpenEdge("quarantine_hospital_ne_" + stage, "south");
            assertOpenEdge("quarantine_hospital_sw_" + stage, "east");
            assertOpenEdge("quarantine_hospital_sw_" + stage, "north");
            assertOpenEdge("quarantine_hospital_se_" + stage, "west");
            assertOpenEdge("quarantine_hospital_se_" + stage, "north");
        }
    }

    @Test
    void buildingDefinitionsForceMultipleFloorsAndResolveEveryPart() {
        Set<String> buildingNames = new HashSet<>(SOLO_BUILDINGS);
        for (String quadrant : HOSPITAL_QUADRANTS) {
            buildingNames.add("quarantine_hospital_" + quadrant);
        }

        for (String building : buildingNames) {
            JsonObject definition = readJson(
                    "/data/biohazard/lostcities/buildings/custom_buildings/"
                            + building + ".json"
            );
            assertTrue(definition.get("overrideFloors").getAsBoolean());
            assertTrue(definition.get("minfloors").getAsInt() >= 5);
            assertTrue(
                    definition.get("maxfloors").getAsInt()
                            >= definition.get("minfloors").getAsInt()
            );
            if (building.startsWith("quarantine_hospital_")) {
                assertEquals(5, definition.get("minfloors").getAsInt());
                assertEquals(7, definition.get("maxfloors").getAsInt());
            }
            assertEquals(0, definition.get("mincellars").getAsInt());
            assertEquals(0, definition.get("maxcellars").getAsInt());
            assertEquals(
                    "biohazard:custom_buildings",
                    definition.get("refpalette").getAsString()
            );

            JsonArray parts = definition.getAsJsonArray("parts");
            assertEquals(3, parts.size());
            for (JsonElement partElement : parts) {
                String part = partElement.getAsJsonObject()
                        .get("part")
                        .getAsString();
                String prefix = "biohazard:custom_buildings/";
                assertTrue(part.startsWith(prefix), () -> "Unexpected part " + part);
                String path = part.substring(prefix.length());
                assertResourceExists(
                        "/data/biohazard/lostcities/parts/custom_buildings/"
                                + path + ".json"
                );
            }
        }
    }

    @Test
    void multibuildingMatricesMatchTheirDeclaredFootprints() {
        assertMultibuilding("quarantine_hospital", 2, 2);
        assertMultibuilding("emergency_block", 3, 2);
    }

    @Test
    void allActiveCityStylesRegisterTheNewSelectors() {
        for (String style : List.of("citystyle_standard", "citystyle_desert")) {
            assertStyleSelectors(
                    "/data/lostcities/lostcities/citystyles/" + style + ".json"
            );
        }
        for (String style : List.of(
                "citystyle_standard",
                "citystyle_desert",
                "citystyle_jungle",
                "citystyle_snowy"
        )) {
            assertStyleSelectors(
                    "/data/lcmt/lostcities/citystyles/" + style + ".json"
            );
        }
    }

    @Test
    void pinnedLostCitiesCodecsAcceptEveryCustomResource() {
        assertCodecParses(
                PaletteRE.CODEC,
                "/data/biohazard/lostcities/palettes/custom_buildings.json"
        );

        for (String building : SOLO_BUILDINGS) {
            for (String stage : STAGES) {
                assertCodecParses(
                        BuildingPartRE.CODEC,
                        partResource(building + "_" + stage)
                );
            }
            assertCodecParses(BuildingRE.CODEC, buildingResource(building));
        }
        for (String quadrant : HOSPITAL_QUADRANTS) {
            String building = "quarantine_hospital_" + quadrant;
            for (String stage : STAGES) {
                assertCodecParses(
                        BuildingPartRE.CODEC,
                        partResource(building + "_" + stage)
                );
            }
            assertCodecParses(BuildingRE.CODEC, buildingResource(building));
        }

        for (String multibuilding : List.of(
                "quarantine_hospital",
                "emergency_block"
        )) {
            assertCodecParses(
                    MultiBuildingRE.CODEC,
                    "/data/biohazard/lostcities/multibuildings/custom_buildings/"
                            + multibuilding + ".json"
            );
        }
        for (String style : List.of("citystyle_standard", "citystyle_desert")) {
            assertCodecParses(
                    CityStyleRE.CODEC,
                    "/data/lostcities/lostcities/citystyles/" + style + ".json"
            );
        }
        for (String style : List.of(
                "citystyle_standard",
                "citystyle_desert",
                "citystyle_jungle",
                "citystyle_snowy"
        )) {
            assertCodecParses(
                    CityStyleRE.CODEC,
                    "/data/lcmt/lostcities/citystyles/" + style + ".json"
            );
        }
    }

    private static Set<Character> readPaletteCharacters() {
        JsonObject root = readJson(
                "/data/biohazard/lostcities/palettes/custom_buildings.json"
        );
        Set<Character> characters = new HashSet<>();
        for (JsonElement entry : root.getAsJsonArray("palette")) {
            String symbol = entry.getAsJsonObject().get("char").getAsString();
            assertEquals(1, symbol.length(), () -> "Palette symbol is not one char: " + symbol);
            assertTrue(
                    characters.add(symbol.charAt(0)),
                    () -> "Duplicate palette symbol " + symbol
            );
        }
        return characters;
    }

    private static Set<Character> readCustomStorageCharacters() {
        JsonObject root = readJson(
                "/data/biohazard/lostcities/palettes/custom_buildings.json"
        );
        Set<Character> characters = new HashSet<>();
        for (JsonElement entry : root.getAsJsonArray("palette")) {
            JsonObject value = entry.getAsJsonObject();
            if (!value.has("block")) {
                continue;
            }
            String block = value.get("block").getAsString();
            if (block.startsWith("handcrafted:")
                    && (block.contains("cupboard")
                    || block.contains("drawer")
                    || block.contains("shelf")
                    || block.contains("nightstand")
                    || block.contains("desk"))) {
                characters.add(value.get("char").getAsString().charAt(0));
            }
        }
        assertFalse(characters.isEmpty(), "Custom palette has no storage blocks");
        return characters;
    }

    private static void assertValidPart(String partName, Set<Character> palette) {
        JsonObject part = readPart(partName);
        assertEquals(16, part.get("xsize").getAsInt());
        assertEquals(16, part.get("zsize").getAsInt());
        assertEquals(
                "biohazard:custom_buildings",
                part.get("refpalette").getAsString()
        );
        JsonArray slices = part.getAsJsonArray("slices");
        assertEquals(6, slices.size(), () -> partName + " must have six slices");
        for (int y = 0; y < slices.size(); y++) {
            JsonArray rows = slices.get(y).getAsJsonArray();
            assertEquals(16, rows.size(), partName + " slice " + y);
            for (int z = 0; z < rows.size(); z++) {
                String row = rows.get(z).getAsString();
                assertEquals(
                        16,
                        row.length(),
                        partName + " slice " + y + " row " + z
                );
                for (char symbol : row.toCharArray()) {
                    assertTrue(
                            symbol == ' ' || palette.contains(symbol),
                            () -> partName + " uses unknown palette symbol " + symbol
                    );
                }
            }
        }
    }

    private static void assertPartContainsNoLadderMarker(String partName) {
        JsonArray slices = readPart(partName).getAsJsonArray("slices");
        for (JsonElement slice : slices) {
            for (JsonElement row : slice.getAsJsonArray()) {
                assertFalse(
                        row.getAsString().contains("l"),
                        () -> "Ladder marker in " + partName
                );
            }
        }
    }

    private static void assertStairCount(String partName, int expected) {
        int count = 0;
        JsonArray slices = readPart(partName).getAsJsonArray("slices");
        for (JsonElement slice : slices) {
            for (JsonElement row : slice.getAsJsonArray()) {
                String value = row.getAsString();
                for (char symbol : value.toCharArray()) {
                    if (symbol == 'c' || symbol == 'd') {
                        count++;
                    }
                }
            }
        }
        assertEquals(expected, count, () -> "Unexpected stair layout in " + partName);
    }

    private static void assertClearCustomStairVestibules(String partName) {
        JsonArray slices = readPart(partName).getAsJsonArray("slices");
        for (int y = 1; y <= 3; y++) {
            JsonArray rows = slices.get(y).getAsJsonArray();
            for (int z : new int[]{6, 7, 14}) {
                String row = rows.get(z).getAsString();
                for (int x = 9; x <= 12; x++) {
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
                hasWalkableEgress(slices, 10, 6, -1),
                partName + " lower stair vestibule is sealed from the floor"
        );
        assertTrue(
                hasWalkableEgress(slices, 10, 14, 1),
                partName + " upper stair vestibule is sealed from the floor"
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

    private static void assertMinimumStorage(
            String partName,
            Set<Character> storage,
            int minimum
    ) {
        int count = 0;
        for (JsonElement slice : readPart(partName).getAsJsonArray("slices")) {
            for (JsonElement row : slice.getAsJsonArray()) {
                for (char symbol : row.getAsString().toCharArray()) {
                    if (storage.contains(symbol)) {
                        count++;
                    }
                }
            }
        }
        int storageCount = count;
        assertTrue(
                storageCount >= minimum,
                () -> partName + " has only " + storageCount
                        + " Handcrafted storage markers"
        );
    }

    private static void assertOpenEdge(String partName, String edge) {
        JsonArray slices = readPart(partName).getAsJsonArray("slices");
        for (int y = 1; y < slices.size(); y++) {
            JsonArray rows = slices.get(y).getAsJsonArray();
            for (int position = 1; position < 15; position++) {
                char symbol = switch (edge) {
                    case "north" -> rows.get(0).getAsString().charAt(position);
                    case "south" -> rows.get(15).getAsString().charAt(position);
                    case "west" -> rows.get(position).getAsString().charAt(0);
                    case "east" -> rows.get(position).getAsString().charAt(15);
                    default -> throw new IllegalArgumentException("Unknown edge " + edge);
                };
                assertEquals(
                        ' ',
                        symbol,
                        partName + " closes its " + edge
                                + " seam at slice " + y
                                + ", position " + position
                );
            }
        }
    }

    private static void assertMultibuilding(
            String name,
            int expectedX,
            int expectedZ
    ) {
        JsonObject definition = readJson(
                "/data/biohazard/lostcities/multibuildings/custom_buildings/"
                        + name + ".json"
        );
        assertEquals(expectedX, definition.get("dimx").getAsInt());
        assertEquals(expectedZ, definition.get("dimz").getAsInt());
        JsonArray xRows = definition.getAsJsonArray("buildings");
        assertEquals(expectedX, xRows.size());
        for (JsonElement xRow : xRows) {
            JsonArray zRows = xRow.getAsJsonArray();
            assertEquals(expectedZ, zRows.size());
            for (JsonElement buildingElement : zRows) {
                String building = buildingElement.getAsString();
                String prefix = "biohazard:custom_buildings/";
                assertTrue(building.startsWith(prefix));
                assertResourceExists(
                        "/data/biohazard/lostcities/buildings/custom_buildings/"
                                + building.substring(prefix.length())
                                + ".json"
                );
            }
        }
    }

    private static void assertStyleSelectors(String resource) {
        JsonObject selectors = readJson(resource).getAsJsonObject("selectors");
        assertEquals(
                EXPECTED_BUILDINGS,
                selectorValues(selectors.getAsJsonArray("buildings")),
                () -> "Unexpected building selectors in " + resource
        );
        assertEquals(
                EXPECTED_MULTIBUILDINGS,
                selectorValues(selectors.getAsJsonArray("multibuildings")),
                () -> "Unexpected multibuilding selectors in " + resource
        );
    }

    private static Set<String> selectorValues(JsonArray entries) {
        Set<String> values = new HashSet<>();
        for (JsonElement entry : entries) {
            values.add(entry.getAsJsonObject().get("value").getAsString());
        }
        return values;
    }

    private static JsonObject readPart(String partName) {
        return readJson(partResource(partName));
    }

    private static String partResource(String partName) {
        return "/data/biohazard/lostcities/parts/custom_buildings/"
                + partName + ".json";
    }

    private static String buildingResource(String buildingName) {
        return "/data/biohazard/lostcities/buildings/custom_buildings/"
                + buildingName + ".json";
    }

    private static <T> void assertCodecParses(
            Codec<T> codec,
            String resource
    ) {
        DataResult<T> result = codec.parse(JsonOps.INSTANCE, readJson(resource));
        assertTrue(
                result.result().isPresent(),
                () -> "Lost Cities codec rejected " + resource + ": " + result
        );
    }

    private static JsonObject readJson(String resource) {
        InputStream stream = CustomBuildingsResourceTest.class
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

    private static void assertResourceExists(String resource) {
        assertNotNull(
                CustomBuildingsResourceTest.class.getResource(resource),
                () -> "Missing bundled resource " + resource
        );
    }
}
