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

package co.carlosjimenez.android.currencyalerts.app.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.format.Time;

/**
 * Defines table and column names for the forex database.
 */
public class ForexContract {

    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.  A convenient string to use for the
    // content authority is the package name for the app, which is guaranteed to be unique on the
    // device.
    public static final String CONTENT_AUTHORITY = "co.carlosjimenez.android.currencyalerts.app";

    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible paths (appended to base content URI for possible URI's)
    // For instance, content://co.carlosjimenez.android.currencyalerts.app/currency/ is a valid path for
    // looking at currency data. content://co.carlosjimenez.android.currencyalerts.app/givemeroot/ will fail,
    // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
    // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.
    public static final String PATH_CURRENCY = "currency";
    public static final String PATH_RATE = "rate";

    public static final String[] RATE_CURRENCY_COLUMNS = {
            ForexContract.RateEntry.TABLE_NAME + "." + ForexContract.RateEntry._ID,
            ForexContract.RateEntry.COLUMN_RATE_FROM_KEY,
            ForexContract.RateEntry.JOIN_RATE_FROM_ALIAS + "." + ForexContract.CurrencyEntry.COLUMN_CURRENCY_NAME,
            ForexContract.RateEntry.JOIN_RATE_FROM_ALIAS + "." + ForexContract.CurrencyEntry.COLUMN_CURRENCY_SYMBOL,
            ForexContract.RateEntry.JOIN_RATE_FROM_ALIAS + "." + ForexContract.CurrencyEntry.COLUMN_COUNTRY_CODE,
            ForexContract.RateEntry.JOIN_RATE_FROM_ALIAS + "." + ForexContract.CurrencyEntry.COLUMN_COUNTRY_NAME,
            ForexContract.RateEntry.JOIN_RATE_FROM_ALIAS + "." + ForexContract.CurrencyEntry.COLUMN_COUNTRY_FLAG_URL,
            ForexContract.RateEntry.COLUMN_RATE_TO_KEY,
            ForexContract.RateEntry.JOIN_RATE_TO_ALIAS + "." + ForexContract.CurrencyEntry.COLUMN_CURRENCY_NAME,
            ForexContract.RateEntry.JOIN_RATE_TO_ALIAS + "." + ForexContract.CurrencyEntry.COLUMN_CURRENCY_SYMBOL,
            ForexContract.RateEntry.JOIN_RATE_TO_ALIAS + "." + ForexContract.CurrencyEntry.COLUMN_COUNTRY_CODE,
            ForexContract.RateEntry.JOIN_RATE_TO_ALIAS + "." + ForexContract.CurrencyEntry.COLUMN_COUNTRY_NAME,
            ForexContract.RateEntry.JOIN_RATE_TO_ALIAS + "." + ForexContract.CurrencyEntry.COLUMN_COUNTRY_FLAG_URL,
            ForexContract.RateEntry.COLUMN_RATE_DATE,
            ForexContract.RateEntry.COLUMN_RATE_VALUE
    };

    // To make it easy to query for the exact date, we normalize all dates that go into
    // the database to the start of the the Julian day at UTC.
    public static long normalizeDate(long startDate) {
        // normalize the start date to the beginning of the (UTC) day
        Time time = new Time();
        time.set(startDate);
        int julianDay = Time.getJulianDay(startDate, time.gmtoff);
        return time.setJulianDay(julianDay);
    }

    /* Inner class that defines the table contents of the currency table */
    public static final class CurrencyEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_CURRENCY).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_CURRENCY;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_CURRENCY;

        // Table name
        public static final String TABLE_NAME = "currency";

        public static final String COLUMN_CURRENCY_ID = "currency_id";
        public static final String COLUMN_CURRENCY_NAME = "currency_name";
        public static final String COLUMN_CURRENCY_SYMBOL = "currency_symbol";
        public static final String COLUMN_COUNTRY_NAME = "country_name";
        public static final String COLUMN_COUNTRY_CODE = "country_code";
        public static final String COLUMN_COUNTRY_FLAG_URL = "country_flag_url";

