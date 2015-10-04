package com.timmo.weather;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DailyForecastFragment extends android.support.v4.app.Fragment implements View.OnClickListener {


    // region Global Vars
    public static final String ARG_FORECAST = "FORECAST";
    private final Handler handler;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private TextView textViewForecastCity;
    private ArrayList<String> arrayListDay, arrayListIcon, arrayListCondition, arrayListTempMin, arrayListTempMax, arrayListWind;
    private RecyclerView.Adapter recyclerViewAdapter;
    private SharedPreferences sharedPreferences;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerViewForecast;
    private GridLayoutManager gridLayoutManager;
    //endregion

    public DailyForecastFragment() {
        handler = new Handler();
    }

    // region onCreateView
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_forecast, container, false);
        int i = getArguments().getInt(ARG_FORECAST);
        String forecast = getResources().getStringArray(R.array.navigation_array)[i];
        getActivity().setTitle(forecast);
        return rootView;
    }
    //endregion

    //region onViewCreated
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        textViewForecastCity = (TextView) getActivity().findViewById(R.id.textViewForecastCity);
        ImageButton imageButtonRefresh = (ImageButton) getActivity().findViewById(R.id.imageButtonForecastRefresh);
        ImageButton imageButtonLocation = (ImageButton) getActivity().findViewById(R.id.imageButtonForecastLocation);
        recyclerViewForecast = (RecyclerView) getActivity().findViewById(R.id.recyclerViewForecast);
        swipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.swipe_container);

        updateWeatherData(new CityPreference(getActivity()).getCity());

        recyclerViewForecast.setHasFixedSize(true);
        recyclerViewForecast.setNestedScrollingEnabled(false);

        gridLayoutManager = new GridLayoutManager(getActivity(), getSpanCount(), 1, false);
        recyclerViewForecast.setLayoutManager(gridLayoutManager);

        recyclerViewForecast.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                boolean enable = false;
                if (recyclerViewForecast != null && recyclerViewForecast.getChildCount() > 0) {
                    // check if the first item of the list is visible
                    boolean firstItemVisible = gridLayoutManager.findFirstVisibleItemPosition() == 0;
                    // check if the top of the first item is visible
                    boolean topOfFirstItemVisible = recyclerViewForecast.getChildAt(0).getTop() == 0;
                    // enabling or disabling the refresh layout
                    enable = firstItemVisible && topOfFirstItemVisible;
                }
                swipeRefreshLayout.setEnabled(enable);
            }
        });

        arrayListDay = new ArrayList<>();
        arrayListIcon = new ArrayList<>();
        arrayListCondition = new ArrayList<>();
        arrayListTempMin = new ArrayList<>();
        arrayListTempMax = new ArrayList<>();
        arrayListWind = new ArrayList<>();

        recyclerViewAdapter = new DailyForecastRecyclerViewAdapter
                (getActivity(), arrayListDay, arrayListIcon, arrayListCondition, arrayListTempMin, arrayListTempMax, arrayListWind);
        recyclerViewForecast.setAdapter(recyclerViewAdapter);

        imageButtonRefresh.setOnClickListener(this);
        imageButtonLocation.setOnClickListener(this);

        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        locationListener = new MyLocationListener();

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateWeatherData(new CityPreference(getActivity()).getCity());
            }
        });

    }
    //endregion

