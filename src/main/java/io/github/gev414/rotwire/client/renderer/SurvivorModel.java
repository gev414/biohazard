package io.github.gev414.rotwire.client.renderer;

import io.github.gev414.rotwire.entity.SurvivorEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * Player-shaped survivor model with a relaxed two-hand rifle stance. The
 * weapon stays below an aim-down-sights pose while following the survivor's
 * head and body turn toward its current threat.
 */
public final class SurvivorModel extends PlayerModel<SurvivorEntity> {

    public SurvivorModel(ModelPart root) {
        super(root, false);
    }

    @Override
    public void setupAnim(
            SurvivorEntity survivor,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        super.setupAnim(
                survivor,
                limbSwing,
                limbSwingAmount,
                ageInTicks,
                netHeadYaw,
                headPitch
        );
        if (!survivor.hasFirearm()) {
            return;
        }

        float headYawInfluence = head.yRot * 0.65F;
        float headPitchInfluence = head.xRot * 0.25F;
        rightArm.xRot = -1.15F + headPitchInfluence;
        rightArm.yRot = -0.12F + headYawInfluence;
        rightArm.zRot = 0.04F;
        leftArm.xRot = -1.05F + headPitchInfluence;
        leftArm.yRot = 0.42F + headYawInfluence;
        leftArm.zRot = -0.08F;

        // PlayerModel animates its sleeve cubes separately from the base arms.
        // Keep both layers together after replacing the walking arm pose.
        rightSleeve.copyFrom(rightArm);
        leftSleeve.copyFrom(leftArm);
    }
}
