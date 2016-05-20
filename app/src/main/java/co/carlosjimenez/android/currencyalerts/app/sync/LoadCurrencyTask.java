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

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Vector;

import co.carlosjimenez.android.currencyalerts.app.R;
import co.carlosjimenez.android.currencyalerts.app.Utility;
import co.carlosjimenez.android.currencyalerts.app.data.Currency;
import co.carlosjimenez.android.currencyalerts.app.data.ForexContract;

public class LoadCurrencyTask extends AsyncTask<Void, Void, Integer> {

    private static final String LOG_TAG = LoadCurrencyTask.class.getSimpleName();

    public static final int CURRENCY_STATUS_OK = 0;
    public static final int CURRENCY_STATUS_UNKNOWN = 1;
    public static final int CURRENCY_STATUS_INVALID = 2;

    private final Context mContext;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CURRENCY_STATUS_OK, CURRENCY_STATUS_UNKNOWN, CURRENCY_STATUS_INVALID})
    public @interface CurrencyStatus {
    }

    public LoadCurrencyTask(Context context) {
        mContext = context;
    }

    /**
     * Sets the forex status into shared preference.  This function should not be called from
     * the UI thread because it uses commit to write to the shared preferences.
     *
     * @param c              Context to get the PreferenceManager from.
     * @param currencyStatus The value to set
     */
    static private void setCurrencyStatus(Context c, @CurrencyStatus int currencyStatus) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor spe = sp.edit();
        spe.putInt(c.getString(R.string.pref_currency_status_key), currencyStatus);
        spe.commit();
    }

    /**
     * Sets the forex status into shared preference.  This function should not be called from
     * the UI thread because it uses commit to write to the shared preferences.
     *
     * @param c              Context to get the PreferenceManager from.
     * @param currencies     The value to set
     */
    static private void setSyncCurrencies(Context c, String currencies) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor spe = sp.edit();
        spe.putString(c.getString(R.string.pref_displayed_currencies_key), currencies);
        spe.commit();
    }

    @Override
    protected Integer doInBackground(Void... params) {

        // Stop the task if the currencies have already been loaded.
        if (Utility.getCurrencyStatus(mContext) == CURRENCY_STATUS_OK) {
            Log.d(LOG_TAG, "Currencies already in DB.");
            return 0;
        }

        Vector<ContentValues> cVVector = getCurrencies();
        long currencyRowId;
        int i = 0;
        String currencies = "";
        String currencyId = "";
        Currency mainCurrency = Utility.getMainCurrency(mContext);

        try {
            for (ContentValues currencyValue : cVVector) {

                currencyId = currencyValue.getAsString(ForexContract.CurrencyEntry.COLUMN_CURRENCY_ID);
                if (!currencyId.equals(mainCurrency.getId())) {
                    if (currencies.length() > 1)
                        currencies = currencies + "," + mainCurrency.getId() + "_" + currencyId;
                    else
                        currencies = mainCurrency.getId() + "_" + currencyId;
                }

                currencyRowId = addCurrency(currencyValue);
                i++;
            }
        } catch (Exception e) {
            setCurrencyStatus(mContext, CURRENCY_STATUS_INVALID);
        }

        setCurrencyStatus(mContext, CURRENCY_STATUS_OK);
        setSyncCurrencies(mContext, currencies);

        Log.d(LOG_TAG, "Inserted currencies : " + i);
        ForexSyncAdapter.syncImmediately(mContext);

        return i;
    }

    /**
     * Helper method to handle insertion of a new currency in the forex database.
     *
     * @param currencyValue Currency information to be stored.
     * @return the row ID of the added location.
     */
    long addCurrency(ContentValues currencyValue) {
        long currencyRowId;
        String currencyId = currencyValue.getAsString(ForexContract.CurrencyEntry.COLUMN_CURRENCY_ID);

        // First, check if the location with this city name exists in the db
        Cursor currencyCursor = mContext.getContentResolver().query(
                ForexContract.CurrencyEntry.CONTENT_URI,
                new String[]{ForexContract.CurrencyEntry._ID},
                ForexContract.CurrencyEntry.COLUMN_CURRENCY_ID + " = ?",
                new String[]{currencyId},
                null);

        if (currencyCursor.moveToFirst()) {
            int currencyIdIndex = currencyCursor.getColumnIndex(ForexContract.CurrencyEntry._ID);
            currencyRowId = currencyCursor.getLong(currencyIdIndex);

            int updatedRows = mContext.getContentResolver().update(
                    ForexContract.CurrencyEntry.CONTENT_URI,
                    currencyValue,
                    ForexContract.CurrencyEntry._ID + " = ?",
                    new String[]{String.valueOf(currencyRowId)}
            );

        } else {
            // Now that the content provider is set up, inserting rows of data is pretty simple.
            // Insert currency data into the database
            Uri insertedUri = mContext.getContentResolver().insert(
                    ForexContract.CurrencyEntry.CONTENT_URI,
                    currencyValue
            );

            // The resulting URI contains the ID for the row.  Extract the locationId from the Uri.
            currencyRowId = ContentUris.parseId(insertedUri);
        }

        currencyCursor.close();
        // Wait, that worked?  Yes!
        return currencyRowId;
    }

    Vector<ContentValues> getCurrencies() {

        Vector<ContentValues> cVVector = new Vector<>();
        ContentValues currencyValues;

        TypedArray country_code = mContext.getResources().obtainTypedArray(R.array.country_code);
        TypedArray country_name = mContext.getResources().obtainTypedArray(R.array.country_name);
        TypedArray country_flag_url = mContext.getResources().obtainTypedArray(R.array.country_flag_url);
        TypedArray currency_id = mContext.getResources().obtainTypedArray(R.array.currency_id);
        TypedArray currency_name = mContext.getResources().obtainTypedArray(R.array.currency_name);
        TypedArray currency_symbol = mContext.getResources().obtainTypedArray(R.array.currency_symbol);

        for (int i = 0; i < country_code.length(); i++) {
            currencyValues = new ContentValues();
            currencyValues.put(ForexContract.CurrencyEntry.COLUMN_CURRENCY_ID, currency_id.getString(i));
            currencyValues.put(ForexContract.CurrencyEntry.COLUMN_CURRENCY_SYMBOL, currency_symbol.getString(i));
            currencyValues.put(ForexContract.CurrencyEntry.COLUMN_CURRENCY_NAME, currency_name.getString(i));
            currencyValues.put(ForexContract.CurrencyEntry.COLUMN_COUNTRY_CODE, country_code.getString(i));
            currencyValues.put(ForexContract.CurrencyEntry.COLUMN_COUNTRY_NAME, country_name.getString(i));
            currencyValues.put(ForexContract.CurrencyEntry.COLUMN_COUNTRY_FLAG_URL, country_flag_url.getString(i));
            cVVector.add(currencyValues);
        }

        return cVVector;
    }
}
