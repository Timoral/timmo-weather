package com.timmo.weather;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;

public class AstronomyFragment extends android.support.v4.app.Fragment {

    private final Handler handler;
    private ImageView imageViewPhase;
    private TextView textViewCity, textViewPhase, textViewHemisphere, textViewDays;
    private boolean north = true;

    public AstronomyFragment() {
        handler = new Handler();
    }

    private static Bitmap flipVertical(Bitmap src) {
        Matrix matrix = new Matrix();
        matrix.preScale(1.0f, -1.0f);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_astronomy, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textViewCity = (TextView) getActivity().findViewById(R.id.textViewCity);
        imageViewPhase = (ImageView) getActivity().findViewById(R.id.imageViewPhase);
        textViewPhase = (TextView) getActivity().findViewById(R.id.textViewPhase);
        textViewHemisphere = (TextView) getActivity().findViewById(R.id.textViewHemisphere);
        textViewDays = (TextView) getActivity().findViewById(R.id.textViewDays);

        updateAstronomyData();
    }

    private void updateAstronomyData() {
        MoonHelper moonHelper = new MoonHelper();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        int year = calendar.get(Calendar.YEAR);
        // January is 0 for some reason..
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        String phaseName = moonHelper.phaseName(moonHelper.moonPhase(year, month, day));
        getHemisphere(new CityPreference(getActivity()).getCity(), phaseName);

        String days = "Days in " + DateFormat.format("MMMM", new Date()) + ": " +
                moonHelper.daysInMonth(month, year) + " Days";
        textViewDays.setText(days);
    }

    private void getHemisphere(final String city, final String phaseName) {
        new Thread() {
            public void run() {
                final JSONObject jsonCurrent = RemoteFetchOWMCurrent.getJSON(getActivity(), city);

                if (jsonCurrent == null) {
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(getActivity(),
                                    getActivity().getString(R.string.place_not_found) + " " + city,
                                    Toast.LENGTH_LONG).show();
                            north = true;
                        }
                    });
                } else {
                    handler.post(new Runnable() {
                        public void run() {
                            try {
                                String currCity = jsonCurrent.getString("name") + ", " + jsonCurrent.getJSONObject("sys").getString("country");
                                textViewCity.setText(currCity);

                                float lat = Float.parseFloat(jsonCurrent.getJSONObject("coord").getString("lat"));
                                north = lat >= 0;

                                imageViewPhase.setImageResource(setAstronomyIcon(phaseName));
                                if (!north) {
                                    Bitmap bitmap = ((BitmapDrawable) imageViewPhase.getDrawable()).getBitmap();
                                    imageViewPhase.setImageBitmap(flipVertical(bitmap));
                                }
                                textViewPhase.setText(phaseName);

                                if (north) {
                                    textViewHemisphere.setText(getResources().getString(R.string.northern_hemisphere));
                                } else {
                                    textViewHemisphere.setText(getResources().getString(R.string.southern_hemisphere));
                                }
                            } catch (JSONException e) {
                                Log.e("TimmoWeather", "One or more fields not found...");
                            }
                        }
                    });
                }
            }
        }.start();
    }


    private int setAstronomyIcon(String phaseName) {
        switch (phaseName) {
            case "New Moon":
                return R.drawable.moon_new;
            case "Waxing Crescent":
                return R.drawable.moon_waxing_crescent;
            case "First Quarter":
                return R.drawable.moon_first_quarter;
            case "Waxing Gibbous":
                return R.drawable.moon_waxing_gibbous;
            case "Full Moon":
                return R.drawable.moon_full;
            case "Waning Gibbous":
                return R.drawable.moon_waning_gibbous;
            case "Last Quarter":
                return R.drawable.moon_last_quarter;
            case "Waning Crescent":
                return R.drawable.moon_waning_cresent;
            default:
                return R.drawable.moon_new;
        }
    }
}