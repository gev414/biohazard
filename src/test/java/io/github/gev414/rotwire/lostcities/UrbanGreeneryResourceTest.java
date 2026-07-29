package io.github.gev414.rotwire.lostcities;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import mcjty.lostcities.worldgen.lost.regassets.BuildingPartRE;
import mcjty.lostcities.worldgen.lost.regassets.PaletteRE;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrbanGreeneryResourceTest {

    private static final List<String> STREET_TYPES = List.of(
            "full",
            "straight",
            "end",
            "bend",
            "t",
            "none",
            "all"
    );
    private static final List<String> PARKS = List.of(
            "overgrown_park",
            "overgrown_park_arid",
            "overgrown_park_jungle",
            "overgrown_park_snowy"
    );
    private static final Set<String> STREET_TREE_POSITIONS = Set.of(
            "2,2",
            "13,2",
            "2,13",
            "13,13"
    );
    private static final Set<String> PARK_TREE_POSITIONS = Set.of(
            "4,4",
            "11,4",
            "4,11",
            "11,11"
    );
    private static final Map<String, String> STYLE_PARKS = Map.of(
            "/data/lostcities/lostcities/citystyles/citystyle_standard.json",
            "rotwire:urban_greenery/overgrown_park",
            "/data/lostcities/lostcities/citystyles/citystyle_desert.json",
            "rotwire:urban_greenery/overgrown_park_arid",
            "/data/lcmt/lostcities/citystyles/citystyle_standard.json",
            "rotwire:urban_greenery/overgrown_park",
            "/data/lcmt/lostcities/citystyles/citystyle_desert.json",
            "rotwire:urban_greenery/overgrown_park_arid",
            "/data/lcmt/lostcities/citystyles/citystyle_jungle.json",
            "rotwire:urban_greenery/overgrown_park_jungle",
            "/data/lcmt/lostcities/citystyles/citystyle_snowy.json",
            "rotwire:urban_greenery/overgrown_park_snowy"
    );

    @Test
    void baseStreetOverridesKeepRoadsClearAndAddFourPlanterCandidates() {
        for (String streetType : STREET_TYPES) {
            JsonObject part = readJson(baseStreetResource(streetType));
            assertPartDimensions(part, baseStreetResource(streetType));
            assertEquals(
                    STREET_TREE_POSITIONS,
                    markerPositions(part, 1, 'P'),
                    () -> streetType + " has unexpected street-tree positions"
            );
            assertMarkerSupport(part, 1, 'P', 0, 'D');
            assertSaplingPaletteEntry(
                    findPaletteEntry(
                            part.getAsJsonObject("palette")
                                    .getAsJsonArray("palette"),
                            'P'
                    )
            );
        }
    }

    @Test
    void lcmtStreetOverridesPreserveThePinnedStreetPaletteContract() {
        JsonObject palette = readJson(
                "/data/lcmt/lostcities/palettes/streets.json"
        );
        JsonArray entries = palette.getAsJsonArray("palette");
        assertEquals(
                "minecraft:dirt",
                findPaletteEntry(entries, 'U').get("block").getAsString()
        );
        assertSaplingPaletteEntry(findPaletteEntry(entries, 'V'));

        for (String streetType : STREET_TYPES) {
            String resource = lcmtStreetResource(streetType);
            JsonObject part = readJson(resource);
            assertPartDimensions(part, resource);
            assertEquals("lcmt:streets", part.get("refpalette").getAsString());
            assertEquals(
                    STREET_TREE_POSITIONS,
                    markerPositions(part, 1, 'V'),
                    () -> streetType + " has unexpected LCMT tree positions"
            );
            assertMarkerSupport(part, 1, 'V', 0, 'U');
        }
    }

    @Test
    void parkVariantsUseBiomeAppropriateSaplingsOnPlantableBeds() {
        for (String park : PARKS) {
            String resource = parkResource(park);
            JsonObject part = readJson(resource);
            assertPartDimensions(part, resource);
            assertEquals(
                    PARK_TREE_POSITIONS,
                    markerPositions(part, 1, 'P'),
                    () -> park + " has unexpected park-tree positions"
            );
            assertMarkerSupport(part, 1, 'P', 0, 'd');
            assertSaplingPaletteEntry(
                    findPaletteEntry(
                            part.getAsJsonObject("palette")
                                    .getAsJsonArray("palette"),
                            'P'
                    )
            );
        }

        assertSaplingSpecies(
                "overgrown_park_arid",
                Set.of("minecraft:acacia_sapling[stage=1]")
        );
        assertSaplingSpecies(
                "overgrown_park_jungle",
                Set.of(
                        "minecraft:jungle_sapling[stage=1]",
                        "minecraft:oak_sapling[stage=1]",
                        "minecraft:acacia_sapling[stage=1]"
                )
        );
        assertSaplingSpecies(
                "overgrown_park_snowy",
                Set.of(
                        "minecraft:spruce_sapling[stage=1]",
                        "minecraft:birch_sapling[stage=1]"
                )
        );
    }

    @Test
    void everyActiveCityStyleSelectsItsOvergrownPark() {
        STYLE_PARKS.forEach((resource, expectedPark) -> {
            JsonArray parks = readJson(resource)
                    .getAsJsonObject("selectors")
                    .getAsJsonArray("parks");
            Set<String> selectedParks = new HashSet<>();
            for (JsonElement element : parks) {
                selectedParks.add(
                        element.getAsJsonObject()
                                .get("value")
                                .getAsString()
                );
            }
            assertTrue(
                    selectedParks.contains(expectedPark),
                    () -> resource + " does not select " + expectedPark
            );
        });
    }

    @Test
    void pinnedLostCitiesCodecsAcceptEveryGreeneryResource() {
        assertCodecParses(
                PaletteRE.CODEC,
                "/data/lcmt/lostcities/palettes/streets.json"
        );
        for (String streetType : STREET_TYPES) {
            assertCodecParses(
                    BuildingPartRE.CODEC,
                    baseStreetResource(streetType)
            );
            assertCodecParses(
                    BuildingPartRE.CODEC,
                    lcmtStreetResource(streetType)
            );
        }
        for (String park : PARKS) {
            assertCodecParses(BuildingPartRE.CODEC, parkResource(park));
        }
    }

    private static void assertSaplingSpecies(
            String park,
            Set<String> expected
    ) {
        JsonObject part = readJson(parkResource(park));
        JsonObject entry = findPaletteEntry(
                part.getAsJsonObject("palette").getAsJsonArray("palette"),
                'P'
        );
        Set<String> species = new HashSet<>();
        for (JsonElement element : entry.getAsJsonArray("blocks")) {
            String block = element.getAsJsonObject().get("block").getAsString();
            if (!block.equals("minecraft:air")) {
                species.add(block);
            }
        }
        assertEquals(expected, species);
    }

    private static void assertSaplingPaletteEntry(JsonObject entry) {
        int saplings = 0;
        int air = 0;
        int saplingRolls = 0;
        int airRolls = 0;
        for (JsonElement element : entry.getAsJsonArray("blocks")) {
            JsonObject choice = element.getAsJsonObject();
            String block = choice.get("block").getAsString();
            int rolls = choice.get("random").getAsInt();
            if (block.equals("minecraft:air")) {
                air++;
                airRolls += rolls;
            } else {
                assertTrue(
                        block.startsWith("minecraft:")
                                && block.endsWith("_sapling[stage=1]"),
                        () -> "Non-vanilla sapling fallback in tree palette: "
                                + block
                );
                saplings++;
                saplingRolls += rolls;
            }
        }
        assertTrue(saplings > 0, "Tree palette contains no saplings");
        assertEquals(1, air, "Tree palette needs one ruined/empty-planter roll");
        assertTrue(
                saplingRolls + airRolls >= 128,
                "Lost Cities random palettes must fill all 128 rolls"
        );
        assertTrue(
                saplingRolls >= 120,
                "At least 93.75% of planter rolls must select a tree"
        );
        assertTrue(
                airRolls <= 8,
                "Empty planters must occupy at most 6.25% of rolls"
        );
    }

    private static JsonObject findPaletteEntry(
            JsonArray palette,
            char symbol
    ) {
        for (JsonElement element : palette) {
            JsonObject entry = element.getAsJsonObject();
            if (entry.get("char").getAsString().charAt(0) == symbol) {
                return entry;
            }
        }
        throw new AssertionError("Missing palette symbol " + symbol);
    }

    private static void assertMarkerSupport(
            JsonObject part,
            int markerSlice,
            char marker,
            int supportSlice,
            char support
    ) {
        JsonArray slices = part.getAsJsonArray("slices");
        JsonArray markers = slices.get(markerSlice).getAsJsonArray();
        JsonArray supports = slices.get(supportSlice).getAsJsonArray();
        for (int z = 0; z < 16; z++) {
            String markerRow = markers.get(z).getAsString();
            String supportRow = supports.get(z).getAsString();
            for (int x = 0; x < 16; x++) {
                if (markerRow.charAt(x) == marker) {
                    assertEquals(
                            support,
                            supportRow.charAt(x),
                            "Tree marker lacks plantable support at "
                                    + x + "," + z
                    );
                }
            }
        }
    }

    private static Set<String> markerPositions(
            JsonObject part,
            int slice,
            char marker
    ) {
        Set<String> positions = new HashSet<>();
        JsonArray rows = part.getAsJsonArray("slices")
                .get(slice)
                .getAsJsonArray();
        for (int z = 0; z < rows.size(); z++) {
            String row = rows.get(z).getAsString();
            for (int x = 0; x < row.length(); x++) {
                if (row.charAt(x) == marker) {
                    positions.add(x + "," + z);
                }
            }
        }
        return positions;
    }

    private static void assertPartDimensions(
            JsonObject part,
            String resource
    ) {
        assertEquals(16, part.get("xsize").getAsInt(), resource);
        assertEquals(16, part.get("zsize").getAsInt(), resource);
        for (JsonElement slice : part.getAsJsonArray("slices")) {
            JsonArray rows = slice.getAsJsonArray();
            assertEquals(16, rows.size(), resource);
            for (JsonElement row : rows) {
                assertEquals(16, row.getAsString().length(), resource);
            }
        }
    }

    private static String baseStreetResource(String streetType) {
        return "/data/lostcities/lostcities/parts/street_"
                + streetType + ".json";
    }

    private static String lcmtStreetResource(String streetType) {
        return "/data/lcmt/lostcities/parts/street/street_"
                + streetType + "_base.json";
    }

    private static String parkResource(String park) {
        return "/data/rotwire/lostcities/parts/urban_greenery/"
                + park + ".json";
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
        InputStream stream = UrbanGreeneryResourceTest.class
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
