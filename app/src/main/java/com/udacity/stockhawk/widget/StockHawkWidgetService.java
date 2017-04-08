package com.udacity.stockhawk.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.icu.text.DecimalFormat;
import android.icu.text.NumberFormat;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.util.Locale;

import timber.log.Timber;

/**
 * Created by kondeelai on 2017-03-30.
 */
public class StockHawkWidgetService extends RemoteViewsService {
    private Context mContext;
    private Cursor mCursor;


    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StockRemoteViewsFactory(this.getApplicationContext(), intent);
    }

    class StockRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
        private final DecimalFormat dollarFormat;
        private final DecimalFormat dollarFormatWithPlus;
        private final DecimalFormat percentageFormat;

        public StockRemoteViewsFactory(Context context, Intent intent){
            dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
            dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
            dollarFormatWithPlus.setPositivePrefix("+$");
            percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
            percentageFormat.setMaximumFractionDigits(2);
            percentageFormat.setMinimumFractionDigits(2);
            percentageFormat.setPositivePrefix("+");
            mContext = context;
        }
        @Override
        public void onCreate() {

        }

        @Override
        public void onDataSetChanged() {
            if (mCursor != null) {
                mCursor.close();
            }

            mCursor = mContext.getContentResolver().query(
                    Contract.Quote.URI,
                    Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                    null,
                    null,
                    Contract.Quote.COLUMN_SYMBOL
            );

            mCursor.moveToFirst();



            Timber.d("This was called at least");
        }

        @Override
        public void onDestroy() {
            if (mCursor != null) {
                mCursor.close();
            }
        }

        @Override
        public int getCount() {
            return mCursor.getCount();
        }

        @Override
        public RemoteViews getViewAt(int position) {

            String symbol = "Symbol";
            double price = 0.0;

            RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_item_quote);

            if (mCursor.moveToPosition(position)){
                symbol = mCursor.getString(Contract.Quote.POSITION_SYMBOL);
                price = mCursor.getDouble(Contract.Quote.POSITION_PRICE);
            }

            rv.setTextViewText(R.id.widget_symbol, symbol);
            rv.setTextViewText(R.id.widget_price, dollarFormat.format(price));

            float rawAbsoluteChange = mCursor.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
            float percentageChange = mCursor.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

            if (rawAbsoluteChange > 0){
                rv.setInt(R.id.widget_change, "setBackgroundResource", R.drawable.percent_change_pill_green);
            } else {
                rv.setInt(R.id.widget_change, "setBackgroundResource", R.drawable.percent_change_pill_red);
            }

            String change = dollarFormatWithPlus.format(rawAbsoluteChange);
            String percentage = percentageFormat.format(percentageChange / 100);

            if (PrefUtils.getDisplayMode(mContext).equals("absolute")){
                rv.setTextViewText(R.id.widget_change, change);
            } else {
                rv.setTextViewText(R.id.widget_change, percentage);
            }


            return rv;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }
    }
}
