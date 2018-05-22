package challenge.com.cameraiqweatherchallenge;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.View;

import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class WeatherActivity extends AppCompatActivity {

    private final String CUR_REQUEST_URL = "http://api.openweathermap.org/data/2.5/weather?units=metric&mode=json&appid=adb4503a31093fed77c0a5f39d4c512b&q=";

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;


    private ArrayList<String> cities;
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private SharedPreferences weatherPreferences;

    private int currentCityIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        weatherPreferences = getSharedPreferences("WEATHER_DATA",0 );
        final Set<String> cities = weatherPreferences.getStringSet("CITIES", null);
        if(cities != null) {
            this.cities = new ArrayList<>(cities);
        } else {
            this.cities = new ArrayList<>();
            //Default city instead of empty, can be deleted with delete option as long as there is at least 1 item in the array
            this.cities.add("Los Angeles");
        }

        // Create the adapter that will return a fragment for each city weather
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                currentCityIndex = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });
        mViewPager.setOffscreenPageLimit(10);
        ImageButton removeButton = findViewById(R.id.remove);
        ImageButton refreshButton = findViewById(R.id.refresh);
        ImageButton addButton = findViewById(R.id.add);

        removeButton.setOnClickListener(new RemoveListener());

        refreshButton.setOnClickListener(new RefreshListener());

        addButton.setOnClickListener(new AddCityListener());
    }

    //Removes city from array and refreshes view pager
    private class RemoveListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if(cities.size() > 1) {
                Toast.makeText(getApplicationContext(), "Index: "+currentCityIndex, Toast.LENGTH_SHORT).show();
                cities.remove(cities.get(currentCityIndex));
                Set<String> cityList = new HashSet<>(cities);
                mViewPager.getAdapter().notifyDataSetChanged();
                weatherPreferences.edit().putStringSet("CITIES", cityList).apply();
            } else {
                Toast.makeText(getApplication(), "Add another city before deleting the last one", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Clears shared preferences to force reload from network
    private class RefreshListener implements View.OnClickListener {
        @SuppressLint("ApplySharedPref")
        @Override
        public void onClick(View view) {
            weatherPreferences.edit().clear().commit();
            mViewPager.getAdapter().notifyDataSetChanged();
        }
    }

    //Listener to activate prompt for city name from user

    private class AddCityListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            //Launch input prompt
            //Add city to global list
            //notify data set changed of view pager to refresh the views
            final AlertDialog.Builder inputAlert = new AlertDialog.Builder(WeatherActivity.this);
            inputAlert.setTitle("Enter City Name");
            inputAlert.setMessage("Will be added to the back of the list");
            final EditText userInput = new EditText(WeatherActivity.this);
            inputAlert.setView(userInput);
            inputAlert.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String cityInputValue = userInput.getText().toString();
                    new CityValidateTask().execute(cityInputValue);
                }
            });
            inputAlert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog alertDialog = inputAlert.create();
            alertDialog.show();
        }
    }


    //Background task to validate city name against weather database

    @SuppressLint("StaticFieldLeak")
    private class CityValidateTask extends AsyncTask<String, Void, Boolean> {

        String city;

        @Override
        protected Boolean doInBackground(String... cits) {
            city = cits[0];
            if(cities.indexOf(city) < 0) {
                String response = sendGetRequest(CUR_REQUEST_URL + city);
                return !response.contains("Error");
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean valid) {
            super.onPostExecute(valid);
            if(valid) {
                Set<String> cityList = weatherPreferences.getStringSet("CITIES", null);
                cities.add(city);
                if(cityList == null){
                    cityList = new HashSet<>(cities);
                }else {
                    cityList.add(city);
                }
                weatherPreferences.edit().putStringSet("CITIES", cityList).apply();
                mViewPager.getAdapter().notifyDataSetChanged();
                Toast.makeText(WeatherActivity.this, "City Added!", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(WeatherActivity.this, "City Cannot Be Added", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the weather pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return WeatherFragment.newInstance(cities.get(position));
        }

        @Override
        public int getCount() {
            return cities.size();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            return super.instantiateItem(container, position);
        }

        @Override
        public int getItemPosition(Object item) {
            WeatherFragment fragment = (WeatherFragment) item;
            String title = fragment.getCityName();
            int position = cities.indexOf(title);

            if (position >= 0) {
                return position;
            } else {
                return POSITION_NONE;
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
            e.printStackTrace();
        }
        return response;
    }

}
