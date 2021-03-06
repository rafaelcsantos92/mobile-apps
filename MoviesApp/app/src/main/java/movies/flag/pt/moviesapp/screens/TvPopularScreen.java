package movies.flag.pt.moviesapp.screens;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.orm.SugarRecord;

import java.util.ArrayList;

import movies.flag.pt.moviesapp.R;
import movies.flag.pt.moviesapp.adapters.SeriesListAdapter;
import movies.flag.pt.moviesapp.http.entities.TvPopular;
import movies.flag.pt.moviesapp.http.entities.TvPopularResponse;
import movies.flag.pt.moviesapp.http.requests.GetNowPlayTvPopularsAsyncTask;
import movies.flag.pt.moviesapp.utils.DLog;
import movies.flag.pt.moviesapp.utils.DataUtils;

/**
 * Created by rafae_000 on 23/01/2018.
 */

public class TvPopularScreen extends Screen {

    private ArrayList<TvPopular> tvPopulars = new ArrayList<>();
    private SeriesListAdapter seriesListAdapter;
    private int page = 1;
    private ListView listView;
    private TextView screenTime;
    private String savedHours;
    private ImageButton backButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_screen);

        listView = findViewById(R.id.list_screen_text_view_list);
        screenTime = findViewById(R.id.list_screen_text_view_time);
        backButton = findViewById(R.id.list_screen_back_button);
        networkChangeReceiver = new NetworkChangeReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, filter);


        final SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.list_screen_swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);
                        executeRequest();
                    }
                }, 3000);
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        View view;
        LayoutInflater layoutInflater = LayoutInflater.from(TvPopularScreen.this);
        view = layoutInflater.inflate(R.layout.footer_view,null);
        listView.addFooterView(view);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                page = page + 1;
                executeRequest();
            }
        });

        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        Boolean isConnect = networkInfo != null && networkInfo.isConnectedOrConnecting();
        if (isConnect) {
//            Toast.makeText(this, getString(R.string.net_activity), Toast.LENGTH_SHORT).show();
            executeRequest();
        }else{
            tvPopulars = (ArrayList<TvPopular>) TvPopular.listAll(TvPopular.class);
            seriesListAdapter = new SeriesListAdapter(tvPopulars, TvPopularScreen.this);
            listView.setAdapter(seriesListAdapter);
            savedHours = PreferenceManager.getDefaultSharedPreferences(TvPopularScreen.this).getString("saved_time","ggg");
            screenTime.setText(savedHours);
        }

    }

    public class NetworkChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

//            Toast.makeText(context, context.getResources().getString(R.string.app_name),
//                    Toast.LENGTH_LONG).show();

            if (connectivity(context)){
                PreferenceManager.getDefaultSharedPreferences(context).getString("saved_time",savedHours);
                long tempo = PreferenceManager.getDefaultSharedPreferences(context).getLong("savedRefresh", 0);
                long horaActual = System.currentTimeMillis();
                long minute = (1000*60);
                long fiveMinutes = 5 * minute;
                long autoRefresh = horaActual - tempo;

                if (autoRefresh > fiveMinutes ) {
                    executeRequest();
                }
            }
        }

        boolean connectivity(Context context) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

            if(networkInfo != null && networkInfo.isConnectedOrConnecting() ) {
                return true;
            }else{
                Toast.makeText(context, context.getResources().getString(R.string.no_internet), Toast.LENGTH_LONG).show();
            }
            return false;

        }
    }
    private NetworkChangeReceiver networkChangeReceiver;

    private void executeRequest() {
        @SuppressLint("StaticFieldLeak") GetNowPlayTvPopularsAsyncTask asyncTask = new GetNowPlayTvPopularsAsyncTask(this, page) {

            @Override
            protected void onResponseSuccess(TvPopularResponse tvPopularResponse) {
                DLog.d(tag, "onResponseSuccess " + tvPopularResponse);
                long dateRefresh = System.currentTimeMillis();
                String fullData = DataUtils.getStringFromDate(dateRefresh);
                screenTime.setText(fullData);
                String savedHours = screenTime.getText().toString();
                if (page == 1) {
                    tvPopulars = tvPopularResponse.getTvPopulars();
                    seriesListAdapter = new SeriesListAdapter(tvPopulars, TvPopularScreen.this);
                    listView.setAdapter(seriesListAdapter);
                    SugarRecord.saveInTx(tvPopulars);
                    PreferenceManager.getDefaultSharedPreferences(context).edit().putString("saved_time",savedHours).apply();
                    PreferenceManager.getDefaultSharedPreferences(context).edit().putLong("savedRefresh", dateRefresh).apply();
                }else{
                    tvPopulars.addAll(tvPopularResponse.getTvPopulars());
                    seriesListAdapter.notifyDataSetChanged();
                }
            }


            @Override
            protected void onNetworkError() {
                DLog.d(tag, "onNetworkError ");
            }
        };
        asyncTask.execute();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(networkChangeReceiver);
    }
}
