package com.timmo.weather;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class HourForecastRecyclerViewAdapter extends RecyclerView.Adapter<HourForecastRecyclerViewAdapter.ViewHolder> {

    private static Context mContext;
    private final ArrayList<String> arrayListHr;
    private final ArrayList<String> arrayListIcon;
    private final ArrayList<String> arrayListCondition;
    private final ArrayList<String> arrayListTemp;

    public HourForecastRecyclerViewAdapter(Context context, ArrayList<String> hr, ArrayList<String> icon,
                                           ArrayList<String> condition, ArrayList<String> temp) {
        mContext = context;
        arrayListHr = hr;
        arrayListIcon = icon;
        arrayListCondition = condition;
        arrayListTemp = temp;
    }

    @Override
    public HourForecastRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate
                (R.layout.view_forecast_item, parent, false);
        ViewHolder holder = new ViewHolder(v);
        holder.textViewHr.setTag(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        Typeface weatherFont = Typeface.createFromAsset(mContext.getAssets(), "weather.ttf");
        holder.textViewIcon.setTypeface(weatherFont);

        holder.textViewHr.setText(arrayListHr.get(position));
        holder.textViewIcon.setText(arrayListIcon.get(position));
        holder.textViewCondition.setText(arrayListCondition.get(position));
        holder.textViewTemp.setText(arrayListTemp.get(position));

        holder.cardViewForecast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return arrayListHr.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final CardView cardViewForecast;
        public final TextView textViewHr, textViewIcon, textViewCondition, textViewTemp;

        public ViewHolder(View v) {
            super(v);
            cardViewForecast = (CardView) v.findViewById(R.id.cardViewForecast);
            textViewHr = (TextView) v.findViewById(R.id.textViewHr);
            textViewIcon = (TextView) v.findViewById(R.id.textViewIcon);
            textViewCondition = (TextView) v.findViewById(R.id.textViewCondition);
            textViewTemp = (TextView) v.findViewById(R.id.textViewTemp);
        }
    }
}