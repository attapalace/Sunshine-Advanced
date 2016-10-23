package com.example.android.sunshine.app;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

/**
 * Created by ahmedatta on 3/5/16.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private ForecastAdapter mForecastAdapter;
    private RecyclerView mRecyclerView;
    private int mPosition = RecyclerView.NO_POSITION;
    private static final String SELECTED_KEY = "selected_position";
    private boolean mUseTodayLayout ;
    private boolean mAutoSelectView;
    private int mChoiceMode;

    private static final int FORECAST_LOADER = 0 ;
    public static final String LOG_TAG = ForecastFragment.class.getSimpleName();



    //private AlarmManager alarmMgr;
    //private PendingIntent alarmIntent;

    public static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;



    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(Uri dateUri);
    }

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.registerOnSharedPreferenceChangeListener(this);
        super.onResume();

    }

    @Override
    public void onPause() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_refresh) {
//            updateWeather();
//            return true;
//        }

        if(id == R.id.action_map){
            openPreferredLocationInMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
         super.onInflate(activity, attrs, savedInstanceState);
         TypedArray a = activity.obtainStyledAttributes(attrs, R.styleable.ForecastFragment,
                 0, 0);
         mChoiceMode = a.getInt(R.styleable.ForecastFragment_android_choiceMode, AbsListView.CHOICE_MODE_NONE);
         mAutoSelectView = a.getBoolean(R.styleable.ForecastFragment_autoSelectView, false);
         a.recycle();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        View emptyView = rootView.findViewById(R.id.recyclerview_forecast_empty);
        // The ForecastAdapter will take data from a source and
        // use it to populate the RecyclerView it's attached to.

//        mForecastAdapter = new ForecastAdapter(emptyView, getActivity());
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview_forecast);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mRecyclerView.setHasFixedSize(true);
        mForecastAdapter = new ForecastAdapter(getActivity(), new ForecastAdapter.ForecastAdapterOnClickHandler() {
            @Override
            public void onClick(Long date, ForecastAdapter.ForecastAdapterViewHolder vh) {
                String locationSetting = Utility.getPreferredLocation(getActivity());
                ((Callback) getActivity())
                        .onItemSelected(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                                locationSetting, date)
                        );
                mPosition = vh.getAdapterPosition();
            }
        }, emptyView, mChoiceMode);

        mRecyclerView.setAdapter(mForecastAdapter);

        final View parallaxView = rootView.findViewById(R.id.parallax_bar);
        if ( null != parallaxView){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
                mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
//                    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        int max = parallaxView.getHeight();
                        if ( dy > 0 ){
                            parallaxView.setTranslationY(Math.max(-max , parallaxView.getTranslationY() - dy /2));
                        }else {
                            parallaxView.setTranslationY(Math.min( 0 , parallaxView.getTranslationY() - dy/2));
                        }
                    }
                });
            }
        }

//        mRecyclerView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView adapterView, View view, int position, long id) {
//                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
//                if (cursor != null) {
//                    String locationSetting = Utility.getPreferredLocation(getActivity());
//                    ((Callback) getActivity())
//                            .onItemSelected(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
//                                    locationSetting, cursor.getLong(COL_WEATHER_DATE)));
//                }
//                mPosition = position;
//            }
//        });

        // to go to the previous position when activity is killed (oriented)
        if (savedInstanceState != null ){
             if(savedInstanceState.containsKey(SELECTED_KEY)){
                mPosition = savedInstanceState.getInt(SELECTED_KEY);
            }
            mForecastAdapter.onRestoreInstanceState(savedInstanceState);
        }

        mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if ( mRecyclerView != null){
            mRecyclerView.clearOnScrollListeners();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    void onLocationChanged(){
        // we cancelled the updateWeather function because we already updating weather at settingsActivity
//        updateWeather();
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
    }

    private void openPreferredLocationInMap(){
        if ( null != mForecastAdapter){
            Cursor c  = mForecastAdapter.getCursor();
            if ( null != c){
                c.moveToFirst();
                String posLat = c.getString(COL_COORD_LAT);
                String posLong = c.getString(COL_COORD_LONG);
                Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Log.d(LOG_TAG, "Could not call " + geoLocation.toString() + ", nothing!");
                }
            }
        }
    }


    public void setUseTodayLayout(boolean useTodayLayout){
        mUseTodayLayout = useTodayLayout;
        if (mForecastAdapter != null){
            mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }


//    public void updateWeather() {
//
//        String location = Utility.getPreferredLocation(getActivity());
//        FetchWeatherTask weatherTask = new FetchWeatherTask(getActivity());
//        weatherService.execute(location);
//
//        Intent intent = new Intent(getActivity(), SunshineService.class);
//
//        getActivity().startActivity(intent);
//
//        alarmMgr = (AlarmManager)getActivity().getSystemService(Context.ALARM_SERVICE);
//        Intent mIntent = new Intent(getActivity(), SunshineService.AlarmReceiver.class);
//        mIntent.putExtra(SunshineService.LOCATION_QUERY_EXTRA, location);
//        alarmIntent = PendingIntent.getBroadcast(getActivity(),0,mIntent,PendingIntent.FLAG_ONE_SHOT);
//
//        alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
//                + 5000, alarmIntent);
//
//        String location = Utility.getPreferredLocation(getActivity());
//        new FetchWeatherTask(getActivity()).execute(location);
//        SunshineSyncAdapter.syncImmediately(getActivity());
//
//    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        if (mPosition != RecyclerView.NO_POSITION){
            outState.putInt(SELECTED_KEY,mPosition);
        }
        mForecastAdapter.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        String locationSetting = Utility.getPreferredLocation(getActivity());

        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";

        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());

        return new CursorLoader(
                getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS
                ,null
                ,null
                ,sortOrder);
    }

    @Override
    public void onLoadFinished(Loader < Cursor > loader, Cursor data) {
        mForecastAdapter.swapCursor(data);

        if (mPosition != RecyclerView.NO_POSITION){
            mRecyclerView.smoothScrollToPosition(mPosition);
        }
        updateEmptyView();
        if ( data.getCount() > 0 ) {
            mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    // Since we know we're going to get items, we keep the listener around until
                    // we see Children.
                    if (mRecyclerView.getChildCount() > 0) {
                        mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                        int itemPosition = mForecastAdapter.getSelectedItemPosition();
                        if ( RecyclerView.NO_POSITION == itemPosition ) itemPosition = 0;
                        RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(itemPosition);
                        if ( null != vh && mAutoSelectView ) {
                            mForecastAdapter.selectView( vh );
                        }
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mForecastAdapter.swapCursor(null);
    }

    private void updateEmptyView(){
        if (mForecastAdapter.getItemCount() == 0){
            TextView tv = (TextView) getView().findViewById(R.id.recyclerview_forecast_empty);
            if ( null != tv){
                int message = R.string.empty_forcast_list;
                @SunshineSyncAdapter.LocationStatus int location = Utility.getLocationStatus(getActivity());
                switch (location){
                    case SunshineSyncAdapter.LOCATION_STATUS_SERVER_DOWN:
                        message = R.string.empty_forecast_list_server_down;
                        break;
                    case SunshineSyncAdapter.LOCATION_STATUS_SERVER_INVALID:
                        message = R.string.empty_forecast_list_server_error;
                        break;
                    case SunshineSyncAdapter.LOCATION_STATUS_INVALED:
                        message = R.string.empty_forecast_list_invalid_location;
                        break;
                    default:
                        if ( !Utility.IsNetworkAvailable(getActivity())){
                            message = R.string.empty_list_no_network;
                        }
                }
                tv.setText(message);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(getString(R.string.pref_location_status_key))){
            updateEmptyView();
        }
    }
}


