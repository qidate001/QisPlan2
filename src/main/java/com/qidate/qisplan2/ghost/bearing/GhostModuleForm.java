package com.qidate.qisplan2.ghost.bearing;

import java.util.Objects;

/** 厉鬼声明自己的存在形式和所需部位，不识别宿主类型。 */
public record GhostModuleForm(Kind kind, SpiritualPart part) {
    public enum Kind { INDEPENDENT, REPLACEMENT }

    /** 独立式不得指定占位，替换式必须指定真实部位。 */
    public GhostModuleForm {
        Objects.requireNonNull(kind, "kind");
        if (kind == Kind.INDEPENDENT && part != null
                || kind == Kind.REPLACEMENT && (part == null || part.equals(SpiritualPart.GLOBAL)))
            throw new IllegalArgumentException("Invalid ghost module form");
    }

    /** 默认独立式，不占用任何承载部位。 */
    public static GhostModuleForm independent() { return new GhostModuleForm(Kind.INDEPENDENT, null); }

    /** 声明替换指定部位，具体宿主能否承载由统一安装层判断。 */
    public static GhostModuleForm replacing(SpiritualPart part) {
        return new GhostModuleForm(Kind.REPLACEMENT, part);
    }
}
