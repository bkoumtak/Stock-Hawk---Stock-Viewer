package com.udacity.stockhawk.sync;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockQuote;
/*
    This class defines a method to fetch quotes and store them into a Content Provider
    database. Also defines a method to sync immediately and schedule a job that does the quote
    fetching. Kind of a makeshift sync adapter.
 */
public final class QuoteSyncJob {
    public static final String ACTION_DATA_UPDATED = "com.udacity.stockhawk.ACTION_DATA_UPDATED";
    public static final String ACTION_DATA_CLEAR = "com.udacity.stockhawk.ACTION_DATA_CLEAR";
    private static final int ONE_OFF_ID = 2;
    private static final int PERIOD = 300000;
    private static final int INITIAL_BACKOFF = 10000;
    private static final int PERIODIC_ID = 1;
    private static final int YEARS_OF_HISTORY = 2;

    private QuoteSyncJob() {
    }

    static void getQuotes(Context context) {

        Timber.d("Running sync job");
        /*
            Gets calendar instances to get the history of the stock.
         */
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();

        // From 2 years in the past
        from.add(Calendar.YEAR, -YEARS_OF_HISTORY);

        try {
            /*
                Gets the current list of stocks and transforms it into an array.
             */
            Set<String> stockPref = PrefUtils.getStocks(context);

            Set<String> stockCopy = new HashSet<>();
            stockCopy.addAll(stockPref);
            String[] stockArray = stockPref.toArray(new String[stockPref.size()]);

            Timber.d(stockCopy.toString());

            if (stockArray.length == 0) {
                return;
            }

            // Gets the Map of stocks and iterates through them.
            Map<String, Stock> quotes = YahooFinance.get(stockArray);
            Iterator<String> iterator = stockCopy.iterator();

            Timber.d(quotes.toString());

            ArrayList<ContentValues> quoteCVs = new ArrayList<>();

            // Iterate through the Map and get every corresponding quote using the Yahoo
            // Finance API.
            while (iterator.hasNext()) {
                String symbol = iterator.next();


                Stock stock = quotes.get(symbol);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = prefs.edit();
                StockQuote testQuote;

                if(stock == null){
                    editor.putBoolean(symbol, false);
                    editor.apply();
                    continue;
                } else {
                    testQuote = stock.getQuote();
                }

                // TODO: Edit Preferences To include valid/invalid stock list
                /*
                for (int i = 0; i < symbol.length(); i++){
                    char c = symbol.charAt(i);
                    int ascii_value = (int) c;
                    Timber.d(Integer.toString(ascii_value));
                    if ( ascii_value < 65 || ascii_value > 90){

                        invalid_character = true;
                        break;
                    }
                }*/

                if (testQuote.getPrice() == null) {
                    editor.putBoolean(symbol, false);
                    editor.apply();
                    continue;
                }

                editor.putBoolean(symbol, true);
                editor.apply();

                StockQuote quote = stock.getQuote();

                float price = quote.getPrice().floatValue();
                float change = quote.getChange().floatValue();
                float percentChange = quote.getChangeInPercent().floatValue();

                // WARNING! Don't request historical data for a stock that doesn't exist!
                // The request will hang forever X_x
                List<HistoricalQuote> history = stock.getHistory(from, to, Interval.WEEKLY);

                StringBuilder historyBuilder = new StringBuilder();

                // Builds the history of each stock
                for (HistoricalQuote it : history) {
                    historyBuilder.append(it.getDate().getTimeInMillis());
                    //historyBuilder.append(getDateString(it.getDate().getTimeInMillis()));
                    //historyBuilder.append(", ");
                    historyBuilder.append(",");
                    historyBuilder.append(it.getClose());
                    historyBuilder.append(",");
                    //historyBuilder.append("\n");
                }

                // Stores all of this into a content value ArrayList
                ContentValues quoteCV = new ContentValues();
                quoteCV.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                quoteCV.put(Contract.Quote.COLUMN_PRICE, price);
                quoteCV.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, percentChange);
                quoteCV.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, change);
                String history_string = historyBuilder.toString();
                history_string = history_string.substring(0, history_string.length()-1);
                quoteCV.put(Contract.Quote.COLUMN_HISTORY, history_string);

                //Timber.d(stock.getSymbol() + " " + historyBuilder.toString());
                quoteCVs.add(quoteCV);

            }

            // Insert this into a Content Provider
            context.getContentResolver()
                    .bulkInsert(
                            Contract.Quote.URI,
                            quoteCVs.toArray(new ContentValues[quoteCVs.size()]));

            // Broadcast that data has been inserted into the database
            Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED);
            Intent dataUpdatedClear = new Intent(ACTION_DATA_CLEAR);
            context.sendBroadcast(dataUpdatedIntent);
            LocalBroadcastManager.getInstance(context).sendBroadcast(dataUpdatedClear);

        } catch (IOException exception) {
            Timber.e(exception, "Error fetching stock quotes");
        }
    }

    /*
        This creates a job or task to do periodically during any network type? Probably should limit
        solely to wifi. Implements a JobService which in turn sets up IntentService to set an
        asynchronous thread to gather quotes through the Yahoo Finance API.
     */

    private static void schedulePeriodic(Context context) {
        Timber.d("Scheduling a periodic task");


        JobInfo.Builder builder = new JobInfo.Builder(PERIODIC_ID, new ComponentName(context, QuoteJobService.class));


        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD)
                .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);


        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        scheduler.schedule(builder.build());
    }

    /*
        Set up periodic job to get stocks and quotes and store them in a database.
        Also do some immediately (syncImmediately).
     */
    public static synchronized void initialize(final Context context) {

        schedulePeriodic(context);
        syncImmediately(context);

    }


    /*
        If the network is connected then immediately start the service to get the quotes.
        If network is not connected, schedule a job to execute when there is network
        connectivity. This executes only once hence "ONE_OFF_ID".
     */
    public static synchronized void syncImmediately(Context context) {

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            Intent nowIntent = new Intent(context, QuoteIntentService.class);
            context.startService(nowIntent);
        } else {

            JobInfo.Builder builder = new JobInfo.Builder(ONE_OFF_ID, new ComponentName(context, QuoteJobService.class));


            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);


            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            scheduler.schedule(builder.build());
        }
    }

    public static String getDateString(long dateInMillis){
        String pattern = "MM/dd/yyyy";
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);

        return dateFormat.format(dateInMillis);
    }


}
