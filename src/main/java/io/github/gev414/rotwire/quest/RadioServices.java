package io.github.gev414.rotwire.quest;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbquests.net.OpenQuestBookMessage;
import io.github.gev414.rotwire.city.CityZoneManager;
import io.github.gev414.rotwire.menu.CampRadioMenu;
import io.github.gev414.rotwire.quest.delivery.DeliveryManager;
import io.github.gev414.rotwire.weather.WeatherManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public final class RadioServices {

    public static boolean openNetwork(
            ServerPlayer player,
            BlockPos radioPosition
    ) {
        if (!RadioNetwork.isConnected(
                player.level(),
                radioPosition
        )) {
            return false;
        }
        if (player.containerMenu instanceof CampRadioMenu) {
            player.closeContainer();
        }

        DeliveryManager.collectReady(player);
        if (DeliveryManager.openReadyChoice(player)) {
            return true;
        }
        DeliveryManager.sendStatus(player);
        RadioNetwork.cityZone(player.level(), radioPosition)
                .ifPresentOrElse(
                        zone -> CityZoneManager.sendStatus(player, zone),
                        () -> CityZoneManager.sendNoStatus(player)
                );
        WeatherManager.sendForecast(player);
        NetworkManager.sendToPlayer(
                player,
                new OpenQuestBookMessage(0L)
        );
        return true;
    }

    private RadioServices() {
    }
}
