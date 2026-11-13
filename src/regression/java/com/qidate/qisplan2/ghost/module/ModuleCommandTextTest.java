package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.command.GhostCommands;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;

/** 使用真实原版翻译组件重现命令 ID、UUID 参数引起的构造异常。 */
public final class ModuleCommandTextTest {
    /** 测试类无需实例化。 */
    private ModuleCommandTextTest() {}

    /** 验证对象参数转换且嵌套译名仍保留为组件，避免服务端提前翻译。 */
    public static void run() {
        try {
            var method = GhostCommands.class.getDeclaredMethod("moduleText", String.class, Object[].class);
            method.setAccessible(true);
            var id = ResourceLocation.fromNamespaceAndPath("qisplan2", "calling_ghost");
            var uuid = UUID.randomUUID();
            var name = Component.translatable("module.qisplan2.calling_ghost");
            Object[] input = {name, id, uuid, 30, 0.5D, true, "text"};
            var result = (Component) method.invoke(null, "name", input);
            var contents = (TranslatableContents) result.getContents();
            var args = contents.getArgs();
            if (args[0] != name || !args[1].equals(id.toString()) || !args[2].equals(uuid.toString())
                    || !args[3].equals(30) || !args[4].equals(0.5D) || !args[5].equals(true)
                    || !args[6].equals("text") || input[1] != id
                    || !contents.getKey().equals("command.qisplan2.module.name"))
                throw new AssertionError("Module command translation arguments changed unexpectedly");
            System.out.println("Module command translation regression checks passed");
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Module command text construction failed", failure);
        }
    }
}
