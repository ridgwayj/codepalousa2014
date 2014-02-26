package com.knoxmobilitylab.weatherexplorer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.StringRes;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

@EActivity(R.layout.activity_main)
public class MainActivity extends Activity {
	
	private static String API_BASE = "http://api.wunderground.com/api/";
	private static String ICON_BASE = "http://icons.wxug.com/i/c/h/";
	private static String CONDITIONS = "/conditions/q/";
	private String uvIndex;
	
	@StringRes(R.string.credits)
	String credits;
	
	@ViewById
	TextView textViewCreditsTag;
	@ViewById
	TextView textViewTemp;
	@ViewById(R.id.textViewLocation)
	TextView textViewLocationValue;
	@ViewById
	TextView textViewFeelsLike;
	@ViewById
	TextView textViewWeather;
	@ViewById
	TextView textViewWinds;
	@ViewById
	TextView textViewRelativeHumidity;
	@ViewById
	TextView textViewBarometricPressure;
	@ViewById
	TextView textViewDewpoint;
	@ViewById
	TextView textViewVisibility;
	@ViewById
	TextView textViewObservationTime;
	@ViewById
	EditText editTextZip;
	@ViewById
	ImageView imageViewWeatherIcon;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @AfterViews
    void updateCreditsTag() {  
    	
    	textViewCreditsTag.setText(credits);
    	
    }
       
    //Retrieve current weather conditions & update display
    @Click
    public void buttonGetWeather(View view) {
        // check if you are connected or not
        if(!isConnected()){
        	Toast.makeText(getBaseContext(), "Not Connected. Try again when connected.", Toast.LENGTH_LONG).show();
            return;      
        }
 
    	String zip = editTextZip.getText().toString();
    	if (zip.length() != 5) {
        	Toast.makeText(getBaseContext(), "Only 5 digit zip codes are supported. Please re-enter the zip.", Toast.LENGTH_LONG).show();
            return;         		
    	}

    	//build the URL to fetch the conditions weather data
        StringBuilder sb = new StringBuilder(API_BASE);
        
        sb.append(ApiPrefs.getApiKey()).append(CONDITIONS).append(zip).append(".json");
        getWeatherData(sb.toString());
        
    }    	


    @Background
    protected void getWeatherData(String weatherApiUrl) {
		String weatherData = null;

		weatherData = fetchWeatherData(weatherApiUrl);

		try {
    		// convert String to JSONObject
			JSONObject jsonResult = new JSONObject(weatherData);		
			setCurrentConditions(jsonResult);

		} catch (JSONException e) {
			e.printStackTrace();
		} 
		
    }
    
	@UiThread
	protected void setCurrentConditions(JSONObject jsonResult) {
		try {

			Toast.makeText(getBaseContext(), "Received Current Weather!", Toast.LENGTH_SHORT).show();
			
			//process the result data
			JSONObject currentObservation = jsonResult.getJSONObject("current_observation"); // get current_observation
			JSONObject displayLocation = currentObservation.getJSONObject("display_location");

			//Display the temperature
			textViewTemp.setText(currentObservation.getString("temp_f")+" ºF");

			//Display the location
			textViewLocationValue.setText(displayLocation.getString("full"));

			//Display the feels like temperature
			textViewFeelsLike.setText(currentObservation.getString("feelslike_f")+" ºF");

			//Display the weather
			textViewWeather.setText(currentObservation.getString("weather"));

			//Display the wind
			textViewWinds.setText(currentObservation.getString("wind_string"));

			//Display the relative humidity
			textViewRelativeHumidity.setText(currentObservation.getString("relative_humidity"));

			//Display the barometric pressure
			textViewBarometricPressure.setText(currentObservation.getString("pressure_in")+" in Hg "+
			currentObservation.getString("pressure_trend"));

			//Display the dewpoint
			textViewDewpoint.setText(currentObservation.getString("dewpoint_f")+" ºF");

			//Display the visibility
			textViewVisibility.setText(currentObservation.getString("visibility_mi")+" miles");

			//Display the observation time
			textViewObservationTime.setText(currentObservation.getString("observation_time"));

			//capture the precipitation for today
			uvIndex = currentObservation.getString("UV");
			
			//build the URL for the current conditions icon
			StringBuilder iconUrl = new StringBuilder(ICON_BASE).append(currentObservation.getString("icon")).append(".gif");

			//Set the current conditions icon if we have one
			if (iconUrl.toString() != null) {
				getWeatherIcon(iconUrl.toString());
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} 

	}
   
	@Background
	protected void getWeatherIcon(String iconUrl) {
		Log.d("WeatherExplorer", "Starting background fetch of icon...");
		Bitmap weatherIcon = fetchWeatherIcon(iconUrl);
		updateWeatherIcon(weatherIcon);
	}
	
	@UiThread
	protected void updateWeatherIcon(Bitmap weatherIcon) {
		if (weatherIcon != null) {
    		Log.d("WeatherExplorer", "Setting fetched icon.");

    		//retrieve the imageView and set the icon			
			imageViewWeatherIcon.setImageBitmap(weatherIcon);
		}

	}
	
	@Click
	public void imageViewWeatherIcon(View view) {
	  Toast.makeText(getBaseContext(), "Today's UV Index is "+uvIndex, Toast.LENGTH_LONG).show();	
	}
	
	/*  UTILITY METHODS */
    //check to see if connected to a network
    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) 
                return true;
            else
                return false;   
    }

    //static method to retrieve weather data from the RESTFul weather API
    public static String fetchWeatherData(String url){

    	String result = "";
    	
    	//fetch JSON weather data from RESTful service
    	try {
    		URL weatherDataUrl = new URL (url);
    		HttpURLConnection conn =
    				(HttpURLConnection) weatherDataUrl.openConnection();
    		conn.setDoInput(true);
    		conn.connect();

    		InputStream inputStream = conn.getInputStream();
    		result = convertInputStreamToString(inputStream);

    	} catch (MalformedURLException e) {
    		e.printStackTrace();
    	} catch (Exception e) {
    		e.printStackTrace();
    	} 

    	return result;
    }

    //utility method to convert inputstream data to a string
    private static String convertInputStreamToString(InputStream inputStream) throws IOException{
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;
 
        inputStream.close();
        return result;
 
    }
  
    //static method to pull images from an HTTP URL
    public static Bitmap fetchWeatherIcon(String url){
        Bitmap result = null;
        try {
            URL iconUrl = new URL (url);
            HttpURLConnection conn =
              (HttpURLConnection) iconUrl.openConnection();
            conn.setDoInput(true);
            conn.connect();
         
            InputStream is = conn.getInputStream();
            result = BitmapFactory.decodeStream(is);
                  
          } catch (MalformedURLException e) {
            e.printStackTrace();
          } catch (Exception e) {
            e.printStackTrace();
          } 
 
        return result;
    }
}