/*
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateWeatherData(new CityPreference(getActivity()).getCity());
    }
*/

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imageButtonForecastRefresh:
                swipeRefreshLayout.setRefreshing(true);
                updateWeatherData(new CityPreference(getActivity()).getCity());
                break;
            case R.id.imageButtonForecastLocation:
                swipeRefreshLayout.setRefreshing(true);
                try {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER, 5000, 100, locationListener);
                } catch (SecurityException e) {
                    Log.e("TimmoWeather", "SecurityException: Permission requirements not met.");
                }
                break;
        }
    }

    private void updateWeatherData(final String city) {
        new Thread() {
            public void run() {
                final JSONObject jsonForecastDaily = RemoteFetchOWMDailyForecast.getJSON(getActivity(), city);
                if (jsonForecastDaily == null) {
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(getActivity(),
                                    getActivity().getString(R.string.place_not_found) + " " + city,
                                    Toast.LENGTH_LONG).show();
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                } else {
                    handler.post(new Runnable() {
                        public void run() {
                            renderWeather(jsonForecastDaily);
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            }
        }.start();
    }

    private void renderWeather(JSONObject jsonForecastDaily) {
        try {

            JSONObject location = jsonForecastDaily.getJSONObject("city");
            String city = location.getString("name") + ", " + location.getString("country");
            textViewForecastCity.setText(city);

            arrayListDay.clear();
            arrayListIcon.clear();
            arrayListCondition.clear();
            arrayListTempMin.clear();
            arrayListTempMax.clear();
            arrayListWind.clear();

            DateFormat dfForecastDay = new SimpleDateFormat("EEE", Locale.getDefault());
            DateFormat dfForecast = DateFormat.getDateInstance(DateFormat.MEDIUM);

            for (int i = 0; i < 16; i++) {
                JSONObject forecast = jsonForecastDaily.getJSONArray("list").getJSONObject(i);

                Date date = new Date(forecast.getLong("dt") * 1000);
                arrayListDay.add(dfForecastDay.format(date) + " " + dfForecast.format(date));
                arrayListIcon.add(setWeatherIcon(forecast.getJSONArray("weather").getJSONObject(0).getInt("id")));
                String conditionForecast = forecast.getJSONArray("weather").getJSONObject(0).getString("description").toUpperCase(Locale.getDefault());
                if (conditionForecast.equals("SKY IS CLEAR")) {
                    conditionForecast = "CLEAR SKIES";
                }
                arrayListCondition.add(conditionForecast);
                String tempMin, tempMax;
                switch (sharedPreferences.getString("temp_scale", "0")) {
                    case "0":
                        tempMin = getResources().getString(R.string.name_temperature_min) + " " + String.format("%.1f", forecast.getJSONObject("temp").getDouble("min")) + "\u2103";
                        tempMax = getResources().getString(R.string.name_temperature_max) + " " + String.format("%.1f", forecast.getJSONObject("temp").getDouble("max")) + "\u2103";
                        break;
                    case "1":
                        tempMin = getResources().getString(R.string.name_temperature_min) + " " + String.format("%.1f", convertToFahrenheit(forecast.getJSONObject("temp").getDouble("min"))) + "\u2109";
                        tempMax = getResources().getString(R.string.name_temperature_max) + " " + String.format("%.1f", convertToFahrenheit(forecast.getJSONObject("temp").getDouble("max"))) + "\u2109";
                        break;
                    default:
                        tempMin = getResources().getString(R.string.name_temperature_min) + " " + String.format("%.1f", forecast.getJSONObject("temp").getDouble("min")) + "\u2103";
                        tempMax = getResources().getString(R.string.name_temperature_max) + " " + String.format("%.1f", forecast.getJSONObject("temp").getDouble("max")) + "\u2103";
                        break;
                }
                arrayListTempMin.add(tempMin);
                arrayListTempMax.add(tempMax);
                String windSpeed;
                switch (sharedPreferences.getString("wind_speed", "0")) {
                    case "0":
                        windSpeed = getResources().getString(R.string.name_wind) + " " + forecast.getDouble("speed") + "mph";
                        break;
                    case "1":
                        windSpeed = getResources().getString(R.string.name_wind) + " " + convertToKPH(forecast.getDouble("speed")) + "kph";
                        break;
                    default:
                        windSpeed = getResources().getString(R.string.name_wind) + " " + forecast.getDouble("speed") + "mph";
                        break;
                }

                arrayListWind.add(windSpeed);
            }

            recyclerViewAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            Log.e("Timmo Weather", "One or more fields not found...");
        }

    }

    private Double convertToFahrenheit(Double celsius) {
        return (9.0 / 5.0) * celsius + 32;
    }

    private Double convertToKPH(Double miles) {
        return round(miles * 1.609344, 2);
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private String setWeatherIcon(int actualId) {
        int id = actualId / 100;
        String icon = "";
        if (actualId == 800) {
            icon = getActivity().getString(R.string.weather_sunny);
        } else {
            switch (id) {
                case 2:
                    icon = getActivity().getString(R.string.weather_thunder);
                    break;
                case 3:
                    icon = getActivity().getString(R.string.weather_drizzle);
                    break;
                case 7:
                    icon = getActivity().getString(R.string.weather_foggy);
                    break;
                case 8:
                    icon = getActivity().getString(R.string.weather_cloudy);
                    break;
                case 6:
                    icon = getActivity().getString(R.string.weather_snowy);
                    break;
                case 5:
                    icon = getActivity().getString(R.string.weather_rainy);
                    break;
            }
        }
        return icon;
    }

    public void changeCity(String city) {
        updateWeatherData(city);
    }

    private int getSpanCount() {
        //String toastMsg;
        int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (screenSize >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
            return 3;
        } else {
            return 2;
        }
    }

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            String cityName = null;
            Geocoder gcd = new Geocoder(getActivity().getBaseContext(), Locale.getDefault());
            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(loc.getLatitude(),
                        loc.getLongitude(), 1);
                if (addresses.size() > 0) {
                    System.out.println(addresses.get(0).getLocality());
                }
                cityName = addresses.get(0).getLocality();
            } catch (IOException e) {
                e.printStackTrace();
            }
            changeCity(cityName);
            new CityPreference(getActivity()).setCity(cityName);
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

}