package io.github.gev414.biohazard.quest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
            "reed_copper"
    );

    @Test
    void everyInstallerEntryIsBundled() {
        for (String file : QUEST_FILES) {
            assertResourceExists(
                    "/biohazard/ftbquests_defaults/" + file
            );
        }
    }

    @Test
    void everyDefaultCourierManifestHasALootTable() {
        for (String manifest : MANIFESTS) {
            assertResourceExists(
                    "/data/biohazard/loot_table/quest_delivery/"
                            + manifest + ".json"
            );
        }
    }

    private static void assertResourceExists(String resource) {
        assertNotNull(
                QuestDefaultsResourceTest.class.getResource(resource),
                () -> "Missing bundled resource " + resource
        );
    }
}
