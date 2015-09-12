package com.timmo.weather;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class ForecastRecyclerViewAdapter extends RecyclerView.Adapter<ForecastRecyclerViewAdapter.ViewHolder> {

    private static Context mContext;
    private final ArrayList<String> arrayListHr;
    private final ArrayList<String> arrayListIcon;
    private final ArrayList<String> arrayListCondition;
    private final ArrayList<String> arrayListTemp;

    public ForecastRecyclerViewAdapter(Context context, ArrayList<String> hr, ArrayList<String> icon,
                                       ArrayList<String> condition, ArrayList<String> temp) {
        mContext = context;
        arrayListHr = hr;
        arrayListIcon = icon;
        arrayListCondition = condition;
        arrayListTemp = temp;
    }

    @Override
    public ForecastRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate
                (R.layout.view_forecast_item, parent, false);
        ViewHolder holder = new ViewHolder(v);
        holder.textViewHrHr.setTag(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        Typeface weatherFont = Typeface.createFromAsset(mContext.getAssets(), "weather.ttf");
        holder.textViewHrIcon.setTypeface(weatherFont);

        holder.textViewHrHr.setText(arrayListHr.get(position));
        holder.textViewHrIcon.setText(arrayListIcon.get(position));
        holder.textViewHrCondition.setText(arrayListCondition.get(position));
        holder.textViewHrTemp.setText(arrayListTemp.get(position));
    }

    @Override
    public int getItemCount() {
        return arrayListHr.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView textViewHrHr, textViewHrIcon, textViewHrCondition, textViewHrTemp;

        public ViewHolder(View v) {
            super(v);
            textViewHrHr = (TextView) v.findViewById(R.id.textViewHrHr);
            textViewHrIcon = (TextView) v.findViewById(R.id.textViewHrIcon);
            textViewHrCondition = (TextView) v.findViewById(R.id.textViewHrCondition);
            textViewHrTemp = (TextView) v.findViewById(R.id.textViewHrTemp);
        }
    }
}