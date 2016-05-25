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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.v4.content.LocalBroadcastManager;
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
import co.carlosjimenez.android.currencyalerts.app.Utility;
import co.carlosjimenez.android.currencyalerts.app.data.Alert;
import co.carlosjimenez.android.currencyalerts.app.data.ForexContract;

public class ForexSyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String LOG_TAG = ForexSyncAdapter.class.getSimpleName();

    public static final String ACTION_DATA_UPDATED =
            "co.carlosjimenez.android.currencyalerts.app.ACTION_DATA_UPDATED";
    public static final String FOREX_DATA_STATUS = "FOREX_DATA_STATUS";

    public static final int FOREX_DAYS_TO_KEEP = 40;

    public static final int FOREX_STATUS_OK = 0;
    public static final int FOREX_STATUS_SERVER_DOWN = 1;
    public static final int FOREX_STATUS_SERVER_INVALID = 2;

    public static final int FOREX_STATUS_UNKNOWN = 3;
    public static final int FOREX_STATUS_INVALID = 4;

    private Alert mAlertData;
    private double mCurrentAlertRate;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FOREX_STATUS_OK, FOREX_STATUS_SERVER_DOWN, FOREX_STATUS_SERVER_INVALID, FOREX_STATUS_UNKNOWN, FOREX_STATUS_INVALID})
    public @interface ForexStatus {
    }

    public ForexSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

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

    private static void onAccountCreated(Account newAccount, Context context) {
        // Interval at which to sync with the rates, in seconds.
        // 60 seconds (1 minute) * 180 = 3 hours
        int syncFrequency = Utility.getSyncFrequency(context);

        if (syncFrequency != -1) {
            int syncInterval = 60 * syncFrequency;
            int syncFlextime = syncInterval / 3;

            ForexSyncAdapter.configurePeriodicSync(context, syncInterval, syncFlextime);
        }

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

    /**
     * Sets the forex sync date into shared preference.  This function should not be called from
     * the UI thread because it uses commit to write to the shared preferences.
     *
     * @param c             Context to get the PreferenceManager from.
     * @param forexSyncDate The IntDef value to set
     */
    static private void setForexSyncDate(Context c, long forexSyncDate) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor spe = sp.edit();
        spe.putLong(c.getString(R.string.pref_forex_sync_date_key), forexSyncDate);
        spe.commit();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String currencies = "";

        // Will contain the raw JSON response as a string.
        String forexJsonStr = null;

        try {
            mAlertData = Utility.getAlertSettings(getContext(), false);

            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are available at currencyconverterapi API page.
            final String FOREX_BASE_URL = "http://free.currencyconverterapi.com/api/v3/convert?";
            final String QUERY_PARAM = "q";
            String currencyQueryStr = Utility.getSyncCurrencies(getContext());

            if (currencyQueryStr.isEmpty()) {
                Log.d(LOG_TAG, "No currencies to sync");
                return;
            }

            Uri currencyUri = Uri.parse(FOREX_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, currencyQueryStr)
                    .build();

            URL url = new URL(currencyUri.toString());

            // Create the request to CurrencyConverterAPI, and open the connection
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
            getForexDataFromJson(forexJsonStr, currencyQueryStr);
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

        // This will only happen if there was an error getting or parsing the forex.
        return;
    }

    /**
     * Take the String representing the complete forex in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     * <p/>
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private void getForexDataFromJson(String forexJsonStr, String currencyQuery)
            throws JSONException {

        // Now we have a String representing the complete forex in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.

        // These are the names of the JSON objects that need to be extracted.
        final String OWM_RESULT = "results";
        final String OWM_RATE_FROM = "fr";
        final String OWM_RATE_TO = "to";
        final String OWM_RATE = "val";
        String[] currencies;
        boolean alertAvailable = false;

        try {
            if (mAlertData != null && mAlertData.getCurrencyFrom() != null && mAlertData.getCurrencyTo() != null) {
                alertAvailable = true;
            }

            JSONObject forexJson = new JSONObject(forexJsonStr);

            // do we have an error?
            if (!forexJson.has(OWM_RESULT)) {
                setForexStatus(getContext(), FOREX_STATUS_INVALID);
                return;
            }

            currencies = currencyQuery.split(",");

            JSONObject forexResultObject = forexJson.getJSONObject(OWM_RESULT);

            // OWM returns daily rates based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our rates.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianDate = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();
            long dateTime = dayTime.setJulianDay(julianDate);

            // Insert the new rates information into the database
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

                if (alertAvailable &&
                        mAlertData.getCurrencyFrom().getId().equals(rate_from) &&
                        mAlertData.getCurrencyTo().getId().equals(rate_to)) {
                    mCurrentAlertRate = result;
                }

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

                setForexSyncDate(getContext(), System.currentTimeMillis());
                sendSyncBroadcast(FOREX_STATUS_OK);
                checkCurrencyData();
            }

            Log.d(LOG_TAG, "ForexSyncAdapter: Sync Complete. " + cVVector.size() + " Inserted");
            setForexStatus(getContext(), FOREX_STATUS_OK);

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
            setForexStatus(getContext(), FOREX_STATUS_SERVER_INVALID);
        }
    }

    /**
     * Send sync broadcast so App and Widget can update the rate values immediately.
     *
     * @param status Status of the rate sync
     */
    private void sendSyncBroadcast(int status) {
        Context context = getContext();

        // Setting the package ensures that only components in our app will receive the broadcast
        Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED)
                .setPackage(context.getPackageName())
                .putExtra(FOREX_DATA_STATUS, status);
        LocalBroadcastManager.getInstance(context).sendBroadcast(dataUpdatedIntent);
    }

    /**
     * This method request the Alert Service to run and validate the data to see if it needs
     * to alert the user about any currency rise or fall.
     */
    private void checkCurrencyData() {
        AlertService.startAlertService(getContext(), mCurrentAlertRate);
    }

}