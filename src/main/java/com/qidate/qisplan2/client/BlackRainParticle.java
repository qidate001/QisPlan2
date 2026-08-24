package com.qidate.qisplan2.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class BlackRainParticle
        extends TextureSheetParticle {

    protected BlackRainParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            SpriteSet sprites
    ) {
        super(
                level,
                x,
                y,
                z
        );

        this.pickSprite(sprites);

        /*
         * 黑色。
         */
        this.setColor(
                0.0F,
                0.0F,
                0.0F
        );

        this.alpha = 0.85F;

        /*
         * 雨丝大小。
         */
        this.quadSize = 0.055F;

        /*
         * 初始向下速度。
         */
        this.yd = -0.75D;

        /*
         * 雨滴生命周期。
         */
        this.lifetime =
                10 + level.random.nextInt(8);
    }

    @Override
    public void tick() {

        xo = x;
        yo = y;
        zo = z;

        if (age++ >= lifetime) {
            remove();
            return;
        }

        /*
         * 向下加速。
         */
        yd -= 0.05D;

        move(
                xd,
                yd,
                zd
        );
    }

    /**
     * 使用粒子图集渲染。
     */
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider
            implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(
                SpriteSet sprites
        ) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd
        ) {
            BlackRainParticle particle =
                    new BlackRainParticle(
                            level,
                            x,
                            y,
                            z,
                            sprites
                    );

            particle.xd = xd;
            particle.yd = yd;
            particle.zd = zd;

            return particle;
        }
    }
}