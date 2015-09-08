package com.timmo.weather;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class OverviewFragment extends android.support.v4.app.Fragment implements View.OnClickListener {


    // region Global Vars
    public static final String ARG_OVERVIEW = "OVERVIEW";
    private SharedPreferences sharedPreferences;
    private Handler handler;
    private Typeface weatherFont;
    private String location, cityName;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private TextView textViewCurrCity, textViewUpdated, textViewCurrIcon, textViewCurrCondition,
            textViewCurrTemp, textViewCurrHumidity, textViewCurrPressure;
    private ImageButton imageButtonRefresh, imageButtonLocation;
    private ProgressBar progressBar;
    private RecyclerView recyclerViewForecast;
    private RecyclerView.Adapter recyclerViewAdapter;

    private ArrayList<String> arrayListHr, arrayListIcon, arrayListCondition, arrayListTemp;

    public OverviewFragment() {
        handler = new Handler();
    }
    //endregion

    // region onCreateView
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_overview, container, false);
        int i = getArguments().getInt(ARG_OVERVIEW);
        String notes = getResources().getStringArray(R.array.navigation_array)[i];
        getActivity().setTitle(notes);
        return rootView;
    }
    //endregion

    //region onViewCreated
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        textViewCurrCity = (TextView) getActivity().findViewById(R.id.textViewCurrCity);
        textViewUpdated = (TextView) getActivity().findViewById(R.id.textViewUpdated);
        textViewCurrIcon = (TextView) getActivity().findViewById(R.id.textViewCurrIcon);
        textViewCurrCondition = (TextView) getActivity().findViewById(R.id.textViewCurrCondition);
        textViewCurrTemp = (TextView) getActivity().findViewById(R.id.textViewCurrTemp);
        textViewCurrHumidity = (TextView) getActivity().findViewById(R.id.textViewCurrHumidity);
        textViewCurrPressure = (TextView) getActivity().findViewById(R.id.textViewCurrPressure);
        imageButtonRefresh = (ImageButton) getActivity().findViewById(R.id.imageButtonRefresh);
        imageButtonLocation = (ImageButton) getActivity().findViewById(R.id.imageButtonLocation);
        progressBar = (ProgressBar) getActivity().findViewById(R.id.progressBar);
        recyclerViewForecast = (RecyclerView) getActivity().findViewById(R.id.recyclerViewForecast);

        recyclerViewForecast.setHasFixedSize(true);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 1, 1, false);
        recyclerViewForecast.setLayoutManager(gridLayoutManager);

        arrayListHr = new ArrayList<>();
        arrayListIcon = new ArrayList<>();
        arrayListCondition = new ArrayList<>();
        arrayListTemp = new ArrayList<>();

        recyclerViewAdapter = new ForecastRecyclerViewAdapter
                (getActivity(), arrayListHr, arrayListIcon, arrayListCondition, arrayListTemp);
        recyclerViewForecast.setAdapter(recyclerViewAdapter);

        textViewCurrIcon.setTypeface(weatherFont);

        imageButtonRefresh.setOnClickListener(this);
        imageButtonLocation.setOnClickListener(this);

        locationManager = (LocationManager)
                getActivity().getSystemService(Context.LOCATION_SERVICE);
        locationListener = new MyLocationListener();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        weatherFont = Typeface.createFromAsset(getActivity().getAssets(), "weather.ttf");
        updateWeatherData(new CityPreference(getActivity()).getCity());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imageButtonRefresh:
                progressBar.setVisibility(View.VISIBLE);
                updateWeatherData(new CityPreference(getActivity()).getCity());
                break;
            case R.id.imageButtonLocation:
                progressBar.setVisibility(View.VISIBLE);
                //TODO API 23 check
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 5000, 100, locationListener);
                break;
        }
    }

    private void updateWeatherData(final String city) {
        new Thread() {
            public void run() {
                final JSONObject jsonCurrent = RemoteFetchCurrent.getJSON(getActivity(), city);
                final JSONObject jsonForecast = RemoteFetchForecast.getJSON(getActivity(), city);
                if (jsonCurrent == null) {
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(getActivity(),
                                    getActivity().getString(R.string.place_not_found),
                                    Toast.LENGTH_LONG).show();
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                }
                if (jsonForecast == null) {
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(getActivity(),
                                    getActivity().getString(R.string.place_not_found),
                                    Toast.LENGTH_LONG).show();
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                } else {
                    handler.post(new Runnable() {
                        public void run() {
                            renderWeather(jsonCurrent, jsonForecast);
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            }
        }.start();
    }

    private void renderWeather(JSONObject jsonCurrent, JSONObject jsonForecast) {
        try {
            textViewCurrCity.setText(jsonCurrent.getString("name") +
                    ", " +
                    jsonCurrent.getJSONObject("sys").getString("country"));

            JSONObject mainCurr = jsonCurrent.getJSONObject("main");
            textViewCurrTemp.setText(getResources().getString(R.string.name_temperature) + String.format("%.2f", mainCurr.getDouble("temp")) + "\u2103");
            textViewCurrHumidity.setText(getResources().getString(R.string.name_humidity) + mainCurr.getString("humidity") + "%");
            textViewCurrPressure.setText(getResources().getString(R.string.name_pressure) + mainCurr.getString("pressure") + " hPa");

            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
            String updatedOn = df.format(new Date(jsonCurrent.getLong("dt") * 1000));
            textViewUpdated.setText(getResources().getString(R.string.name_updated) + updatedOn);

            JSONObject detailsCurr = jsonCurrent.getJSONArray("weather").getJSONObject(0);
            textViewCurrCondition.setText(detailsCurr.getString("description").toUpperCase(Locale.getDefault()));

            textViewCurrIcon.setText(setWeatherIconCurr(detailsCurr.getInt("id"),
                    jsonCurrent.getJSONObject("sys").getLong("sunrise") * 1000,
                    jsonCurrent.getJSONObject("sys").getLong("sunset") * 1000));


            arrayListHr.clear();
            arrayListIcon.clear();
            arrayListCondition.clear();
            arrayListTemp.clear();


            DateFormat dfTime = DateFormat.getTimeInstance(DateFormat.SHORT);
            int max;
            switch (sharedPreferences.getInt("hour_forecast_hours", 1)) {
                case 0:
                    max = 2;
                    break;
                case 1:
                    max = 4;
                    break;
                case 2:
                    max = 6;
                    break;
                case 3:
                    max = 8;
                    break;
                default:
                    max = 4;
                    break;
            }
            for (int i = 0; i < max; i++) {
                JSONObject mainForecast = jsonForecast.getJSONArray("list").getJSONObject(i).getJSONObject("main");
                JSONObject detailsForecast = jsonForecast.getJSONArray("list").getJSONObject(i).getJSONArray("weather").getJSONObject(0);

                arrayListHr.add(dfTime.format(new Date(jsonForecast.getJSONArray("list").getJSONObject(i).getLong("dt") * 1000)));
                arrayListIcon.add(setWeatherIconForecast(detailsForecast.getInt("id")));
                arrayListCondition.add(detailsForecast.getString("description").toUpperCase(Locale.getDefault()));
                arrayListTemp.add(String.format("%.2f", mainForecast.getDouble("temp")) + "\u2103");
            }

        } catch (Exception e) {
            Log.e("TimmoWeather", "One or more fields not found in the JSON data");
        }
    }

    private String setWeatherIconCurr(int actualId, long sunrise, long sunset) {
        int id = actualId / 100;
        String icon = "";
        if (actualId == 800) {
            long currentTime = new Date().getTime();
            if (currentTime >= sunrise && currentTime < sunset) {
                icon = getActivity().getString(R.string.weather_sunny);
            } else {
                icon = getActivity().getString(R.string.weather_clear_night);
            }
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

    private String setWeatherIconForecast(int actualId) {
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


    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            //editLocation.setText("");
            //pb.setVisibility(View.INVISIBLE);
            //Toast.makeText(
            //        getActivity().getBaseContext(),
            //        "Location changed: Latitude: " + loc.getLatitude() + " Longitude: "
            //                + loc.getLongitude(), Toast.LENGTH_SHORT).show();
            //String longitude = "Longitude: " + loc.getLongitude();
            //Log.v(TAG, longitude);
            //String latitude = "Latitude: " + loc.getLatitude();
            //Log.v(TAG, latitude);

            cityName = null;
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
            location = loc.getLongitude() + ", " + loc.getLatitude();

            //Toast.makeText(getActivity(), cityName, Toast.LENGTH_SHORT).show();
            changeCity(cityName);
            new CityPreference(getActivity()).setCity(cityName);

            //location = longitude + "\n" + latitude + "\n\nMy Current City is: " + cityName;
            //editLocation.setText(s);
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