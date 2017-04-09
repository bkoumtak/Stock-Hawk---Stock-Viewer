package com.udacity.stockhawk.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.sync.QuoteSyncJob;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        StockAdapter.StockAdapterOnClickHandler {

    private static final int STOCK_LOADER = 0;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.recycler_view)
    RecyclerView stockRecyclerView;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.error)
    TextView error;
    private StockAdapter adapter;

    @Override
    // Adapter item just displays log on click, this is the onClick handler
    public void onClick(String symbol, String history) {

        Timber.d("Symbol clicked: %s", symbol);

        Toast.makeText(this, "This is supposed to be a chart for " + symbol, Toast.LENGTH_LONG)
                .show();
        Intent intent = new Intent(this , ChartActivity.class);
        intent.putExtra(ChartActivity.HISTORY_KEY, history);
        intent.putExtra(ChartActivity.SYMBOL_KEY, symbol);

        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Makes things simpler so you don't have to do TextView error = (TextView) findViewById(R.id.error)
        // every time.
        ButterKnife.bind(this);

        /*
            Creates the RecyclerView to populate, sets up adapter and stuff with a default
            horizontal Layout Manager. Also sets up a refresh listener to keep tabs on vertical
            swipes.
         */
        adapter = new StockAdapter(this, this);
        stockRecyclerView.setAdapter(adapter);
        stockRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setRefreshing(true);
        onRefresh();

        QuoteSyncJob.initialize(this);
        getSupportLoaderManager().initLoader(STOCK_LOADER, null, this);

        /*
            Enables swipe features on RecyclerView elements. Deletes the stock from the interface
            as well as the underlying database.

            So a simple callback was created. You are disabling dragging but limiting swiping on
            each element to the right.
         */
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                String symbol = adapter.getSymbolAtPosition(viewHolder.getAdapterPosition());
                PrefUtils.removeStock(MainActivity.this, symbol);
                getContentResolver().delete(Contract.Quote.makeUriForStock(symbol), null, null);
                Intent dataUpdatedIntent = new Intent(QuoteSyncJob.ACTION_DATA_UPDATED);
                sendBroadcast(dataUpdatedIntent);
                //QuoteSyncJob.syncImmediately(getApplicationContext());
            }
        }).attachToRecyclerView(stockRecyclerView);

        LocalBroadcastManager.getInstance(this).registerReceiver(mDeleteInvalidReceiver,
                new IntentFilter(QuoteSyncJob.ACTION_DATA_CLEAR));
    }

    private BroadcastReceiver mDeleteInvalidReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.d("WHy wasn't this calle");
           clearInvalid();
        }
    };
    /*
        Using the ConnectivityManager, you simply check to see if you are connected or not
        connected.
     */
    private boolean networkUp() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }


    /*
        This gets called every time you swipe vertically, refreshes and makes the error text view
        visible if there is any error connecting.
     */
    @Override
    public void onRefresh() {

        QuoteSyncJob.syncImmediately(this);

        List<String> stockList = new ArrayList<String>();

        //TODO: See if you can look up a certain stock, if it's invalid like remove it or something
        //clearInvalid();

        if (!networkUp() && adapter.getItemCount() == 0) {
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.error_no_network));
            error.setVisibility(View.VISIBLE);
        } else if (!networkUp()) {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, R.string.toast_no_connectivity, Toast.LENGTH_LONG).show();
        } else if (PrefUtils.getStocks(this).size() == 0) {
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.error_no_stocks));
            error.setVisibility(View.VISIBLE);
        } else {
            error.setVisibility(View.GONE);
        }
    }

    public void clearInvalid(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> stockList = PrefUtils.getStocks(this);
        String stockToRemove = null;

        for (String sym : stockList){
            if (prefs.getBoolean(sym, true) == false){
                Toast.makeText(this, "There is no stock for " + sym, Toast.LENGTH_LONG).show();
                stockToRemove = sym;

            }
        }
        editor.remove(stockToRemove);
        editor.apply();
        PrefUtils.removeStock(this, stockToRemove);
    }
    // This is called when you press the Floating Action Button, creates a new Dialogbox
    public void button(@SuppressWarnings("UnusedParameters") View view) {
        new AddStockDialog().show(getFragmentManager(), "StockDialogFragment");
    }

    /*
        Add stock and sync your QuoteSyncJob Immediately, displays relevant messages based on
        internet connectivity.
     */

    void addStock(String symbol) {
        if (symbol != null && !symbol.isEmpty()) {
            if (networkUp()) {
                swipeRefreshLayout.setRefreshing(true);
            } else {
                String message = getString(R.string.toast_stock_added_no_connectivity, symbol);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }

            PrefUtils.addStock(this, symbol);
            QuoteSyncJob.syncImmediately(this);
        }
    }

    /*
        Loader queries the database for you and loads it into the adapter when it
        detect changes. These are located in asynchronous threads located outside the
        Main UI.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                Contract.Quote.URI,
                Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                null, null, Contract.Quote.COLUMN_SYMBOL);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swipeRefreshLayout.setRefreshing(false);

        if (data.getCount() != 0) {
            error.setVisibility(View.GONE);
        }
        adapter.setCursor(data);
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swipeRefreshLayout.setRefreshing(false);
        adapter.setCursor(null);
    }


    private void setDisplayModeMenuItemIcon(MenuItem item) {
        if (PrefUtils.getDisplayMode(this)
                .equals(getString(R.string.pref_display_mode_absolute_key))) {
            item.setIcon(R.drawable.ic_percentage);
        } else {
            item.setIcon(R.drawable.ic_dollar);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_settings, menu);
        MenuItem item = menu.findItem(R.id.action_change_units);
        setDisplayModeMenuItemIcon(item);
        return true;
    }

    /*
        Switch between the two different display modes of the app on press. Changes the icons
        accordingly.
     */

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_change_units) {
            PrefUtils.toggleDisplayMode(this);
            setDisplayModeMenuItemIcon(item);
            adapter.notifyDataSetChanged();
            Intent dataUpdatedIntent = new Intent(QuoteSyncJob.ACTION_DATA_UPDATED);
            sendBroadcast(dataUpdatedIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
