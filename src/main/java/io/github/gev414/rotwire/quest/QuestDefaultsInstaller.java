package io.github.gev414.rotwire.quest;

import io.github.gev414.rotwire.Rotwire;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class QuestDefaultsInstaller {

    private static final String RESOURCE_ROOT =
            "/rotwire/ftbquests_defaults/";
    private static final List<String> DEFAULT_FILES = List.of(
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

    public static void installIfMissing() {
        Path questRoot = FMLPaths.CONFIGDIR.get()
                .resolve("ftbquests")
                .resolve("quests");
        try {
            if (Files.exists(questRoot) && directoryHasEntries(questRoot)) {
                Rotwire.LOGGER.info(
                        "Keeping existing FTB Quests book at {}",
                        questRoot
                );
                return;
            }

            for (String relativePath : DEFAULT_FILES) {
                copyDefault(questRoot, relativePath);
            }
            Rotwire.LOGGER.info(
                    "Installed Rotwire Survivor Network quest defaults at {}",
                    questRoot
            );
        } catch (IOException exception) {
            Rotwire.LOGGER.error(
                    "Could not install Rotwire FTB Quests defaults at {}",
                    questRoot,
                    exception
            );
        }
    }

    private static boolean directoryHasEntries(Path directory)
            throws IOException {
        try (var entries = Files.list(directory)) {
            return entries.findAny().isPresent();
        }
    }

    private static void copyDefault(Path questRoot, String relativePath)
            throws IOException {
        Path target = questRoot.resolve(relativePath);
        Files.createDirectories(target.getParent());
        try (InputStream input = QuestDefaultsInstaller.class
                .getResourceAsStream(RESOURCE_ROOT + relativePath)) {
            if (input == null) {
                throw new IOException(
                        "Missing bundled quest resource " + relativePath
                );
            }
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private QuestDefaultsInstaller() {
    }
}
