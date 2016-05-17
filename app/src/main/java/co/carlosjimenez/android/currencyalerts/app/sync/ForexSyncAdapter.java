/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Carlos Andres Jimenez <apps@carlosandresjimenez.co>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package co.carlosjimenez.android.currencyalerts.app.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.text.format.Time;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

import co.carlosjimenez.android.currencyalerts.app.R;
import co.carlosjimenez.android.currencyalerts.app.data.ForexContract;

public class ForexSyncAdapter extends AbstractThreadedSyncAdapter {
    public static final String ACTION_DATA_UPDATED =
            "co.carlosjimenez.android.currencyalerts.app.ACTION_DATA_UPDATED";
    public static final int FOREX_DAYS_TO_KEEP = 40;
    // Interval at which to sync with the weather, in seconds.
    // 60 seconds (1 minute) * 180 = 3 hours
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;
    public static final int FOREX_STATUS_OK = 0;
    public static final int FOREX_STATUS_SERVER_DOWN = 1;
    public static final int FOREX_STATUS_SERVER_INVALID = 2;

//    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[]{
//            ForexContract.RateEntry.COLUMN_WEATHER_ID,
//            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
//            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
//            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
//    };

    // these indices must match the projection
//    private static final int INDEX_WEATHER_ID = 0;
//    private static final int INDEX_MAX_TEMP = 1;
//    private static final int INDEX_MIN_TEMP = 2;
//    private static final int INDEX_SHORT_DESC = 3;
    public static final int FOREX_STATUS_UNKNOWN = 3;
    public static final int FOREX_STATUS_INVALID = 4;
    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int WEATHER_NOTIFICATION_ID = 3004;
    public final String LOG_TAG = ForexSyncAdapter.class.getSimpleName();

    public ForexSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

//    private GoogleApiClient mGoogleApiClient;
//    private static final String WEAR_MAX_TEMP_KEY = "com.example.android.sunshine.max_temp";
//    private static final String WEAR_MIN_TEMP_KEY = "com.example.android.sunshine.min_temp";
//    private static final String WEAR_WEATHER_ID_KEY = "com.example.android.sunshine.weather_id";

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    /**
     * Helper method to have the sync adapter sync immediately
     *
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if (null == accountManager.getPassword(newAccount)) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

//    private void updateWidgets() {
//        Context context = getContext();
//        // Setting the package ensures that only components in our app will receive the broadcast
//        Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED)
//                .setPackage(context.getPackageName());
//        context.sendBroadcast(dataUpdatedIntent);
//    }
//
//    private void updateMuzei() {
//        // Muzei is only compatible with Jelly Bean MR1+ devices, so there's no need to update the
//        // Muzei background on lower API level devices
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//            Context context = getContext();
//            context.startService(new Intent(ACTION_DATA_UPDATED)
//                    .setClass(context, WeatherMuzeiSource.class));
//        }
//    }
//
//    private void notifyWeather() {
//        Context context = getContext();
//        //checking the last update and notify if it' the first of the day
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
//        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
//        boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,
//                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));
//
//        if (displayNotifications) {
//
//            String lastNotificationKey = context.getString(R.string.pref_last_notification);
//            long lastSync = prefs.getLong(lastNotificationKey, 0);
//
//            if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {
//                // Last sync was more than 1 day ago, let's send a notification with the weather.
//                String locationQuery = Utility.getPreferredLocation(context);
//
//                Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery, System.currentTimeMillis());
//
//                // we'll query our contentProvider, as always
//                Cursor cursor = context.getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);
//
//                if (cursor.moveToFirst()) {
//                    int weatherId = cursor.getInt(INDEX_WEATHER_ID);
//                    double high = cursor.getDouble(INDEX_MAX_TEMP);
//                    double low = cursor.getDouble(INDEX_MIN_TEMP);
//                    String desc = cursor.getString(INDEX_SHORT_DESC);
//
//                    int iconId = Utility.getIconResourceForWeatherCondition(weatherId);
//                    Resources resources = context.getResources();
//                    int artResourceId = Utility.getArtResourceForWeatherCondition(weatherId);
//                    String artUrl = Utility.getArtUrlForWeatherCondition(context, weatherId);
//
//                    // On Honeycomb and higher devices, we can retrieve the size of the large icon
//                    // Prior to that, we use a fixed size
//                    @SuppressLint("InlinedApi")
//                    int largeIconWidth = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
//                            ? resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
//                            : resources.getDimensionPixelSize(R.dimen.notification_large_icon_default);
//                    @SuppressLint("InlinedApi")
//                    int largeIconHeight = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
//                            ? resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height)
//                            : resources.getDimensionPixelSize(R.dimen.notification_large_icon_default);
//
//                    // Retrieve the large icon
//                    Bitmap largeIcon;
//                    try {
//                        largeIcon = Glide.with(context)
//                                .load(artUrl)
//                                .asBitmap()
//                                .error(artResourceId)
//                                .fitCenter()
//                                .into(largeIconWidth, largeIconHeight).get();
//                    } catch (InterruptedException | ExecutionException e) {
//                        Log.e(LOG_TAG, "Error retrieving large icon from " + artUrl, e);
//                        largeIcon = BitmapFactory.decodeResource(resources, artResourceId);
//                    }
//                    String title = context.getString(R.string.app_name);
//
//                    // Define the text of the forecast.
//                    String contentText = String.format(context.getString(R.string.format_notification),
//                            desc,
//                            Utility.formatTemperature(context, high),
//                            Utility.formatTemperature(context, low));
//
//                    // NotificationCompatBuilder is a very convenient way to build backward-compatible
//                    // notifications.  Just throw in some data.
//                    NotificationCompat.Builder mBuilder =
//                            new NotificationCompat.Builder(getContext())
//                                    .setColor(resources.getColor(R.color.primary_light))
//                                    .setSmallIcon(iconId)
//                                    .setLargeIcon(largeIcon)
//                                    .setContentTitle(title)
//                                    .setContentText(contentText);
//
//                    // Make something interesting happen when the user clicks on the notification.
//                    // In this case, opening the app is sufficient.
//                    Intent resultIntent = new Intent(context, MainActivity.class);
//
//                    // The stack builder object will contain an artificial back stack for the
//                    // started Activity.
//                    // This ensures that navigating backward from the Activity leads out of
//                    // your application to the Home screen.
//                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
//                    stackBuilder.addNextIntent(resultIntent);
//                    PendingIntent resultPendingIntent =
//                            stackBuilder.getPendingIntent(
//                                    0,
//                                    PendingIntent.FLAG_UPDATE_CURRENT
//                            );
//                    mBuilder.setContentIntent(resultPendingIntent);
//
//                    NotificationManager mNotificationManager =
//                            (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
//                    // WEATHER_NOTIFICATION_ID allows you to update the notification later on.
//                    mNotificationManager.notify(WEATHER_NOTIFICATION_ID, mBuilder.build());
//
//                    //refreshing last sync
//                    SharedPreferences.Editor editor = prefs.edit();
//                    editor.putLong(lastNotificationKey, System.currentTimeMillis());
//                    editor.commit();
//                }
//                cursor.close();
//            }
//        }
//    }

    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        ForexSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }

    /**
     * Sets the forex status into shared preference.  This function should not be called from
     * the UI thread because it uses commit to write to the shared preferences.
     *
     * @param c           Context to get the PreferenceManager from.
     * @param forexStatus The IntDef value to set
     */
    static private void setForexStatus(Context c, @ForexStatus int forexStatus) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor spe = sp.edit();
        spe.putInt(c.getString(R.string.pref_forex_status_key), forexStatus);
        spe.commit();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(LOG_TAG, "Starting sync");

