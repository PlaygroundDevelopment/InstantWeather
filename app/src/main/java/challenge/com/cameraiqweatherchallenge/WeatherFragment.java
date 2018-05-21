package challenge.com.cameraiqweatherchallenge;

import android.annotation.SuppressLint;
import android.content.Context;
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
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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

    ArrayList<Day> forecast;

    RecyclerView recyclerView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public WeatherFragment() {

    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
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
        //TODO: get current forecast
        forecast = new ArrayList<>();
        isCel = true;
        TextView cityNameView = (TextView) view.findViewById(R.id.cityName);
        cityNameView.setText(cityName);
        TextView metricView = (TextView) view.findViewById(R.id.metric);
        metricView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(Day day : forecast){
                    day.convert();
                }
                recyclerView.getAdapter().notifyDataSetChanged();
                if(isCel){
                    ((TextView) view).setText("Farenheit");
                }else {
                    ((TextView) view).setText("Celcius");
                }
                isCel = !isCel;
            }
        });

        recyclerView = (RecyclerView) view.findViewById(R.id.list);
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
                    today.add(Calendar.DAY_OF_YEAR,i );
                    SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.US);
                    Date d = today.getTime();
                    String dayOfTheWeek = sdf.format(d);
                    SimpleDateFormat dayDateFormat = new SimpleDateFormat("MM-DD-YYYY", Locale.US);
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
                }
            } catch(JSONException j){
                Log.e("JSON Error", j.getMessage());
            }
        } else {
            loadForecast();
        }

        String curWeatherDataJsonText = savedWeatherData.getString("cur"+cityName+date, null);
        ImageView currentIcon = (ImageView) view.findViewById(R.id.currentIcon);
        TextView currentTemp = (TextView) view.findViewById(R.id.currentTemp);
        if(curWeatherDataJsonText != null){
            try {
                JSONObject curCityWeatherData = new JSONObject(curWeatherDataJsonText);
                JSONObject main = curCityWeatherData.getJSONObject("main");
                String temp = main.getString("temp");
                JSONArray weatherArray = curCityWeatherData.getJSONArray("weather");
                JSONObject weatherObj = weatherArray.getJSONObject(0);
                String icon = weatherObj.getString("icon");

                currentTemp.setText(temp);
                new DownloadImageTask(currentIcon).execute(ICON_URI+icon+".png");

            } catch(JSONException j){
                Log.e("JSON Error", j.getMessage());
            }
        }else {
            loadCurrent(currentIcon, currentTemp);
        }

        recyclerView.setAdapter(new MyWeatherRecyclerViewAdapter(forecast));

        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void loadCurrent(final ImageView currentIcon, final TextView currentTemp) {
        Thread loadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Calendar today = Calendar.getInstance();
                int date = today.get(Calendar.DATE);
                //TODO: fetch the forecast array from local storage, if empty or outdated load from API
                SharedPreferences savedWeatherData = getContext().getSharedPreferences(WEATHER_DATA, 0);
                //fetch data from server
                //

                HashMap<String, String> data = new HashMap<>();
                final String weatherDataResponse = sendGetRequest(CUR_REQUEST_URL+cityName, data);
                if(!weatherDataResponse.isEmpty()){
                    savedWeatherData.edit().putString("cur"+cityName+date, weatherDataResponse).apply();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Log.d("CURRENT DATA JSON", weatherDataResponse);
                                JSONObject curCityWeatherData = new JSONObject(weatherDataResponse);
                                JSONObject main = curCityWeatherData.getJSONObject("main");
                                String temp = main.getString("temp");
                                JSONArray weatherArray = curCityWeatherData.getJSONArray("weather");
                                JSONObject weatherObj = weatherArray.getJSONObject(0);
                                String icon = weatherObj.getString("icon");

                                currentTemp.setText(temp);
                                new DownloadImageTask(currentIcon).execute(ICON_URI+icon+".png");
                                Log.d("GET_RESPONSE_LIST", curCityWeatherData.getString("list"));

                            } catch(JSONException j){
                                Log.e("JSON Error", j.getMessage());
                                j.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
        loadThread.start();
    }

    private void loadForecast(){
        Thread loadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                forecast.clear();
                Calendar today = Calendar.getInstance();
                int date = today.get(Calendar.DATE);
                //TODO: fetch the forecast array from local storage, if empty or outdated load from API
                SharedPreferences savedWeatherData = getContext().getSharedPreferences(WEATHER_DATA, 0);
                //fetch data from server

                HashMap<String, String> data = new HashMap<>();
                String weatherDataResponse = sendGetRequest(REQUEST_URL+cityName, data);
                if(!weatherDataResponse.isEmpty()){
                    try {
                        Log.d("GET_RESPONSE", weatherDataResponse);
                        JSONObject cityWeatherData = new JSONObject(weatherDataResponse);
                        savedWeatherData.edit().putString(cityName+date,weatherDataResponse ).apply();
                        JSONArray foreCastDataArray = cityWeatherData.getJSONArray("list");
                        for(int i = 0; i < foreCastDataArray.length(); i++){
                            JSONObject dayObject = foreCastDataArray.getJSONObject(i);
                            today.add(Calendar.DAY_OF_YEAR,i );
                            SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.US);
                            Date d = today.getTime();
                            String dayOfTheWeek = sdf.format(d);
                            SimpleDateFormat dayDateFormat = new SimpleDateFormat("MM-DD-YYYY", Locale.US);
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
                        }
                    } catch(JSONException j){
                        Log.e("JSON Error2", j.toString());
                        j.printStackTrace();
                    }
                }
                //refresh list view
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recyclerView.getAdapter().notifyDataSetChanged();
                    }
                });
            }
        });
        loadThread.start();
    }

    public String sendGetRequest(String requestURL, HashMap<String, String> postDataParams){
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

            OutputStream outputStream = urlConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            String dataString = getDataString(postDataParams);
            Log.d("DATA_STRING" , dataString);
            writer.write(dataString);
            writer.flush();
            writer.close();
            outputStream.close();
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

    private String getDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if(first){
                first = false;
            }else{
                result.append("&");
            }

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
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
