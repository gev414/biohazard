package io.github.gev414.rotwire.quest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestDefaultsResourceTest {

    private static final List<String> QUEST_FILES = List.of(
            "data.snbt",
            "chapter_groups.snbt",
            "chapters/survivor_network.snbt",
            "chapters/medic.snbt",
            "chapters/quartermaster.snbt",
            "chapters/arms_broker.snbt",
            "chapters/surveyor.snbt",
            "chapters/builder.snbt",
            "lang/en_us.snbt"
    );

    private static final List<String> MANIFESTS = List.of(
            "starter_signal_cache",
            "basic_ammunition",
            "medical_resupply",
            "advanced_medical",
            "shotgun_requisition",
            "brute_bounty",
            "warp_stone_requisition",
            "rail_setup",
            "attachments_random",
            "weapons_choice",
            "weapons_random",
            "reed_scaffolding",
            "reed_timber",
            "reed_stone_masonry",
            "reed_deepslate_masonry",
            "reed_sandstone",
            "reed_brickworks",
            "reed_blackstone",
            "reed_copper",
            "camp_storage_module",
            "camp_workshop_module",
            "camp_operations_module"
    );

    @Test
    void everyInstallerEntryIsBundled() {
        for (String file : QUEST_FILES) {
            assertResourceExists(
                    "/rotwire/ftbquests_defaults/" + file
            );
        }
    }

    @Test
    void everyDefaultCourierManifestHasALootTable() {
        for (String manifest : MANIFESTS) {
            assertResourceExists(
                    "/data/rotwire/loot_table/quest_delivery/"
                            + manifest + ".json"
            );
        }
    }

    @Test
    void starterSignalCacheIncludesTheStarterTarp() throws IOException {
        String lootTable = readResource(
                "/data/rotwire/loot_table/quest_delivery/starter_signal_cache.json"
        );
        assertTrue(lootTable.contains("\"name\": \"rotwire:tarp\""));
    }

    @Test
    void campModuleContractsAreBundledAsRepeatableCourierWork()
            throws IOException {
        assertModuleContract(
                "chapters/quartermaster.snbt",
                "3100000000000002",
                "rotwire_manifest_camp_storage_module"
        );
        assertModuleContract(
                "chapters/builder.snbt",
                "6100000000000009",
                "rotwire_manifest_camp_workshop_module"
        );
        assertModuleContract(
                "chapters/surveyor.snbt",
                "5100000000000004",
                "rotwire_manifest_camp_operations_module"
        );

        String language = readQuestResource("lang/en_us.snbt");
        assertTrue(language.contains(
                "quest.3100000000000002.title"
        ));
        assertTrue(language.contains(
                "quest.6100000000000009.title"
        ));
        assertTrue(language.contains(
                "quest.5100000000000004.title"
        ));
    }

    @Test
    void masonReedRequirementLabelsMatchTheConfiguredItems()
            throws IOException {
        String builder = readQuestResource("chapters/builder.snbt");
        String language = readQuestResource("lang/en_us.snbt");

        assertMasonReedRequirement(
                builder, language, "6110000000000002", "64L",
                "minecraft:iron_bars", "SUBMIT 64 IRON BARS"
        );
        assertMasonReedRequirement(
                builder, language, "6120000000000002", null,
                "rotwire:documents", "SUBMIT 1 DOCUMENT"
        );
        assertMasonReedRequirement(
                builder, language, "6120000000000003", "8L",
                "minecraft:iron_ingot", "SUBMIT 8 IRON INGOTS"
        );
        assertMasonReedRequirement(
                builder, language, "6120000000000004", null,
                "minecraft:iron_axe", "SUBMIT 1 IRON AXE"
        );
        assertMasonReedRequirement(
                builder, language, "6130000000000002", null,
                "rotwire:research_data", "SUBMIT 1 RESEARCH DATA"
        );
        assertMasonReedRequirement(
                builder, language, "6130000000000003", "32L",
                "minecraft:cobblestone", "SUBMIT 32 COBBLESTONE"
        );
        assertMasonReedRequirement(
                builder, language, "6130000000000004", null,
                "minecraft:iron_pickaxe", "SUBMIT 1 IRON PICKAXE"
        );
        assertMasonReedRequirement(
                builder, language, "6140000000000002", "2L",
                "rotwire:research_data", "SUBMIT 2 RESEARCH DATA"
        );
        assertMasonReedRequirement(
                builder, language, "6140000000000003", "3L",
                "toughasnails:purified_water_bottle",
                "SUBMIT 3 PURIFIED WATER BOTTLES"
        );
        assertMasonReedRequirement(
                builder, language, "6140000000000004", null,
                "minecraft:lava_bucket", "SUBMIT 1 LAVA BUCKET"
        );
        assertMasonReedRequirement(
                builder, language, "6150000000000002", null,
                "rotwire:documents", "SUBMIT 1 DOCUMENT"
        );
        assertMasonReedRequirement(
                builder, language, "6150000000000003", "12L",
                "minecraft:sand", "SUBMIT 12 SAND"
        );
        assertMasonReedRequirement(
                builder, language, "6150000000000004", null,
                "minecraft:water_bucket", "SUBMIT 1 WATER BUCKET"
        );
        assertMasonReedRequirement(
                builder, language, "6160000000000002", "2L",
                "rotwire:documents", "SUBMIT 2 DOCUMENTS"
        );
        assertMasonReedRequirement(
                builder, language, "6160000000000003", "3L",
                "toughasnails:purified_water_bottle",
                "SUBMIT 3 PURIFIED WATER BOTTLES"
        );
        assertMasonReedRequirement(
                builder, language, "6160000000000004", "7L",
                "minecraft:coal", "SUBMIT 7 COAL"
        );
        assertMasonReedRequirement(
                builder, language, "6170000000000002", "2L",
                "rotwire:research_data", "SUBMIT 2 RESEARCH DATA"
        );
        assertMasonReedRequirement(
                builder, language, "6170000000000003", "16L",
                "minecraft:bread", "SUBMIT 16 BREAD"
        );
        assertMasonReedRequirement(
                builder, language, "6170000000000004", null,
                "minecraft:lava_bucket", "SUBMIT 1 LAVA BUCKET"
        );
        assertMasonReedRequirement(
                builder, language, "6180000000000002", "2L",
                "rotwire:documents", "SUBMIT 2 DOCUMENTS"
        );
        assertMasonReedRequirement(
                builder, language, "6180000000000003", "8L",
                "minecraft:gold_ingot", "SUBMIT 8 GOLD INGOTS"
        );
        assertMasonReedRequirement(
                builder, language, "6180000000000004", null,
                "minecraft:shears", "SUBMIT 1 SHEARS"
        );
    }

    private static void assertMasonReedRequirement(
            String builder,
            String language,
            String taskId,
            String count,
            String itemId,
            String title
    ) {
        int taskIdIndex = builder.indexOf("id: \"" + taskId + "\"");
        assertTrue(taskIdIndex >= 0, () -> "Missing task " + taskId);

        int taskStart = builder.lastIndexOf("\n\t\t\t\t{", taskIdIndex);
        int taskEnd = builder.indexOf("\n\t\t\t\t{", taskIdIndex);
        if (taskEnd < 0) {
            taskEnd = builder.indexOf("\n\t\t\t]", taskIdIndex);
        }
        assertTrue(taskStart >= 0 && taskEnd >= 0,
                () -> "Could not isolate task " + taskId);
        String task = builder.substring(taskStart, taskEnd);

        if (count != null) {
            assertTrue(task.contains("count: " + count));
        }
        assertTrue(task.contains("item: { count: 1, id: \"" + itemId + "\" }"));
        assertTrue(language.contains(
                "task." + taskId + ".title: \"" + title + "\""
        ));
    }

    private static void assertResourceExists(String resource) {
        assertNotNull(
                QuestDefaultsResourceTest.class.getResource(resource),
                () -> "Missing bundled resource " + resource
        );
    }

    private static void assertModuleContract(
            String chapter,
            String questId,
            String manifest
    ) throws IOException {
        String contents = readQuestResource(chapter);
        assertTrue(contents.contains("id: \"" + questId + "\""));
        assertTrue(contents.contains(manifest));
        assertTrue(contents.contains("can_repeat: true"));
        assertTrue(contents.contains("repeat_cooldown: 6000"));
    }

    private static String readQuestResource(String resource)
            throws IOException {
        return readResource("/rotwire/ftbquests_defaults/" + resource);
    }

    private static String readResource(String resource) throws IOException {
        try (var input = QuestDefaultsResourceTest.class.getResourceAsStream(resource)) {
            assertNotNull(input);
            return new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }
    }
}
