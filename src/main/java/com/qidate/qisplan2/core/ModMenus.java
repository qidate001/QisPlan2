package com.qidate.qisplan2.core;

import com.qidate.qisplan2.menu.GhostStoveMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;

import static com.qidate.qisplan2.core.ModRegistries.MENUS;

public class ModMenus {

    private ModMenus() {}

    public static void init() {
        /*
         * 故意留空。
         *
         * 调用这个方法本身，就会强制 JVM
         * 在正确的时机完成 ModMenus 的静态初始化。
         */
    }


    // 鬼灶台 GUI
    public static final DeferredHolder<
            MenuType<?>,
            MenuType<GhostStoveMenu>
            > GHOST_STOVE_MENU =
            MENUS.register(
                    "ghost_stove",
                    () -> new MenuType<>(
                            GhostStoveMenu::new,
                            FeatureFlags.DEFAULT_FLAGS
                    )
            );
}
