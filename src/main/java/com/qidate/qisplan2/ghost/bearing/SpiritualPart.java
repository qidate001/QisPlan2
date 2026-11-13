package com.qidate.qisplan2.ghost.bearing;

import java.util.ArrayList;

import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * 可扩展的灵异承载部位标识，以资源位置按值比较，不依赖固定枚举序号。
 * 新实体可直接声明自己的命名空间与部位，无需修改此类。
 * GLOBAL 仅为侵蚀统计的全局来源标识，禁止加入实体组成；UNIVERSAL 是实际万能部位。
 */
public record SpiritualPart(ResourceLocation id) {
    public static final SpiritualPart GLOBAL = builtin("global");
    public static final SpiritualPart UNIVERSAL = builtin("universal");
    public static final SpiritualPart BRAIN = builtin("brain");
    public static final SpiritualPart HEART = builtin("heart");
    public static final SpiritualPart LUNG = builtin("lung");
    public static final SpiritualPart STOMACH = builtin("stomach");
    public static final SpiritualPart LIVER = builtin("liver");
    public static final SpiritualPart KIDNEY = builtin("kidney");
    public static final SpiritualPart PANCREAS = builtin("pancreas");
    public static final SpiritualPart GALLBLADDER = builtin("gallbladder");
    public static final SpiritualPart SPLEEN = builtin("spleen");
    public static final SpiritualPart INTESTINE = builtin("intestine");
    public static final SpiritualPart SKIN = builtin("skin");
    public static final SpiritualPart BLOOD = builtin("blood");
    public static final SpiritualPart BONE = builtin("bone");
    public static final SpiritualPart FLESH = builtin("flesh");
    public static final SpiritualPart EYE = builtin("eye");
    public static final SpiritualPart EAR = builtin("ear");
    public static final SpiritualPart NOSE = builtin("nose");
    public static final SpiritualPart MOUTH = builtin("mouth");
    public static final SpiritualPart HAND = builtin("hand");
    public static final SpiritualPart FOOT = builtin("foot");
    public static final SpiritualPart HAIR = builtin("hair");

    private static final List<SpiritualPart> HUMANOID = List.of(
            BRAIN, HEART, LUNG, STOMACH, LIVER, KIDNEY, PANCREAS, GALLBLADDER, SPLEEN, INTESTINE, SKIN, BLOOD, BONE, FLESH, EYE, EAR, NOSE, MOUTH, HAND, FOOT, HAIR);

    /** 标识必须存在，且由 ResourceLocation 保证格式合法。 */
    public SpiritualPart { Objects.requireNonNull(id, "id"); }

    /** 创建模组内置标识。 */
    private static SpiritualPart builtin(String path) {
        return new SpiritualPart(ResourceLocation.fromNamespaceAndPath("qisplan2", path));
    }

    /** 人形默认组成，共二十一个真实部位，不包含全局来源或万能部位。 */
    public static List<SpiritualPart> humanoidParts() { return HUMANOID; }

    /** 玩家侵蚀展示所需的统计项；不用于定义其他宿主的组成。 */
    public static List<SpiritualPart> playerCorrosionTargets() {
        var result = new ArrayList<SpiritualPart>();
        result.add(GLOBAL);
        result.addAll(HUMANOID);
        return List.copyOf(result);
    }

    /** 保持既有玩家界面的翻译键后缀。自定义部位应使用完整资源标识构建翻译键。 */
    public String name() { return id.getPath(); }
}
