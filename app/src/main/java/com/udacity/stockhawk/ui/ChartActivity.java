package com.udacity.stockhawk.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.udacity.stockhawk.R;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

/**
 * Created by kondeelai on 2017-04-04.
 */
public class ChartActivity extends AppCompatActivity implements OnChartValueSelectedListener{
    public static final String HISTORY_KEY = "SYMBOL_HISTORY";
    public static final String SYMBOL_KEY = "SYMBOL_TITLE";

    private static final int[] x_values = {1, 2, 3, 4,5 };
    private static final int[] y_values = {10, 2, 4, 5, 3};
    private long[] date_values;
    private double[] close_values;

    private String symbol_history = "empty";
    private String symbol = "symbol";

    private final DecimalFormat dollarFormat;

    public ChartActivity(){
        dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Intent intent = getIntent();

        Bundle extras = intent.getExtras();
        symbol_history = extras.getString(HISTORY_KEY);
        symbol = extras.getString(SYMBOL_KEY);

        //Timber.d(symbol_history);
        long[] dates = parseStockDate(symbol_history);
        float[] close_values = parseStockClose(symbol_history);


        for(int i = 0; i < dates.length; i++){
            Timber.d(dates[i] + " " + close_values[i] +  "\n");
        }

        setContentView(R.layout.activity_chart);

        TextView titleView = (TextView) findViewById(R.id.chart_title);
        titleView.setText(getString(R.string.chart_descripton)+ symbol);
        LineChart chart = (LineChart) findViewById(R.id.sample_chart);

        chart.setOnChartValueSelectedListener(this);

        List<Entry> entries = new ArrayList<Entry>();

        Timber.d(Integer.toString(dates.length));
        for(int i = dates.length-1; i >= 0; i--){
            entries.add(new Entry(dates[i], close_values[i]));
        }

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.dataset_string));
        dataSet.setHighLightColor(Color.RED);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        //xAxis.setLabelCount(10);
        chart.fitScreen();
        //yoxAxis.setAxisMinimum();
        xAxis.setLabelCount(4);
        xAxis.setAxisMinimum(dates[dates.length-1]);
        xAxis.setAxisMaximum(dates[0]);

        xAxis.setValueFormatter(new DateValueFormatter());


        chart.invalidate(); // refresh


    }

    private long[] parseStockDate(String history){
        String[] historyElements = history.split(",");
        long[] dates =  new long[historyElements.length/2];

        int index = 0;
        for (int i = 0; i < historyElements.length; i++){
            if ((i%2) == 0){
                dates[index] = Long.parseLong(historyElements[i]);
                index++;
            }
        }

        return dates;
    }

    private float[] parseStockClose(String history){
        String[] historyElements = history.split(",");
        float[] close = new float[historyElements.length/2];

        int index = 0;
        for (int i = 0; i < historyElements.length; i++){
            if ((i%2) == 1){
                close[index] = Float.parseFloat(historyElements[i]);
                index++;
            }
        }

        return close;
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        SimpleDateFormat monthDayFormat = new SimpleDateFormat(" MMMM dd, YYYY");
        String dateString = monthDayFormat.format(e.getX());
        double closeValue = e.getY();

        TextView dateTextView = (TextView)findViewById(R.id.txt_date);
        TextView closeTextView = (TextView)findViewById(R.id.txt_close);

        dateTextView.setText(getString(R.string.chart_date_string) + dateString);
        closeTextView.setText(getString(R.string.chart_close_string) + dollarFormat.format(closeValue));
    }

    @Override
    public void onNothingSelected() {

    }
}
