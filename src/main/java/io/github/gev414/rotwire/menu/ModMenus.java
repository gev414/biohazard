package io.github.gev414.rotwire.menu;

import io.github.gev414.rotwire.Rotwire;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, Rotwire.MOD_ID);

    public static final Supplier<MenuType<CampRadioMenu>> CAMP_RADIO =
            MENU_TYPES.register(
                    "camp_radio",
                    () -> IMenuTypeExtension.create(
                            CampRadioMenu::new
                    )
            );
    public static final Supplier<MenuType<CampStorageMenu>> CAMP_STORAGE =
            MENU_TYPES.register(
                    "camp_storage",
                    () -> IMenuTypeExtension.create(
                            CampStorageMenu::new
                    )
            );

    private ModMenus() {
    }
}