        public static Uri buildCurrencyUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    /* Inner class that defines the table contents of the rates table */
    public static final class RateEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_RATE).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_RATE;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_RATE;

        public static final String TABLE_NAME = "rate";

        public static final String JOIN_RATE_FROM_ALIAS = "currency_from";
        public static final String JOIN_RATE_TO_ALIAS = "currency_to";

        public static final String COLUMN_RATE_FROM_KEY = "currency_from_id";
        public static final String COLUMN_RATE_TO_KEY = "currency_to_id";
        // Date, stored as long in milliseconds since the epoch
        public static final String COLUMN_RATE_DATE = "date";
        // Rate value as returned by API
        public static final String COLUMN_RATE_VALUE = "value";

        public static final String[] RATE_COLUMNS = {
                ForexContract.RateEntry.TABLE_NAME + "." + ForexContract.RateEntry._ID,
                ForexContract.RateEntry.COLUMN_RATE_FROM_KEY,
                ForexContract.RateEntry.COLUMN_RATE_TO_KEY,
                ForexContract.RateEntry.COLUMN_RATE_DATE,
                ForexContract.RateEntry.COLUMN_RATE_VALUE
        };

        public static Uri buildRateUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildCurrencyRate(String[] currencies) {
            return CONTENT_URI.buildUpon()
                    .appendPath(currencies[0])
                    .appendPath(currencies[1])
                    .build();
        }

        public static Uri buildCurrencyRateWithValue(String[] currencies, double value) {
            return CONTENT_URI.buildUpon()
                    .appendPath(currencies[0])
                    .appendPath(currencies[1])
                    .appendQueryParameter(COLUMN_RATE_VALUE, Double.toString(value))
                    .build();
        }

        public static Uri buildStartCurrencyRate(String[] currencies) {
            return CONTENT_URI.buildUpon()
                    .appendPath(currencies[0])
                    .build();
        }

        public static Uri buildCurrencyRateWithStartDate(
                String[] currencies, long startDate) {
            long normalizedDate = normalizeDate(startDate);
            return CONTENT_URI.buildUpon()
                    .appendPath(currencies[0])
                    .appendPath(currencies[1])
                    .appendQueryParameter(COLUMN_RATE_DATE, Long.toString(normalizedDate))
                    .build();
        }

        public static Uri buildStartCurrencyWithDate(
                String currency, long startDate) {
            long normalizedDate = normalizeDate(startDate);
            return CONTENT_URI.buildUpon()
                    .appendPath(currency)
                    .appendQueryParameter(COLUMN_RATE_DATE, Long.toString(normalizedDate))
                    .build();
        }

        public static Uri buildCurrencyRateWithDate(String[] currencies, long date) {
            return CONTENT_URI.buildUpon()
                    .appendPath(currencies[0])
                    .appendPath(currencies[1])
                    .appendPath(Long.toString(normalizeDate(date)))
                    .build();
        }

        public static String getCurrencyFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String[] getCurrenciesFromUri(Uri uri) {
            String[] currencies = new String[2];
            currencies[0] = uri.getPathSegments().get(1);
            currencies[1] = uri.getPathSegments().get(2);
            return currencies;
        }

        public static long getDateFromUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(3));
        }

        public static long getStartDateFromUri(Uri uri) {
            String dateString = uri.getQueryParameter(COLUMN_RATE_DATE);
            if (null != dateString && dateString.length() > 0)
                return Long.parseLong(dateString);
            else
                return 0;
        }

        public static double getRateFromUri(Uri uri) {
            String rateString = uri.getQueryParameter(COLUMN_RATE_VALUE);
            if (null != rateString && rateString.length() > 0)
                return Double.parseDouble(rateString);
            else
                return 1;
        }
    }
}
