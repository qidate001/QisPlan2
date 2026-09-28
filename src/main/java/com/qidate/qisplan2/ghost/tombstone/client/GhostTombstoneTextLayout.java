package com.qidate.qisplan2.ghost.tombstone.client;

import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.List;

public final class GhostTombstoneTextLayout {

    private GhostTombstoneTextLayout() {
    }

    /*
     * ============================================================
     * 墓碑文字安全区域
     * ============================================================
     *
     * 墓碑模型本身约为：
     *
     * 宽：12 px
     * 高：20 px
     *
     * 中文继续使用原来的竖排逻辑。
     */

    /*
     * 中文文字区域最大宽度。
     */
    private static final float MAX_WIDTH = 10.0F;

    /*
     * 中文文字区域最大高度。
     */
    private static final float MAX_HEIGHT = 40.0F;

    /*
     * 中文单个字符的基础宽度。
     */
    private static final float CHAR_WIDTH = 8.0F;

    /*
     * 基础行高。
     */
    private static final float ROW_HEIGHT = 8.0F;

    /*
     * 最多允许两列。
     */
    private static final int MAX_COLUMNS = 2;

    /*
     * 第一行基础位置。
     *
     * 保留你目前已经调好的位置。
     */
    private static final float START_Y = -8.0F;

    /*
     * 最小文字缩放比例。
     */
    private static final float MIN_SCALE = 0.55F;

    /*
     * ============================================================
     * 英文排版参数
     * ============================================================
     *
     * 英文和中文完全不同：
     *
     * 中文：
     *     一个字一个 Glyph
     *     纵向排列
     *
     * 英文：
     *     整句话作为一个 Glyph
     *     横向排列
     *
     * 这里的 18 px 是 Minecraft Font 坐标中的
     * 目标最大宽度。
     *
     * 大约相当于：
     *
     *     两个中文字符 + 中间间距
     *
     * 因此视觉上不会比中文区域宽太多。
     */
    private static final float ENGLISH_MAX_WIDTH = 18.0F;

    /*
     * 英文最小缩放比例。
     *
     * 英文是横排文字，
     * 为了保证较长的名字也能完整放进墓碑，
     * 允许比中文缩得更小。
     */
    private static final float ENGLISH_MIN_SCALE = 0.25F;

    /*
     * 英文默认垂直位置。
     *
     * 先和当前中文第一行保持一致。
     * 后续如果觉得英文需要上下微调，
     * 只需要调整这里。
     */
    private static final float ENGLISH_Y = START_Y;

    /**
     * 对墓碑文字进行排版。
     *
     * 英文需要 Font，
     * 因为 Minecraft 的字体并不是每个字符固定 8 px。
     */
    public static List<GhostTombstoneGlyph> layout(
            String text,
            Font font
    ) {

        List<GhostTombstoneGlyph> result =
                new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return result;
        }

        /*
         * ========================================================
         * 纯英文 / 数字 / ASCII
         * ========================================================
         *
         * 如果整个碑文都是 ASCII，
         * 就直接走英文横排。
         */
        if (isAllAscii(text)) {

            return layoutEnglish(
                    text.trim(),
                    font
            );
        }

