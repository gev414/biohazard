package io.github.gev414.biohazard.client;

import io.github.gev414.biohazard.network.CourierChoiceOpenPayload;
import net.minecraft.client.Minecraft;

public final class CourierChoiceClient {

    public static void open(CourierChoiceOpenPayload payload) {
        Minecraft.getInstance().setScreen(new CourierChoiceScreen(payload));
    }

    private CourierChoiceClient() {
    }
}
