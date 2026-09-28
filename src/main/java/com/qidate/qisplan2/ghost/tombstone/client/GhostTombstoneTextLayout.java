package com.qidate.qisplan2.ghost.tombstone.client;

import java.util.ArrayList;
import java.util.List;

public final class GhostTombstoneTextLayout {

    private GhostTombstoneTextLayout() {}

    /*
     * 每列最多 7 行。
     */
    private static final int ROWS = 7;

    /*
     * 行高。
     */
    private static final float ROW_HEIGHT = 8.0F;

    /*
     * 第一行起始位置。
     *
     * 负值表示整体往上移动。
     */
    private static final float START_Y = -8.0F;

    /*
     * 列宽。
     */
    private static final float COLUMN_WIDTH = 8.0F;

    public static List<GhostTombstoneGlyph> layout(
            String text
    ) {

        List<GhostTombstoneGlyph> result =
                new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return result;
        }

        int row = 0;
        int column = 0;

        int i = 0;

        while (i < text.length()) {

            char c = text.charAt(i);

            /*
             * 英文 / 数字连续合并。
             */
            if (isAscii(c)) {

                int start = i;

                while (
                        i < text.length()
                                && isAscii(text.charAt(i))
                ) {
                    i++;
                }

                String word =
                        text.substring(start, i);

                result.add(
                        new GhostTombstoneGlyph(
                                word,
                                -column * COLUMN_WIDTH,
                                START_Y + row * ROW_HEIGHT,
                                90.0F
                        )
                );

                row++;

            } else {

                result.add(
                        new GhostTombstoneGlyph(
                                String.valueOf(c),
                                -column * COLUMN_WIDTH,
                                START_Y + row * ROW_HEIGHT,
                                0.0F
                        )
                );

                row++;
                i++;
            }

            if (row >= ROWS) {

                row = 0;
                column++;
            }
        }

        return result;
    }

    private static boolean isAscii(char c) {

        return c <= 127
                && !Character.isWhitespace(c);
    }
}