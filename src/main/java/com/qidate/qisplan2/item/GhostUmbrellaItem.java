package com.qidate.qisplan2.item;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GhostUmbrellaItem extends Item {
    private static final long MIN_OPEN_TICKS = 10L * 20L;

    public GhostUmbrellaItem(
            Properties properties
    ) {
        super(properties);
    }

    public static boolean isOpen(
            ItemStack stack
    ) {
        Boolean value =
                stack.get(
                        QisPlan2.GHOST_UMBRELLA_OPEN
                );

        return Boolean.TRUE.equals(value);
    }

    public static void setOpen(
            ItemStack stack,
            boolean open
    ) {
        stack.set(
                QisPlan2.GHOST_UMBRELLA_OPEN,
                open
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack =
                player.getItemInHand(hand);

        boolean open =
                isOpen(stack);

        /*
         * ========================================================
         * 当前已经打开 → 尝试关闭
         * ========================================================
         */
        if (open) {

            Long openedAt =
                    stack.get(
                            QisPlan2.GHOST_UMBRELLA_OPENED_AT
                    );

            /*
             * 没有记录开始时间时，
             * 保险起见允许关闭。
             */
            if (openedAt != null) {

                long elapsed =
                        level.getGameTime()
                                - openedAt;

                /*
                 * ================================================
                 * 还没达到 10 秒
                 * ================================================
                 */
                if (elapsed < MIN_OPEN_TICKS) {

                    /*
                     * 只提示服务器上的玩家。
                     */
                    if (!level.isClientSide()) {

                        long remaining =
                                MIN_OPEN_TICKS
                                        - elapsed;

                        int seconds =
                                (int) Math.ceil(
                                        remaining / 20.0D
                                );

                        player.displayClientMessage(
                                Component.literal(
                                        "鬼雨伞至少需要撑开 "
                                                + seconds
                                                + " 秒"
                                ),
                                true
                        );
                    }

                    /*
                     * ★关键：
                     *
                     * 不执行 setOpen(false)
                     * 不删除 openedAt
                     *
                     * 所以客户端模型也不会关闭。
                     */
                    return InteractionResultHolder.fail(
                            stack
                    );
                }
            }

            /*
             * ====================================================
             * 已达到 10 秒 → 正常关伞
             * ====================================================
             */
            setOpen(
                    stack,
                    false
            );

            stack.remove(
                    QisPlan2.GHOST_UMBRELLA_OPENED_AT
            );

            return InteractionResultHolder.sidedSuccess(
                    stack,
                    level.isClientSide()
            );
        }


        /*
         * ========================================================
         * 当前关闭 → 开伞
         * ========================================================
         */

        setOpen(
                stack,
                true
        );

        /*
         * 记录本次开伞时间。
         *
         * 只由服务端记录。
         */
        if (!level.isClientSide()) {

            stack.set(
                    QisPlan2.GHOST_UMBRELLA_OPENED_AT,
                    level.getGameTime()
            );
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide()
        );
    }
}