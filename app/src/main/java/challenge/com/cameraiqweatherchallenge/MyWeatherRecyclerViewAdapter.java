package challenge.com.cameraiqweatherchallenge;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import challenge.com.cameraiqweatherchallenge.Models.Day;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Day}.
 */
public class MyWeatherRecyclerViewAdapter extends RecyclerView.Adapter<MyWeatherRecyclerViewAdapter.ViewHolder> {

    private final String ICON_URI = "http://openweathermap.org/img/w/";

    private final List<Day> forecast;

    MyWeatherRecyclerViewAdapter(List<Day> days) {
        forecast = days;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_weather, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.day = forecast.get(position);
        holder.dateField.setText(forecast.get(position).getDate());
        holder.dayField.setText(forecast.get(position).getWeekDay());
        holder.tempField.setText(forecast.get(position).getTemp());
        new DownloadImageTask(holder.weatherIcon).execute(ICON_URI+forecast.get(position).getIcon()+".png");
    }

    @Override
    public int getItemCount() {
        return forecast.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final TextView dayField;
        final TextView dateField;
        final ImageView weatherIcon;
        final TextView tempField;
        Day day;

        ViewHolder(View view) {
            super(view);
            mView = view;
            dateField = (TextView) view.findViewById(R.id.day_item_date);
            dayField = (TextView) view.findViewById(R.id.day_item_name);
            weatherIcon = (ImageView) view.findViewById(R.id.day_item_icon);
            tempField = (TextView) view.findViewById(R.id.day_item_temp);

        }

        @Override
        public String toString() {
            return super.toString() + " '" + dateField.getText() + "'";
        }
    }

    //Load image icon asynchronously -- can find a safer way with more time
    @SuppressLint("StaticFieldLeak")
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
}