        String[] currencies = {"USD_ZAR", "USD_COP"};

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forexJsonStr = null;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            String sUrl = "";
            String baseUrl = "http://free.currencyconverterapi.com/api/v3/convert?q=";
            //String apiKey = "&access_key=" + BuildConfig.CURRENCY_LAYER_API_KEY;

            for (int i = 0; i < currencies.length; i++) {
                if (i == 0)
                    sUrl = baseUrl.concat(currencies[i]);
                else
                    sUrl = sUrl.concat("," + currencies[i]);
            }

            URL url = new URL(sUrl);

            Log.v(LOG_TAG, "Forex url: " + sUrl);

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                setForexStatus(getContext(), FOREX_STATUS_SERVER_DOWN);
                return;
            }
            forexJsonStr = buffer.toString();
            getForexDataFromJson(forexJsonStr, currencies);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the forex data, there's no point in attemping
            // to parse it.
            setForexStatus(getContext(), FOREX_STATUS_SERVER_DOWN);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
            setForexStatus(getContext(), FOREX_STATUS_SERVER_INVALID);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        // This will only happen if there was an error getting or parsing the forecast.
        return;
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     * <p/>
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private void getForexDataFromJson(String forexJsonStr, String[] currencies)
            throws JSONException {

        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.

        // These are the names of the JSON objects that need to be extracted.
        final String OWM_RESULT = "results";
        final String OWM_RATE_FROM = "fr";
        final String OWM_RATE_TO = "to";
        final String OWM_RATE = "val";

        try {
            JSONObject forexJson = new JSONObject(forexJsonStr);

            // do we have an error?
            if (!forexJson.has(OWM_RESULT)) {
                setForexStatus(getContext(), FOREX_STATUS_INVALID);
                return;
            }

            JSONObject forexResultObject = forexJson.getJSONObject(OWM_RESULT);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianDate = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();
            long dateTime = dayTime.setJulianDay(julianDate);

            // Insert the new weather information into the database
            Vector<ContentValues> cVVector = new Vector<>(currencies.length);

            for (int i = 0; i < currencies.length; i++) {

                JSONObject currencyObject = forexResultObject.getJSONObject(currencies[i]);
                String rate_from = currencyObject.getString(OWM_RATE_FROM);
                String rate_to = currencyObject.getString(OWM_RATE_TO);
                double result = currencyObject.getDouble(OWM_RATE);

                ContentValues forexValues = new ContentValues();

                forexValues.put(ForexContract.RateEntry.COLUMN_RATE_FROM_KEY, rate_from);
                forexValues.put(ForexContract.RateEntry.COLUMN_RATE_TO_KEY, rate_to);
                forexValues.put(ForexContract.RateEntry.COLUMN_RATE_DATE, dateTime);
                forexValues.put(ForexContract.RateEntry.COLUMN_RATE_VALUE, result);

                Log.v(LOG_TAG, "Forex entry: 1 " + rate_from + " -> " + rate_to + " = " + result);

                cVVector.add(forexValues);
            }

            int inserted = 0;
            // add to database
            if (cVVector.size() > 0) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                getContext().getContentResolver().bulkInsert(ForexContract.RateEntry.CONTENT_URI, cvArray);

                // delete old data so we don't build up an endless history
                getContext().getContentResolver().delete(ForexContract.RateEntry.CONTENT_URI,
                        ForexContract.RateEntry.COLUMN_RATE_DATE + " <= ?",
                        new String[]{Long.toString(dayTime.setJulianDay(julianDate - FOREX_DAYS_TO_KEEP))});

//                updateWidgets();
            }

            Log.d(LOG_TAG, "Sync Complete. " + cVVector.size() + " Inserted");
            setForexStatus(getContext(), FOREX_STATUS_OK);

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
            setForexStatus(getContext(), FOREX_STATUS_SERVER_INVALID);
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FOREX_STATUS_OK, FOREX_STATUS_SERVER_DOWN, FOREX_STATUS_SERVER_INVALID, FOREX_STATUS_UNKNOWN, FOREX_STATUS_INVALID})
    public @interface ForexStatus {
    }