        /*
         * ========================================================
         * 中文 / 混合文本
         * ========================================================
         *
         * 中文继续使用原来的竖排逻辑。
         */
        return layoutChinese(text);
    }

    /**
     * ============================================================
     * 英文横排
     * ============================================================
     *
     * 整段英文作为一个 Glyph。
     *
     * 例如：
     *
     *     REST IN PEACE
     *
     * 会作为一个整体横向显示。
     */
    private static List<GhostTombstoneGlyph> layoutEnglish(
            String text,
            Font font
    ) {

        List<GhostTombstoneGlyph> result =
                new ArrayList<>();

        if (text.isEmpty()) {
            return result;
        }

        /*
         * Minecraft 实际字体宽度。
         *
         * 不能简单使用：
         *
         *     text.length() * 8
         *
         * 因为：
         *
         *     I
         *     W
         *     M
         *     space
         *
         * 的宽度都不一样。
         */
        int fontWidth =
                font.width(text);

        if (fontWidth <= 0) {
            return result;
        }

        /*
         * 根据实际字体宽度计算缩放。
         *
         * 例如：
         *
         * 字体宽度 = 30
         * 最大宽度 = 18
         *
         * 那么：
         *
         * scale = 18 / 30 = 0.6
         */
        float scale =
                Math.min(
                        1.0F,
                        ENGLISH_MAX_WIDTH
                                / fontWidth
                );

        /*
         * 不允许无限缩小。
         */
        scale =
                Math.max(
                        ENGLISH_MIN_SCALE,
                        scale
                );

        /*
         * 英文：
         *
         * x = 0
         *
         * 因为 Renderer 会使用：
         *
         * -width / 2
         *
         * 来让文字自身水平居中。
         */
        result.add(
                new GhostTombstoneGlyph(
                        text,
                        0.0F,
                        ENGLISH_Y,
                        0.0F,
                        scale
                )
        );

        return result;
    }

    /**
     * ============================================================
     * 中文竖排
     * ============================================================
     *
     * 这里基本保持你之前已经调好的逻辑。
     */
    private static List<GhostTombstoneGlyph> layoutChinese(
            String text
    ) {

        List<GhostTombstoneGlyph> result =
                new ArrayList<>();

        /*
         * ========================================================
         * 第一步：把文本拆成碑文单元
         * ========================================================
         */
        List<String> units =
                splitUnits(text);

        if (units.isEmpty()) {
            return result;
        }

        /*
         * ========================================================
         * 第二步：确定基础布局
         * ========================================================
         */
        int rowsPerColumn =
                Math.max(
                        1,
                        (int) Math.floor(
                                MAX_HEIGHT
                                        / ROW_HEIGHT
                        )
                );

        /*
         * 两列最多容纳的单元数量。
         */
        int maxUnits =
                rowsPerColumn * MAX_COLUMNS;

        /*
         * 默认不缩放。
         */
        float scale = 1.0F;

        /*
         * 如果内容太多，
         * 根据需要的高度进行缩放。
         */
        if (units.size() > maxUnits) {

            int requiredRows =
                    (int) Math.ceil(
                            (double) units.size()
                                    / MAX_COLUMNS
                    );

            float requiredHeight =
                    requiredRows * ROW_HEIGHT;

            scale =
                    Math.min(
                            1.0F,
                            MAX_HEIGHT
                                    / requiredHeight
                    );

            scale =
                    Math.max(
                            MIN_SCALE,
                            scale
                    );
        }

        /*
         * 缩放之后的行高。
         */
        float scaledRowHeight =
                ROW_HEIGHT * scale;

        /*
         * 缩放之后每列可以放多少行。
         */
        int scaledRowsPerColumn =
                Math.max(
                        1,
                        (int) Math.floor(
                                MAX_HEIGHT
                                        / scaledRowHeight
                        )
                );

        /*
         * 最终需要多少列。
         */
        int columnCount =
                (int) Math.ceil(
                        (double) units.size()
                                / scaledRowsPerColumn
                );

        /*
         * 最多两列。
         */
        columnCount =
                Math.min(
                        columnCount,
                        MAX_COLUMNS
                );

        /*
         * ========================================================
         * 第三步：计算每列宽度
         * ========================================================
         */
        float scaledColumnWidth =
                CHAR_WIDTH * scale;

        float columnSpacing =
                2.0F * scale;

        float totalWidth =
                columnCount
                        * scaledColumnWidth
                        + (columnCount - 1)
                        * columnSpacing;

        /*
         * 从中心开始排。
         */
        float firstColumnX =
                totalWidth / 2.0F
                        - scaledColumnWidth / 2.0F;

        /*
         * ========================================================
         * 第四步：生成 Glyph
         * ========================================================
         */
        int row = 0;
        int column = 0;

        for (String unit : units) {

            float x =
                    firstColumnX
                            - column
                            * (
                            scaledColumnWidth
                                    + columnSpacing
                    );

            float y =
                    START_Y
                            + row * scaledRowHeight;

            /*
             * ASCII 单元：
             *
             *     英文
             *     数字
             *
             * 目前在混合文本中仍然沿用原来的竖排方式。
             *
             * 纯英文则不会进入这里，
             * 而是走 layoutEnglish()。
             */
            float rotation =
                    isAsciiUnit(unit)
                            ? 90.0F
                            : 0.0F;

            result.add(
                    new GhostTombstoneGlyph(
                            unit,
                            x,
                            y,
                            rotation,
                            scale
                    )
            );

            row++;

            if (row >= scaledRowsPerColumn) {

                row = 0;
                column++;
            }

            /*
             * 达到两列以后停止。
             */
            if (column >= MAX_COLUMNS) {
                break;
            }
        }

        return result;
    }

    /**
     * ============================================================
     * 判断整段文本是不是 ASCII
     * ============================================================
     *
     * 注意：
     *
     * 空格也允许。
     *
     * 所以：
     *
     *     REST IN PEACE
     *
     * 会被识别成纯英文。
     */
    private static boolean isAllAscii(
            String text
    ) {

        if (text.isEmpty()) {
            return false;
        }

        for (int i = 0; i < text.length(); i++) {

            if (text.charAt(i) > 127) {
                return false;
            }
        }

        return true;
    }

    /**
     * 把字符串拆成碑文单元。
     *
     * 中文：
     *
     *     鬼
     *     墓
     *     碑
     *
     * 英文：
     *
     *     RIP
     *
     * 作为一个完整单元。
     */
    private static List<String> splitUnits(
            String text
    ) {

        List<String> units =
                new ArrayList<>();

        int i = 0;

        while (i < text.length()) {

            char c =
                    text.charAt(i);

            /*
             * 空格直接跳过。
             */
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            /*
             * 英文 / 数字连续读取。
             */
            if (isAscii(c)) {

                int start = i;

                while (
                        i < text.length()
                                && isAscii(
                                text.charAt(i)
                        )
                ) {
                    i++;
                }

                units.add(
                        text.substring(
                                start,
                                i
                        )
                );

                continue;
            }

            /*
             * 中文 / 其他 Unicode：
             * 一个字符一个碑文单元。
             */
            int codePoint =
                    text.codePointAt(i);

            units.add(
                    new String(
                            Character.toChars(
                                    codePoint
                            )
                    )
            );

            i +=
                    Character.charCount(
                            codePoint
                    );
        }

        return units;
    }

    /**
     * 判断一个碑文单元是不是 ASCII。
     */
    private static boolean isAsciiUnit(
            String unit
    ) {

        if (unit.isEmpty()) {
            return false;
        }

        return isAscii(
                unit.charAt(0)
        );
    }

    /**
     * 判断字符是不是 ASCII 非空白字符。
     */
    private static boolean isAscii(
            char c
    ) {

        return c <= 127
                && !Character.isWhitespace(c);
    }
}