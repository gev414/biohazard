package io.github.gev414.rotwire.client;

import io.github.gev414.rotwire.network.CourierChoiceOpenPayload;
import net.minecraft.client.Minecraft;

public final class CourierChoiceClient {

    public static void open(CourierChoiceOpenPayload payload) {
        Minecraft.getInstance().setScreen(new CourierChoiceScreen(payload));
    }

    private CourierChoiceClient() {
    }
}
