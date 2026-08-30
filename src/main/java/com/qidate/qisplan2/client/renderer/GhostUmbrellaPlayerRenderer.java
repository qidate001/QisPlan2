package com.qidate.qisplan2.client.renderer;

import com.qidate.qisplan2.item.GhostUmbrellaItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;

public final class GhostUmbrellaPlayerRenderer {

    private GhostUmbrellaPlayerRenderer() {
    }

    public static void afterSetupAnim(
            AbstractClientPlayer player,
            HumanoidModel<?> model
    ) {
        if (!(player.getMainHandItem().getItem()
                instanceof GhostUmbrellaItem)) {
            return;
        }

        /*
         * 固定右臂。
         */
        model.rightArm.xRot =
                (float) Math.toRadians(-65.0D);

        model.rightArm.yRot =
                (float) Math.toRadians(-8.0D);

        model.rightArm.zRot =
                (float) Math.toRadians(-8.0D);
    }
}