package com.code44.finance.ui.reports.trends;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;

import com.code44.finance.R;
import com.code44.finance.data.model.CurrencyFormat;
import com.code44.finance.money.MoneyFormatter;
import com.code44.finance.ui.common.presenters.Presenter;
import com.code44.finance.ui.reports.AmountGroups;
import com.code44.finance.utils.ThemeUtils;
import com.code44.finance.utils.interval.BaseInterval;

import org.joda.time.Interval;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lecho.lib.hellocharts.formatter.LineChartValueFormatter;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;

public abstract class TrendsChartPresenter extends Presenter {
    private final TrendsChartView trendsChartView;
    private final CurrencyFormat mainCurrencyFormat;
    private final Formatter formatter;

    public TrendsChartPresenter(TrendsChartView trendsChartView, CurrencyFormat mainCurrencyFormat) {
        this.trendsChartView = trendsChartView;
        this.mainCurrencyFormat = mainCurrencyFormat;
        this.formatter = new Formatter(mainCurrencyFormat);
    }

    public void setData(Cursor cursor, BaseInterval baseInterval) {
        final AmountGroups.AmountCalculator[] amountCalculators = getTransactionValidators();
        final AmountGroups amountGroups = new AmountGroups(baseInterval);
        final Map<AmountGroups.AmountCalculator, List<Long>> groups = amountGroups.getGroups(cursor, mainCurrencyFormat, amountCalculators);

        final List<Line> lines = new ArrayList<>();
        for (AmountGroups.AmountCalculator amountCalculator : amountCalculators) {
            final Line line = getLine(groups.get(amountCalculator))
                    .setColor(ThemeUtils.getColor(trendsChartView.getContext(), R.attr.textColorNegative))
                    .setHasLabels(true)
                    .setHasLabelsOnlyForSelected(true);
            onLineCreated(amountCalculator, line);
            lines.add(line);
        }

        final LineChartData lineChartData = new LineChartData(lines);
        lineChartData.setAxisXBottom(getAxis(baseInterval));

        trendsChartView.setLineGraphData(lineChartData);
    }

    protected abstract AmountGroups.AmountCalculator[] getTransactionValidators();

    protected abstract void onLineCreated(AmountGroups.AmountCalculator amountCalculator, Line line);

    protected Context getContext() {
        return trendsChartView.getContext();
    }

    private Line getLine(List<Long> amounts) {
        final List<PointValue> points = new ArrayList<>();
        int index = 0;
        for (Long amount : amounts) {
            final PointValue value = new PointValue(index++, amount);
            points.add(value);
        }

        final int lineWidthDp = (int) (trendsChartView.getResources().getDimension(R.dimen.report_line_graph_width) / Resources.getSystem().getDisplayMetrics().density);
        return new Line(points)
                .setCubic(true)
                .setStrokeWidth(lineWidthDp)
                .setPointRadius(lineWidthDp)
                .setFormatter(formatter)
                .setHasPoints(false);
    }

    private Axis getAxis(BaseInterval baseInterval) {
        final List<AxisValue> values = new ArrayList<>();
        final Period period = BaseInterval.getSubPeriod(baseInterval.getType(), baseInterval.getLength());

        Interval interval = new Interval(baseInterval.getInterval().getStart(), period);
        int index = 0;
        while (interval.overlaps(baseInterval.getInterval())) {
            values.add(new AxisValue(index++, BaseInterval.getSubTypeShortestTitle(interval, baseInterval.getType()).toCharArray()));
            interval = new Interval(interval.getEnd(), period);
        }

        return new Axis(values).setHasLines(true).setHasSeparationLine(true);
    }

    private static class Formatter implements LineChartValueFormatter {
        private final CurrencyFormat mainCurrencyFormat;

        public Formatter(CurrencyFormat mainCurrencyFormat) {
            this.mainCurrencyFormat = mainCurrencyFormat;
        }

        @Override public int formatChartValue(char[] chars, PointValue pointValue) {
            final char[] fullText = MoneyFormatter.format(mainCurrencyFormat, (long) pointValue.getY()).toCharArray();
            final int size = Math.min(chars.length, fullText.length);
            System.arraycopy(fullText, 0, chars, chars.length - size, size);
            return size;
        }
    }
}
