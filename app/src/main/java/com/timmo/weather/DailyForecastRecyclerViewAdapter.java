package com.timmo.weather;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class DailyForecastRecyclerViewAdapter extends RecyclerView.Adapter<DailyForecastRecyclerViewAdapter.ViewHolder> {

    private static Context mContext;
    private final ArrayList<String> arrayListHr;
    private final ArrayList<String> arrayListIcon;
    private final ArrayList<String> arrayListCondition;
    private final ArrayList<String> arrayListTempMin;
    private final ArrayList<String> arrayListTempMax;

    public DailyForecastRecyclerViewAdapter(Context context, ArrayList<String> hr,
                                            ArrayList<String> icon, ArrayList<String> condition,
                                            ArrayList<String> tempMin, ArrayList<String> tempMax) {
        mContext = context;
        arrayListHr = hr;
        arrayListIcon = icon;
        arrayListCondition = condition;
        arrayListTempMin = tempMin;
        arrayListTempMax = tempMax;
    }

    @Override
    public DailyForecastRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate
                (R.layout.view_forecast_daily_item, parent, false);
        ViewHolder holder = new ViewHolder(v);
        holder.textViewDay.setTag(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        Typeface weatherFont = Typeface.createFromAsset(mContext.getAssets(), "weather.ttf");
        holder.textViewIcon.setTypeface(weatherFont);

        holder.textViewDay.setText(arrayListHr.get(position));
        holder.textViewIcon.setText(arrayListIcon.get(position));
        holder.textViewCondition.setText(arrayListCondition.get(position));
        holder.textViewTempMin.setText(arrayListTempMin.get(position));
        holder.textViewTempMax.setText(arrayListTempMax.get(position));
    }

    @Override
    public int getItemCount() {
        return arrayListHr.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView textViewDay, textViewIcon, textViewCondition, textViewTempMin, textViewTempMax;

        public ViewHolder(View v) {
            super(v);
            textViewDay = (TextView) v.findViewById(R.id.textViewDay);
            textViewIcon = (TextView) v.findViewById(R.id.textViewIcon);
            textViewCondition = (TextView) v.findViewById(R.id.textViewCondition);
            textViewTempMin = (TextView) v.findViewById(R.id.textViewTempMin);
            textViewTempMax = (TextView) v.findViewById(R.id.textViewTempMax);
        }
    }
}