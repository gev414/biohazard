package io.github.gev414.rotwire.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KnockbackManagerTest {

    @Test
    void retainedStrengthScalesTheCurrentEventStrength() {
        assertEquals(
                0.15F,
                KnockbackManager.retainedStrength(1.0F, 0.15D),
                0.0001F
        );
        assertEquals(
                0.30F,
                KnockbackManager.retainedStrength(1.0F, 0.30D),
                0.0001F
        );
        assertEquals(
                0.30F,
                KnockbackManager.retainedStrength(2.0F, 0.15D),
                0.0001F
        );
    }
}
