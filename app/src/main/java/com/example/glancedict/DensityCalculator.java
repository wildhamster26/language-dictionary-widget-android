package com.example.glancedict;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;

public final class DensityCalculator {
    private static final int FALLBACK_WIDTH_DP = 180;
    private static final int MIN_COLUMN_WIDTH_DP = 96;
    private static final int MAX_COLUMNS = 4;
    private static final int GRID_HORIZONTAL_PADDING_DP = 12; // 6dp padding * 2
    private static final int GRID_HORIZONTAL_SPACING_DP = 4; // horizontalSpacing in XML
    private static final int CELL_HORIZONTAL_PADDING_DP = 16; // 8dp padding * 2 in item layout
    private static final int TEXT_SAFETY_PADDING_DP = 8; // Reduced safety buffer

    private DensityCalculator() {
    }

    public static int calculateColumns(Context context, Bundle options, int fontSizeSp) {
        int widgetWidthDp = readWidgetWidthDp(options);
        int requiredTextWidthDp = calculateRequiredTextWidthDp(context, fontSizeSp);

        // Total usable width after grid-level padding (6dp on each side = 12dp)
        int usableWidthDp = widgetWidthDp - GRID_HORIZONTAL_PADDING_DP;

        // Try highest possible column count and step down
        for (int cols = MAX_COLUMNS; cols > 1; cols--) {
            // Space taken by horizontal gaps between columns
            int totalSpacingDp = GRID_HORIZONTAL_SPACING_DP * (cols - 1);
            int availableForColumnsDp = usableWidthDp - totalSpacingDp;

            if (availableForColumnsDp <= 0) {
                continue;
            }

            int columnWidthDp = availableForColumnsDp / cols;

            // Ensure column is wide enough for the text + cell padding + safety
            if (columnWidthDp >= requiredTextWidthDp && columnWidthDp >= MIN_COLUMN_WIDTH_DP) {
                return cols;
            }
        }

        return 1; // Fallback to single column
    }

    private static int calculateRequiredTextWidthDp(Context context, int fontSizeSp) {
        String longestText = DictionaryPrefs.getLongestTextCache(context);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // Bold text is wider; must match the layout's textStyle="bold"
        paint.setTypeface(Typeface.DEFAULT_BOLD);

        float textSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                fontSizeSp,
                context.getResources().getDisplayMetrics());
        paint.setTextSize(textSizePx);

        float measuredPx = paint.measureText(longestText == null ? "" : longestText);
        // Add 5% scale factor for rendering safety
        float measuredDp = (measuredPx * 1.05f) / context.getResources().getDisplayMetrics().density;

        // Text width + cell internal padding + safety buffer
        return (int) Math.ceil(measuredDp + CELL_HORIZONTAL_PADDING_DP + TEXT_SAFETY_PADDING_DP);
    }

    private static int readWidgetWidthDp(Bundle options) {
        if (options == null) {
            return FALLBACK_WIDTH_DP;
        }

        // Use MIN_WIDTH to be conservative and prevent wrapping on narrow launchers
        int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        return minWidth > 0 ? minWidth : FALLBACK_WIDTH_DP;
    }
}
