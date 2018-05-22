package challenge.com.cameraiqweatherchallenge;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import challenge.com.cameraiqweatherchallenge.Models.Day;

/**
 * A fragment representing a list of Days.
 */
public class WeatherFragment extends Fragment {

    private final String REQUEST_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?&mode=json&units=metric&cnt=5&appid=adb4503a31093fed77c0a5f39d4c512b&q=";

    private final String CUR_REQUEST_URL = "http://api.openweathermap.org/data/2.5/weather?units=metric&mode=json&appid=adb4503a31093fed77c0a5f39d4c512b&q=";

    private final String ICON_URI = "http://openweathermap.org/img/w/";

    private final String WEATHER_DATA = "WEATHER_DATA";

    private static final String ARG_CITY_NAME = "city-name";

    private boolean isCel;

    private String cityName;

    Day curDay;

    ArrayList<Day> forecast;

    RecyclerView recyclerView;

    ImageView currentIcon;
    TextView currentTemp;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public WeatherFragment() {

    }

    public static WeatherFragment newInstance(String cityName) {
        WeatherFragment fragment = new WeatherFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CITY_NAME, cityName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            cityName = getArguments().getString(ARG_CITY_NAME);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather, container, false);
        //get current forecast
        forecast = new ArrayList<>();
        isCel = true;
        TextView cityNameView = view.findViewById(R.id.cityName);
        cityNameView.setText(cityName);
        TextView metricView = view.findViewById(R.id.metric);
        metricView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(Day day : forecast){
                    day.convert();
                }
                curDay.convert();
                recyclerView.getAdapter().notifyDataSetChanged();
                if(isCel){
                    ((TextView) view).setText("Farenheit");
                    currentTemp.setText(curDay.getTemp());
                }else {
                    ((TextView) view).setText("Celcius");
                    currentTemp.setText(curDay.getTemp());
                }
                isCel = !isCel;
            }
        });

        recyclerView = view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        Calendar today = Calendar.getInstance();
        int date = today.get(Calendar.DATE);

        //fetch the forecast array from local storage, if empty or outdated load from API
        SharedPreferences savedWeatherData = getContext().getSharedPreferences(WEATHER_DATA, 0);
        String weatherDataJsonText = savedWeatherData.getString(cityName+date, null);

        if(weatherDataJsonText != null) {
            try {
                JSONObject cityWeatherData = new JSONObject(weatherDataJsonText);
                Log.d("GET_RESPONSE_LIST2", cityWeatherData.getString("list"));
                JSONArray foreCastDataArray = cityWeatherData.getJSONArray("list");
                for(int i = 0; i < foreCastDataArray.length(); i++){
                    JSONObject dayObject = foreCastDataArray.getJSONObject(i);
                    SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.US);
                    Date d = today.getTime();
                    String dayOfTheWeek = sdf.format(d);
                    SimpleDateFormat dayDateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                    String dayDate = dayDateFormat.format(d);
                    //get day temperature from temp array
                    JSONObject tempObject = dayObject.getJSONObject("temp");
                    String temp = tempObject.getString("day");
                    //get icon from weather array
                    JSONArray weatherArray = dayObject.getJSONArray("weather");
                    JSONObject weatherObject = weatherArray.getJSONObject(0);
                    String icon  = weatherObject.getString("icon");
                    Day day = new Day(dayDate,dayOfTheWeek,icon,temp);
                    forecast.add(day);
                    today.add(Calendar.DAY_OF_YEAR,1 );
                }
            } catch(JSONException j){
                Log.e("JSON Error", j.getMessage());
            }
        } else {
            new LoadForecastTask().execute();
        }

        String curWeatherDataJsonText = savedWeatherData.getString("cur"+cityName+date, null);
        currentIcon = view.findViewById(R.id.currentIcon);
        currentTemp = view.findViewById(R.id.currentTemp);

        if(curWeatherDataJsonText != null){
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.US);
                Date d = today.getTime();
                String dayOfTheWeek = sdf.format(d);
                SimpleDateFormat dayDateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                String dayDate = dayDateFormat.format(d);
                JSONObject curCityWeatherData = new JSONObject(curWeatherDataJsonText);
                JSONObject main = curCityWeatherData.getJSONObject("main");
                String temp = main.getString("temp");
                JSONArray weatherArray = curCityWeatherData.getJSONArray("weather");
                JSONObject weatherObj = weatherArray.getJSONObject(0);
                String icon = weatherObj.getString("icon");

                curDay = new Day(dayDate,dayOfTheWeek,icon,temp);
                currentTemp.setText(curDay.getTemp());
                new DownloadImageTask(currentIcon).execute(ICON_URI+icon+".png");

            } catch(JSONException j){
                Log.e("JSON Error", j.getMessage());
            }
        }else {
            new LoadCurrentTask().execute();
        }

        recyclerView.setAdapter(new MyWeatherRecyclerViewAdapter(forecast));

        return view;
    }


    //load current data from network
    @SuppressLint("StaticFieldLeak")
    private class LoadCurrentTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void ... voids) {
            Calendar today = Calendar.getInstance();
            int date = today.get(Calendar.DATE);
            //TODO: fetch the forecast array from local storage, if empty or outdated load from API
            SharedPreferences savedWeatherData = getContext().getSharedPreferences(WEATHER_DATA, 0);
            //fetch data from server
            //

            final String weatherDataResponse = sendGetRequest(CUR_REQUEST_URL+cityName);
            if(!weatherDataResponse.isEmpty()){
                savedWeatherData.edit().putString("cur"+cityName+date, weatherDataResponse).apply();
                return weatherDataResponse;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String weatherDataResponse) {
            super.onPostExecute(weatherDataResponse);
            if(weatherDataResponse != null) {
                try {
                    Calendar today = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.US);
                    Date d = today.getTime();
                    String dayOfTheWeek = sdf.format(d);
                    SimpleDateFormat dayDateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                    String dayDate = dayDateFormat.format(d);
                    Log.d("CURRENT DATA JSON", weatherDataResponse);
                    JSONObject curCityWeatherData = new JSONObject(weatherDataResponse);
                    JSONObject main = curCityWeatherData.getJSONObject("main");
                    String temp = main.getString("temp");
                    JSONArray weatherArray = curCityWeatherData.getJSONArray("weather");
                    JSONObject weatherObj = weatherArray.getJSONObject(0);
                    String icon = weatherObj.getString("icon");

                    curDay = new Day(dayDate, dayOfTheWeek, icon, temp);
                    currentTemp.setText(curDay.getTemp());
                    new DownloadImageTask(currentIcon).execute(ICON_URI + icon + ".png");

                } catch (JSONException j) {
                    Log.e("JSON Error", j.getMessage());
                    j.printStackTrace();
                }
            }
        }
    }

    //Load forecast data from network
    @SuppressLint("StaticFieldLeak")
    private class LoadForecastTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void ... voids) {
            forecast.clear();
            Calendar today = Calendar.getInstance();
            int date = today.get(Calendar.DATE);
            //TODO: fetch the forecast array from local storage, if empty or outdated load from API
            SharedPreferences savedWeatherData = getContext().getSharedPreferences(WEATHER_DATA, 0);
            //fetch data from server

            HashMap<String, String> data = new HashMap<>();
            String weatherDataResponse = sendGetRequest(REQUEST_URL+cityName);
            if(!weatherDataResponse.isEmpty()){
                try {
                    Log.d("GET_RESPONSE", weatherDataResponse);
                    JSONObject cityWeatherData = new JSONObject(weatherDataResponse);
                    savedWeatherData.edit().putString(cityName+date,weatherDataResponse ).apply();
                    JSONArray foreCastDataArray = cityWeatherData.getJSONArray("list");
                    for(int i = 0; i < foreCastDataArray.length(); i++){
                        JSONObject dayObject = foreCastDataArray.getJSONObject(i);
                        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.US);
                        Date d = today.getTime();
                        String dayOfTheWeek = sdf.format(d);
                        SimpleDateFormat dayDateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                        String dayDate = dayDateFormat.format(d);
                        //get day temperature from temp array
                        JSONObject tempObject = dayObject.getJSONObject("temp");
                        String temp = tempObject.getString("day");
                        //get icon from weather array
                        JSONArray weatherArray = dayObject.getJSONArray("weather");
                        JSONObject weatherObject = weatherArray.getJSONObject(0);
                        String icon  = weatherObject.getString("icon");
                        Day day = new Day(dayDate,dayOfTheWeek,icon,temp);
                        forecast.add(day);
                        today.add(Calendar.DAY_OF_YEAR,1 );
                    }
                    return true;
                } catch(JSONException j){
                    Log.e("JSON Error2", j.toString());
                    j.printStackTrace();
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean valid) {
            super.onPostExecute(valid);
            if(valid){
                recyclerView.getAdapter().notifyDataSetChanged();
            }
        }
    }

    public String sendGetRequest(String requestURL){
        URL url;
        String response = "";
        try {
            url = new URL(requestURL);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(30000);
            urlConnection.setConnectTimeout(30000);
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();

            if(responseCode == HttpURLConnection.HTTP_OK){
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                response = reader.readLine();
            }else{
                response = "Error Registering";
            }
        } catch (IOException e) {
            //make response equal to the error and handle it on the main page
            e.printStackTrace();
        }
        //Log.d("RESPONSE", response);
        return response;
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

    public String getCityName(){
        return cityName;
    }
}