//    private void sendToWear() {
//        Context context = getContext();
//
//        mGoogleApiClient = new GoogleApiClient.Builder(context)
//                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
//                    @Override
//                    public void onConnected(Bundle connectionHint) {
//                        Log.d(LOG_TAG, "onConnected: " + connectionHint);
//                        // Now you can use the Data Layer API
//                        updateWearData();
//                    }
//
//                    @Override
//                    public void onConnectionSuspended(int cause) {
//                        Log.d(LOG_TAG, "onConnectionSuspended: " + cause);
//                    }
//                })
//                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
//                    @Override
//                    public void onConnectionFailed(ConnectionResult result) {
//                        Log.d(LOG_TAG, "onConnectionFailed: " + result);
//                    }
//                })
//                // Request access only to the Wearable API
//                .addApi(Wearable.API)
//                .build();
//
//        mGoogleApiClient.connect();
//    }
//
//    private void updateWearData() {
//
//        if (!mGoogleApiClient.isConnected()) {
//            Log.e(LOG_TAG, "Google Api not connected, cannot send data to wear");
//            return;
//        }
//
//        Context context = getContext();
//
//        // Last sync was more than 1 day ago, let's send a notification with the weather.
//        String locationQuery = Utility.getPreferredLocation(context);
//
//        Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery, System.currentTimeMillis());
//
//        // we'll query our contentProvider, as always
//        Cursor cursor = context.getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);
//
//        int weatherId = 0;
//        double maxTemp = 0;
//        double minTemp = 0;
//        if (cursor.moveToFirst()) {
//            weatherId = cursor.getInt(INDEX_WEATHER_ID);
//            maxTemp = cursor.getDouble(INDEX_MAX_TEMP);
//            minTemp = cursor.getDouble(INDEX_MIN_TEMP);
//        } else {
//            cursor.close();
//            return;
//        }
//        cursor.close();
//
//        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/sunshine");
//        putDataMapReq.getDataMap().putDouble(WEAR_MAX_TEMP_KEY, maxTemp);
//        putDataMapReq.getDataMap().putDouble(WEAR_MIN_TEMP_KEY, minTemp);
//        putDataMapReq.getDataMap().putInt(WEAR_WEATHER_ID_KEY, weatherId);
//        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
//        PendingResult<DataApi.DataItemResult> pendingResult =
//                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
//
//        mGoogleApiClient.disconnect();
//    }
}