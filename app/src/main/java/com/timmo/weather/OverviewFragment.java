package com.timmo.weather;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
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

public class OverviewFragment extends android.support.v4.app.Fragment implements View.OnClickListener {


    // region Global Vars
    private final Handler handler;
    private Typeface weatherFont;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private TextView textViewCurrCity, textViewUpdated, textViewCurrIcon, textViewCurrCondition,
            textViewCurrTemp, textViewCurrHumidity, textViewCurrWind;
    private ArrayList<String> arrayListHr, arrayListIcon, arrayListCondition, arrayListTemp, arrayListWind;
    private RecyclerView.Adapter recyclerViewAdapter;
    private SharedPreferences sharedPreferences;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerViewForecast;
    private GridLayoutManager gridLayoutManager;
    private boolean goodCity;
    private Dialog dialogFirstRun;
    //endregion

    public OverviewFragment() {
        handler = new Handler();
    }

    private static double round(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    // region onCreateView
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        goodCity = false;
        return inflater.inflate(R.layout.fragment_overview, container, false);
    }

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
        textViewCurrWind = (TextView) getActivity().findViewById(R.id.textViewCurrWind);
        ImageButton imageButtonChangeCity = (ImageButton) getActivity().findViewById(R.id.imageButtonChangeCity);
        ImageButton imageButtonRefresh = (ImageButton) getActivity().findViewById(R.id.imageButtonRefresh);
        ImageButton imageButtonLocation = (ImageButton) getActivity().findViewById(R.id.imageButtonLocation);
        recyclerViewForecast = (RecyclerView) getActivity().findViewById(R.id.recyclerView);
        swipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.swipe_container);

        recyclerViewForecast.setHasFixedSize(true);
        recyclerViewForecast.setNestedScrollingEnabled(false);

        gridLayoutManager = new GridLayoutManager(getActivity(), getSpanCount(), 1, false);
        recyclerViewForecast.setLayoutManager(gridLayoutManager);

        recyclerViewForecast.addOnScrollListener(new RecyclerView.OnScrollListener() {

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

        arrayListHr = new ArrayList<>();
        arrayListIcon = new ArrayList<>();
        arrayListCondition = new ArrayList<>();
        arrayListTemp = new ArrayList<>();
        arrayListWind = new ArrayList<>();

        recyclerViewAdapter = new HourForecastRecyclerViewAdapter
                (getActivity(), arrayListHr, arrayListIcon, arrayListCondition, arrayListTemp, arrayListWind);
        recyclerViewForecast.setAdapter(recyclerViewAdapter);

        textViewCurrIcon.setTypeface(weatherFont);

        imageButtonChangeCity.setOnClickListener(this);
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


        if (sharedPreferences.getBoolean("first_run", true)) {
            firstRunDialog();
        }
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
            case R.id.imageButtonChangeCity:
                showInputDialog();
                break;
            case R.id.imageButtonRefresh:
                swipeRefreshLayout.setRefreshing(true);
                updateWeatherData(new CityPreference(getActivity()).getCity());
                break;
            case R.id.imageButtonLocation:
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

    private void firstRunDialog() {
        goodCity = false;

        dialogFirstRun = new Dialog(getActivity());
        dialogFirstRun.setContentView(R.layout.dialog_first_run);
        dialogFirstRun.setCancelable(false);
        dialogFirstRun.setTitle("First Run");

        final CheckBox checkBoxGPS = (CheckBox) dialogFirstRun.findViewById(R.id.checkBoxGPS);
        final EditText editTextInput = (EditText) dialogFirstRun.findViewById(R.id.editTextInput);
        Button buttonGo = (Button) dialogFirstRun.findViewById(R.id.buttonGo);

        checkBoxGPS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isChecked()) {
                    editTextInput.setEnabled(false);
                } else {
                    editTextInput.setEnabled(true);
                }
            }
        });

        // if button is clicked, close the custom dialogFirstRun
        buttonGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkBoxGPS.isChecked()) {
                    swipeRefreshLayout.setRefreshing(true);
                    try {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER, 5000, 100, locationListener);
                        dialogFirstRun.dismiss();
                        sharedPreferences.edit().putBoolean("first_run", false).apply();
                    } catch (SecurityException e) {
                        Log.e("TimmoWeather", "SecurityException: Permission requirements not met.");
                    }
                } else {
                    String text = editTextInput.getText().toString();
                    if (!text.equals("")) {
                        changeCity(text);
                        if (goodCity) {
                            dialogFirstRun.dismiss();
                            sharedPreferences.edit().putBoolean("first_run", false).apply();
                        }
                    } else {
                        goodCity = false;
                        Toast.makeText(getActivity(), "Please Type something...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        dialogFirstRun.show();
    }

    private void showInputDialog() {
        AlertDialog.Builder dialogBuilderChangeCity = new AlertDialog.Builder(getActivity());
        dialogBuilderChangeCity.setTitle("Change city");

        @SuppressLint("InflateParams") View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_change_city, null);

        final EditText editTextInput = (EditText) view.findViewById(R.id.editTextInput);
        final CheckBox checkboxGPS = (CheckBox) view.findViewById(R.id.checkboxGPS);

        checkboxGPS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isChecked()) {
                    editTextInput.setEnabled(false);
                } else {
                    editTextInput.setEnabled(true);
                }
            }
        });

        dialogBuilderChangeCity.setView(view);
        dialogBuilderChangeCity.setPositiveButton("Go", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (checkboxGPS.isChecked()) {
                    swipeRefreshLayout.setRefreshing(true);
                    try {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER, 5000, 100, locationListener);
                    } catch (SecurityException e) {
                        Log.e("TimmoWeather", "SecurityException: Permission requirements not met.");
                    }
                } else {
                    changeCity(editTextInput.getText().toString());
                    swipeRefreshLayout.setRefreshing(true);
                }
                dialog.dismiss();
            }
        });
        dialogBuilderChangeCity.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialogBuilderChangeCity.show();
    }

    private void changeCity(String city) {
        updateWeatherData(city);
        if (goodCity) {
            new CityPreference(getActivity()).setCity(city);
        }
    }

    private void updateWeatherData(final String city) {
        new Thread() {
            public void run() {
                final JSONObject jsonCurrent = RemoteFetchOWMCurrent.getJSON(getActivity(), city);
                final JSONObject jsonForecast = RemoteFetchOWMForecast.getJSON(getActivity(), city);

                if (jsonCurrent == null || jsonForecast == null) {
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(getActivity(),
                                    getActivity().getString(R.string.place_not_found) + " " + city,
                                    Toast.LENGTH_LONG).show();
                            goodCity = false;
                            swipeRefreshLayout.setRefreshing(false);
                            showInputDialog();
                        }
                    });
                } else {
                    handler.post(new Runnable() {
                        public void run() {
                            goodCity = true;
                            renderWeather(jsonCurrent, jsonForecast);
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            }
        }.start();
    }

    private void renderWeather(JSONObject jsonCurrent, JSONObject jsonForecast) {
        try {
            String currCity = jsonCurrent.getString("name") + ", " + jsonCurrent.getJSONObject("sys").getString("country");
            textViewCurrCity.setText(currCity);

            JSONObject detailsCurr = jsonCurrent.getJSONArray("weather").getJSONObject(0);
            String currCondition = detailsCurr.getString("description").toUpperCase(Locale.getDefault());
            if (currCondition.equals("SKY IS CLEAR")) {
                currCondition = "CLEAR SKIES";
            }
            textViewCurrCondition.setText(currCondition);

            textViewCurrIcon.setText(setWeatherIcon(detailsCurr.getInt("id"),
                    jsonCurrent.getJSONObject("sys").getLong("sunrise") * 1000,
                    jsonCurrent.getJSONObject("sys").getLong("sunset") * 1000,
                    new Date().getTime()));

            JSONObject mainCurr = jsonCurrent.getJSONObject("main");

            String tempCurr;
            switch (sharedPreferences.getString("temp_scale", "0")) {
                case "0":
                    tempCurr = getResources().getString(R.string.name_temperature) + " " + String.format("%.2f", mainCurr.getDouble("temp")) + "\u2103";
                    break;
                case "1":
                    tempCurr = getResources().getString(R.string.name_temperature) + " " + String.format("%.2f", convertToFahrenheit(mainCurr.getDouble("temp"))) + "\u2109";
                    break;
                default:
                    tempCurr = getResources().getString(R.string.name_temperature) + " " + String.format("%.2f", mainCurr.getDouble("temp")) + "\u2103";
                    break;
            }
            textViewCurrTemp.setText(tempCurr);
            String humidity = getResources().getString(R.string.name_humidity) + " " + mainCurr.getString("humidity") + "%";
            //String pressure = getResources().getString(R.string.name_pressure) + " " + mainCurr.getString("pressure") + "hPa";
            textViewCurrHumidity.setText(humidity);
            //textViewCurrPressure.setText(pressure);
            String windSpeedCurr;
            switch (sharedPreferences.getString("wind_speed", "0")) {
                case "0":
                    windSpeedCurr = getResources().getString(R.string.name_wind) + " " + jsonCurrent.getJSONObject("wind").getDouble("speed") + "mph";
                    break;
                case "1":
                    windSpeedCurr = getResources().getString(R.string.name_wind) + " " + convertToKPH(jsonCurrent.getJSONObject("wind").getDouble("speed")) + "kph";
                    break;
                default:
                    windSpeedCurr = getResources().getString(R.string.name_wind) + " " + jsonCurrent.getJSONObject("wind").getDouble("speed") + "mph";
                    break;
            }
            textViewCurrWind.setText(windSpeedCurr);

            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
            String updated = getResources().getString(R.string.name_updated) + df.format(new Date(jsonCurrent.getLong("dt") * 1000));
            textViewUpdated.setText(updated);

            arrayListHr.clear();
            arrayListIcon.clear();
            arrayListCondition.clear();
            arrayListTemp.clear();
            arrayListWind.clear();

            int max;
            switch (sharedPreferences.getString("hour_forecast_hours", "1")) {
                case "0":
                    max = 3;
                    break;
                case "1":
                    max = 5;
                    break;
                case "2":
                    max = 7;
                    break;
                case "3":
                    max = 9;
                    break;
                default:
                    max = 5;
                    break;
            }

            DateFormat dfForecastDay = new SimpleDateFormat("EEE", Locale.getDefault());
            DateFormat dfForecastTime = DateFormat.getTimeInstance(DateFormat.SHORT);

            for (int i = 0; i < max; i++) {
                JSONObject mainForecast = jsonForecast.getJSONArray("list").getJSONObject(i).getJSONObject("main");
                JSONObject detailsForecast = jsonForecast.getJSONArray("list").getJSONObject(i).getJSONArray("weather").getJSONObject(0);
                JSONObject windForecast = jsonForecast.getJSONArray("list").getJSONObject(i).getJSONObject("wind");

                Date date = new Date(jsonForecast.getJSONArray("list").getJSONObject(i).getLong("dt") * 1000);
                arrayListHr.add(dfForecastDay.format(date) + " " + dfForecastTime.format(date));
                arrayListIcon.add(setWeatherIcon(detailsForecast.getInt("id"),
                        jsonCurrent.getJSONObject("sys").getLong("sunrise") * 1000,
                        jsonCurrent.getJSONObject("sys").getLong("sunset") * 1000,
                        jsonForecast.getJSONArray("list").getJSONObject(i).getLong("dt") * 1000));
                String conditionForecast = detailsForecast.getString("description").toUpperCase(Locale.getDefault());
                if (conditionForecast.equals("SKY IS CLEAR")) {
                    conditionForecast = "CLEAR SKIES";
                }
                arrayListCondition.add(conditionForecast);
                switch (sharedPreferences.getString("temp_scale", "0")) {
                    case "0":
                        arrayListTemp.add(getResources().getString(R.string.name_temperature) + " " + String.format("%.1f", mainForecast.getDouble("temp")) + "\u2103");
                        break;
                    case "1":
                        arrayListTemp.add(getResources().getString(R.string.name_temperature) + " " + String.format("%.1f", convertToFahrenheit(mainForecast.getDouble("temp"))) + "\u2109");
                        break;
                    default:
                        arrayListTemp.add(getResources().getString(R.string.name_temperature) + " " + String.format("%.1f", mainForecast.getDouble("temp")) + "\u2103");
                        break;
                }
                String windSpeed;
                switch (sharedPreferences.getString("wind_speed", "0")) {
                    case "0":
                        windSpeed = getResources().getString(R.string.name_wind) + " " + windForecast.getDouble("speed") + "mph";
                        break;
                    case "1":
                        windSpeed = getResources().getString(R.string.name_wind) + " " + convertToKPH(windForecast.getDouble("speed")) + "kph";
                        break;
                    default:
                        windSpeed = getResources().getString(R.string.name_wind) + " " + windForecast.getDouble("speed") + "mph";
                        break;
                }

                arrayListWind.add(windSpeed);
            }
            recyclerViewAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            Log.e("TimmoWeather", "One or more fields not found...");
        }
    }

    private Double convertToFahrenheit(Double celsius) {
        return (9.0 / 5.0) * celsius + 32;
    }

    private Double convertToKPH(Double miles) {
        return round(miles * 1.609344);
    }

    @SuppressWarnings("deprecation")
    private String setWeatherIcon(int actualId, long sunrise, long sunset, long time) {
        int id = actualId / 100;
        String icon = "";

        long sunriseTime = new Date(sunrise).getHours();
        long sunsetTime = new Date(sunset).getHours();
        long timeNow = new Date(time).getHours();

        if (timeNow >= sunriseTime && timeNow < sunsetTime) {
            //Toast.makeText(getActivity(), sunriseTime + "\n" + timeNow + "\n" + sunsetTime+ "\n" + "DAY", Toast.LENGTH_LONG).show();
            if (actualId == 800) {
                icon = getActivity().getString(R.string.weather_day_sunny);
            } else {
                switch (id) {
                    case 2:
                        icon = getActivity().getString(R.string.weather_day_thunder);
                        break;
                    case 3:
                        icon = getActivity().getString(R.string.weather_day_drizzle);
                        break;
                    case 7:
                        icon = getActivity().getString(R.string.weather_day_foggy);
                        break;
                    case 8:
                        icon = getActivity().getString(R.string.weather_day_cloudy);
                        break;
                    case 6:
                        icon = getActivity().getString(R.string.weather_day_snowy);
                        break;
                    case 5:
                        icon = getActivity().getString(R.string.weather_day_rainy);
                        break;
                }
            }
        } else {
            //Toast.makeText(getActivity(), sunriseTime + "\n" + timeNow + "\n" + sunsetTime+ "\n" + "NIGHT", Toast.LENGTH_LONG).show();
            if (actualId == 800) {
                icon = getActivity().getString(R.string.weather_night_clear);
            } else {
                switch (id) {
                    case 2:
                        icon = getActivity().getString(R.string.weather_night_thunder);
                        break;
                    case 3:
                        icon = getActivity().getString(R.string.weather_night_drizzle);
                        break;
                    case 7:
                        icon = getActivity().getString(R.string.weather_night_foggy);
                        break;
                    case 8:
                        icon = getActivity().getString(R.string.weather_night_cloudy);
                        break;
                    case 6:
                        icon = getActivity().getString(R.string.weather_night_snowy);
                        break;
                    case 5:
                        icon = getActivity().getString(R.string.weather_night_rainy);
                        break;
                }
            }
        }
        return icon;
    }

    // region getSpanCount
    private int getSpanCount() {
        int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (screenSize >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
            return 3;
        } else {
            return 2;
        }
    }
    // endregion

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
            goodCity = true;
            changeCity(cityName);
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