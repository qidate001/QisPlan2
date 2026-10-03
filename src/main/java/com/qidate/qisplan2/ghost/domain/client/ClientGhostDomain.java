package com.qidate.qisplan2.ghost.domain.client;

import com.qidate.qisplan2.ghost.domain.CylinderDomainShape;
import com.qidate.qisplan2.ghost.domain.GhostDomainShape;
import com.qidate.qisplan2.ghost.domain.SphereDomainShape;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class ClientGhostDomain {

    private final UUID id;
    private final UUID sourceUUID;

    /**
     * GhostDomain Source 当前所在的灵异隔绝 Region。
     *
     * <p>
     * 这里保存的是服务端同步过来的
     * Region 真实 UUID。
     *
     * <p>
     * {@code null} 表示 Source 当前位于开放空间，
     * 或者客户端尚未收到该 Source 的 Region 信息。
     *
     * <p>
     * 注意：
     * 这里绝对不能保存 GPU Region Index。
     * GPU Index 是客户端运行期间动态分配的临时编号，
     * 可能随着 Region 重建而发生变化。
     */
    private UUID sourceRegionUUID;

    private final ResourceLocation domainType;
    private final ResourceLocation dimension;

    // 当前客户端实际使用的位置
    private double x;
    private double y;
    private double z;

    // 服务端同步过来的目标位置
    private double targetX;
    private double targetY;
    private double targetZ;

    private final GhostDomainShape shape;
    private double strength;
    private int layer;
    private double radius;

    /**
     * 鬼域视觉动画状态。
     */
    private AnimationState animationState =
            AnimationState.OPENING;

    /**
     * 鬼域视觉展开进度。
     *
     * 0.0 = 完全收起
     * 1.0 = 完全展开
     */
    private float animationProgress = 0.0F;

    /**
     * 展开/收回动画持续时间。
     */
    private static final int OPENING_TICKS = 12;
    private static final int CLOSING_TICKS = 10;

    /**
     * 当前动画已经进行的 tick。
     */
    private int animationTicks = 0;



    private enum AnimationState {
        OPENING,
        OPEN,
        CLOSING,
        CLOSED
    }

    public ClientGhostDomain(
            UUID id,
            UUID sourceUUID,
            ResourceLocation domainType,
            ResourceLocation dimension,
            double x,
            double y,
            double z,
            double strength,
            int layer,
            int shapeType,
            double radius
    ) {
        this.id = id;
        this.sourceUUID = sourceUUID;
        this.domainType = domainType;
        this.dimension = dimension;

        // 初始位置直接使用服务器位置
        this.x = x;
        this.y = y;
        this.z = z;

        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;

        this.strength = strength;
        this.layer = layer;

        if (shapeType == 1) {
            this.shape = new SphereDomainShape(radius);
        } else {
            this.shape = new CylinderDomainShape(radius);
        }

        this.radius = radius;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceUUID() {
        return sourceUUID;
    }

    /**
     * 获取 GhostDomain Source 当前所在的
     * 灵异隔绝 Region UUID。
     *
     * @return Region UUID；
     *         如果 Source 位于开放空间，则返回 null
     */
    public UUID getSourceRegionUUID() {
        return sourceRegionUUID;
    }

    public ResourceLocation getDomainType() {
        return domainType;
    }

    public ResourceLocation getDimension() {
        return dimension;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public GhostDomainShape getShape() {
        return shape;
    }

    public double getStrength() {
        return strength;
    }

    public int getLayer() {
        return layer;
    }

    public double getRadius() {
        return radius;
    }

    /**
     * 获取当前视觉动画使用的鬼域半径。
     *
     * <p>不会影响鬼域本身的实际逻辑半径。</p>
     */
    public double getRenderRadius() {
        return radius * animationProgress;
    }

    public boolean isAnimationFinished() {
        return animationState == AnimationState.CLOSED;
    }

    public void setStrength(double strength) {
        this.strength = strength;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    public void setRadius(double radius) {

        this.radius = radius;

        if (shape instanceof CylinderDomainShape cylinder) {
            cylinder.setRadius(radius);
        }
    }

    /**
     * 设置服务端同步过来的目标位置。
     *
     * 不直接修改当前渲染位置，
     * 当前渲染位置会在 tick() 中逐渐追上目标位置。
     */
    public void setPosition(
            double x,
            double y,
            double z
    ) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    /**
     * 立即设置鬼域位置。
     *
     * <p>用于玩家传送等需要瞬间同步的情况，
     * 不经过客户端平滑移动。</p>
     */
    public void setPositionImmediate(
            double x,
            double y,
            double z
    ) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    /**
     * 更新 GhostDomain Source 当前所在的
     * 灵异隔绝 Region。
     *
     * <p>
     * 该 UUID 由服务端同步，
     * 客户端只负责保存。
     *
     * @param sourceRegionUUID 新的 Region UUID；
     *                         {@code null} 表示开放空间
     */
    public void setSourceRegionUUID(
            UUID sourceRegionUUID
    ) {

        this.sourceRegionUUID =
                sourceRegionUUID;
    }

    /**
     * 客户端平滑更新鬼域位置。
     */
    public void tick() {

        moveAxis();
        tickAnimation();
    }

    private void moveAxis() {

        double speed = 0.9D; // 每tick最多移动0.9格

        x = moveTowards(x, targetX, speed);
        y = moveTowards(y, targetY, speed);
        z = moveTowards(z, targetZ, speed);
    }

    private void tickAnimation() {

        switch (animationState) {

            case OPENING -> {

                animationTicks++;

                animationProgress =
                        Math.min(
                                1.0F,
                                (float) animationTicks
                                        / OPENING_TICKS
                        );

                if (animationProgress >= 1.0F) {
                    animationProgress = 1.0F;
                    animationState = AnimationState.OPEN;
                }
            }

            case OPEN -> {
                animationProgress = 1.0F;
            }

            case CLOSING -> {

                animationTicks++;

                animationProgress =
                        Math.max(
                                0.0F,
                                1.0F
                                        - (float) animationTicks
                                        / CLOSING_TICKS
                        );

                if (animationProgress <= 0.0F) {
                    animationProgress = 0.0F;
                    animationState = AnimationState.CLOSED;
                }
            }

            case CLOSED -> {
                animationProgress = 0.0F;
            }
        }
    }

    public void startClosing() {

        if (
                animationState == AnimationState.CLOSING
                        || animationState == AnimationState.CLOSED
        ) {
            return;
        }

        animationState = AnimationState.CLOSING;

        animationTicks = 0;
    }

    private static double moveTowards(
            double current,
            double target,
            double maxStep
    ) {

        double delta = target - current;

        if (Math.abs(delta) <= maxStep) {
            return target;
        }

        return current + Math.copySign(maxStep, delta);
    }

    public boolean contains(
            double x,
            double y,
            double z
    ) {
        return shape.contains(
                this.x,
                this.y,
                this.z,
                x,
                y,
                z
        );
    }
}