package com.udacity.stockhawk.ui;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.SimpleDateFormat;



/**
 * Created by kondeelai on 2017-04-05.
 */
public class DateValueFormatter implements IAxisValueFormatter {
    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        SimpleDateFormat shortFormat = new SimpleDateFormat("MM/dd/yyyy");

        return shortFormat.format((long)value);
    }
}
