package com.knoxmobilitylab.weatherexplorer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private static String API_BASE = "http://api.wunderground.com/api/";
	private static String ICON_BASE = "http://icons.wxug.com/i/c/h/";
	private static String CONDITIONS = "/conditions/q/";
	private String uvIndex;
	
	String credits;

	TextView textViewTemp;
	TextView textViewLocationValue;
	TextView textViewFeelsLikeValue;
	TextView textViewWeather;
	TextView textViewWinds;
	TextView textViewRelativeHumidity;
	TextView textViewBarometricPressure;
	TextView textViewDewpoint;
	TextView textViewVisibility;
	TextView textViewObservationTimeValue;
	EditText editTextZip;
	ImageView imageViewWeatherIcon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
    	textViewTemp = (TextView) findViewById(R.id.textViewTemp);
        textViewLocationValue = (TextView) findViewById(R.id.textViewLocation);
    	textViewFeelsLikeValue = (TextView) findViewById(R.id.textViewFeelsLike);
    	textViewWeather = (TextView) findViewById(R.id.textViewWeather);
    	textViewWinds = (TextView) findViewById(R.id.textViewWinds);
    	textViewRelativeHumidity = (TextView) findViewById(R.id.textViewRelativeHumidity);
    	textViewBarometricPressure = (TextView) findViewById(R.id.textViewBarometricPressure);
    	textViewDewpoint = (TextView) findViewById(R.id.textViewDewpoint);
    	textViewVisibility = (TextView) findViewById(R.id.textViewVisibility);
    	textViewObservationTimeValue = (TextView) findViewById(R.id.textViewObservationTime);
    	editTextZip = (EditText)findViewById(R.id.editTextZip);
		imageViewWeatherIcon = (ImageView) findViewById(R.id.imageViewWeatherIcon);
    	
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    //Retrieve current weather conditions & update display
    public void getWeather(View view) {
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
        // call AsyncTask to perform network operation on separate thread
        new WeatherDataAsyncTask().execute(sb.toString(), "conditions");
        
    }    	

    /* UTILITY METHODS */
    
    //check to see if connected to a network
    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) 
                return true;
            else
                return false;   
    }
 
    //static method to retrieve weather data from the weather API
    public static String getWeatherData(String url){

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
 
    //static method to pull images from a URL
    public static Bitmap getWeatherIcon(String url){
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
 

    /* ASYNC PRIVATE CLASSES */
 
    //private async class to fetch weather data
    private class WeatherDataAsyncTask extends AsyncTask<String, Void, String> {

    	String weatherDataType = null;
    	
    	@Override
    	protected String doInBackground(String... params) {
    		String weatherData = null;
    		String weatherApiUrl = params[0];
    		 
    		//save the type of data (conditions, forecast, etc.) this request is retrieving
    		weatherDataType = params[1];

    		weatherData = getWeatherData(weatherApiUrl);
    		
    		return weatherData;
    	}
    	
    	// onPostExecute displays the results of the AsyncTask.
    	@Override
    	protected void onPostExecute(String result) {

    		Toast.makeText(getBaseContext(), "Received Current Weather!", Toast.LENGTH_LONG).show();

    		try {
        		// convert String to JSONObject
    			JSONObject jsonResult = new JSONObject(result);
    			
    			//process the result data
    			if (weatherDataType.equals("conditions")) {
    				setCurrentConditions(jsonResult);
    			}
    		} catch (JSONException e) {
    			e.printStackTrace();
    		} 

    	}
    	
    	protected void setCurrentConditions(JSONObject jsonResult) {
    		try {
    			JSONObject currentObservation = jsonResult.getJSONObject("current_observation"); // get current_observation
    			JSONObject displayLocation = currentObservation.getJSONObject("display_location");

    			//Display the temperature
     			textViewTemp.setText(currentObservation.getString("temp_f")+" ºF");

    			//Display the location
    			textViewLocationValue.setText(displayLocation.getString("full"));

    			//Display the feels like temperature
    			textViewFeelsLikeValue.setText(currentObservation.getString("feelslike_f")+" ºF");

    			//Display the weather
    			textViewWeather.setText(currentObservation.getString("weather"));

    			//Display the wind
    			textViewWinds.setText(currentObservation.getString("wind_string"));

    			//Display the relative humidity
    			textViewRelativeHumidity.setText(currentObservation.getString("relative_humidity"));

    			//Display the barometric pressure
    			textViewBarometricPressure.setText(currentObservation.getString("pressure_in")+" in Hg");

    			//Display the dewpoint
    			textViewDewpoint.setText(currentObservation.getString("dewpoint_f")+" ºF");

    			//Display the visibility
    			textViewVisibility.setText(currentObservation.getString("visibility_mi")+" miles");

    			//Display the observation time
    			textViewObservationTimeValue.setText(currentObservation.getString("observation_time"));

    			//build the URL for the current conditions icon
    			StringBuilder iconUrl = new StringBuilder(ICON_BASE).append(currentObservation.getString("icon")).append(".gif");

    			//call AsyncTask to set the current conditions icon if we have one
    			if (iconUrl.toString() != null) {
    				new WeatherConditionsIconAsyncTask().execute(iconUrl.toString());
    			}
    		} catch (JSONException e) {
    			e.printStackTrace();
    		} 

    	}
    }

    //private async class to fetch weather data
    private class WeatherConditionsIconAsyncTask extends AsyncTask<String, Void, Bitmap> {
    	@Override
    	protected Bitmap doInBackground(String... urls) {

    		Log.d("WeatherExplorer", "Starting background fetch of icon...");
    		return getWeatherIcon(urls[0]);
    	}
    	
    	// onPostExecute displays the results of the AsyncTask.
    	@Override
    	protected void onPostExecute(Bitmap bm) {
    		if (bm != null) {
        		Log.d("WeatherExplorer", "Setting fetched icon.");
    			//retrieve the imageView and set the icon
    			imageViewWeatherIcon.setImageBitmap(bm);
    		}
    	}
   
    }
}

	