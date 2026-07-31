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
