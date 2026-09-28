package com.qidate.qisplan2.ghost.tombstone.client;

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
     * 这里不把文字贴满模型，
     * 而是留出一圈安全边距。
     */

    /*
     * 文字区域最大宽度。
     */
    private static final float MAX_WIDTH = 10.0F;

    /*
     * 文字区域最大高度。
     */
    private static final float MAX_HEIGHT = 40.0F;

    /*
     * 单个中文字符的基础宽度。
     */
    private static final float CHAR_WIDTH = 8.0F;

    /*
     * 基础行高。
     */
    private static final float ROW_HEIGHT = 8.0F;

    /*
     * 最多允许两列。
     *
     * 12 px 宽的墓碑，
     * 两列已经基本达到视觉极限。
     */
    private static final int MAX_COLUMNS = 2;

    /*
     * 第一行的基础位置。
     *
     * 你之前已经调过这个位置，
     * 所以这里继续保留 -8。
     */
    private static final float START_Y = -8.0F;

    /*
     * 最小文字缩放比例。
     *
     * 太小以后就会失去碑文的可读性，
     * 所以不无限缩小。
     */
    private static final float MIN_SCALE = 0.55F;

    /**
     * 对墓碑文字进行排版。
     *
     * 规则：
     *
     * 1. 中文一个字符作为一个碑文单元。
     * 2. 英文 / 数字连续字符串作为一个单元。
     * 3. 超过一列高度后自动换列。
     * 4. 最多两列。
     * 5. 内容过长时整体缩小。
     * 6. 最终整体水平居中。
     */
    public static List<GhostTombstoneGlyph> layout(
            String text
    ) {

        List<GhostTombstoneGlyph> result =
                new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return result;
        }

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
         *
         * 基础情况下：
         *
         * 18 px 高 / 8 px 行高
         *
         * 大约可以容纳两行。
         *
         * 但是为了避免中文上下拥挤，
         * 我们实际采用两行作为一个基础列。
         */
        int rowsPerColumn =
                Math.max(
                        1,
                        (int) Math.floor(
                                MAX_HEIGHT / ROW_HEIGHT
                        )
                );

        /*
         * 两列最多容纳的单元数量。
         */
        int maxUnits =
                rowsPerColumn * MAX_COLUMNS;

        /*
         * 如果内容超过最大容量，
         * 就尝试缩小文字。
         */
        float scale = 1.0F;

        if (units.size() > maxUnits) {

            /*
             * 根据需要的列数计算缩放比例。
             */
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
                            MAX_HEIGHT / requiredHeight
                    );

            scale =
                    Math.max(
                            MIN_SCALE,
                            scale
                    );
        }

        /*
         * 缩小之后重新计算每列能够容纳的行数。
         */
        float scaledRowHeight =
                ROW_HEIGHT * scale;

        int scaledRowsPerColumn =
                Math.max(
                        1,
                        (int) Math.floor(
                                MAX_HEIGHT
                                        / scaledRowHeight
                        )
                );

        /*
         * 最终使用的列数。
         */
        int columnCount =
                (int) Math.ceil(
                        (double) units.size()
                                / scaledRowsPerColumn
                );

        /*
         * 不允许超过两列。
         */
        columnCount =
                Math.min(
                        columnCount,
                        MAX_COLUMNS
                );

        /*
         * ========================================================
         * 第三步：计算每列的实际宽度
         * ========================================================
         *
         * 中文一列大约 8 px。
         *
         * 两列之间留一点间距。
         */
        float scaledColumnWidth =
                CHAR_WIDTH * scale;

        float columnSpacing =
                2.0F * scale;

        float totalWidth =
                columnCount * scaledColumnWidth
                        + (columnCount - 1)
                        * columnSpacing;

        /*
         * 从中心开始排。
         */
        float firstColumnX =
                (totalWidth / 2.0F)
                        - scaledColumnWidth / 2.0F;

        /*
         * ========================================================
         * 第四步：生成 Glyph
         * ========================================================
         */
        int row = 0;
        int column = 0;

        for (String unit : units) {

            /*
             * 当前列的 X。
             *
             * 原来的排版是向左增加列，
             * 所以这里仍然保持：
             *
             * 第一列 → 右
             * 第二列 → 左
             */
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
             *
             * 正常情况下 scale 已经保证
             * 内容应该能够容纳。
             */
            if (column >= MAX_COLUMNS) {
                break;
            }
        }

        return result;
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
             * 中文 / 其他 Unicode 字符：
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
     * 判断一个碑文单元是不是英文 / 数字。
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