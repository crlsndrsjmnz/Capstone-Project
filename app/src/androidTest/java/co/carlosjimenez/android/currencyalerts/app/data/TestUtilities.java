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

import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.test.AndroidTestCase;

import java.util.Map;
import java.util.Set;

import co.carlosjimenez.android.currencyalerts.app.utils.PollingCheck;

/*
    Students: These are functions and some test data to make it easier to test your database and
    Content Provider.  Note that you'll want your ForexContract class to exactly match the one
    in our solution to use these as-given.
 */
public class TestUtilities extends AndroidTestCase {
    static final String[] TEST_CURRENCIES = {"USD", "ZAR"};
    static final String TEST_CURRENCY_FROM = "USD";
    static final String TEST_CURRENCY_TO = "ZAR";
    static final long TEST_DATE = 1419033600L;  // December 20th, 2014

    static void validateCursor(String error, Cursor valueCursor, ContentValues expectedValues) {
        assertTrue("Empty cursor returned. " + error, valueCursor.moveToFirst());
        validateCurrentRecord(error, valueCursor, expectedValues);
        valueCursor.close();
    }

    static void validateCurrentRecord(String error, Cursor valueCursor, ContentValues expectedValues) {
        Set<Map.Entry<String, Object>> valueSet = expectedValues.valueSet();
        for (Map.Entry<String, Object> entry : valueSet) {
            String columnName = entry.getKey();
            int idx = valueCursor.getColumnIndex(columnName);
            assertFalse("Column '" + columnName + "' not found. " + error, idx == -1);
            String expectedValue = entry.getValue().toString();
            assertEquals("Value '" + entry.getValue().toString() +
                    "' did not match the expected value '" +
                    expectedValue + "'. " + error, expectedValue, valueCursor.getString(idx));
        }
    }

    /*
        Students: Use this to create some default rate values for your database tests.
     */
    static ContentValues createRateValues() {
        ContentValues rateValues = new ContentValues();
        rateValues.put(ForexContract.RateEntry.COLUMN_RATE_FROM_KEY, "USD");
        rateValues.put(ForexContract.RateEntry.COLUMN_RATE_TO_KEY, "ZAR");
        rateValues.put(ForexContract.RateEntry.COLUMN_RATE_DATE, TEST_DATE);
        rateValues.put(ForexContract.RateEntry.COLUMN_RATE_VALUE, 14.1);

        return rateValues;
    }

    /*
        Students: You can uncomment this helper function once you have finished creating the
        LocationEntry part of the ForexContract.
     */
    static ContentValues createCurrencyFromValues() {
        // Create a new map of values, where column names are the keys
        ContentValues testValues = new ContentValues();
        testValues.put(ForexContract.CurrencyEntry.COLUMN_CURRENCY_ID, "USD");
        testValues.put(ForexContract.CurrencyEntry.COLUMN_CURRENCY_NAME, "United States dollar");
        testValues.put(ForexContract.CurrencyEntry.COLUMN_CURRENCY_SYMBOL, "$");
        testValues.put(ForexContract.CurrencyEntry.COLUMN_COUNTRY_CODE, "US");
        testValues.put(ForexContract.CurrencyEntry.COLUMN_COUNTRY_NAME, "United States of America");
        testValues.put(ForexContract.CurrencyEntry.COLUMN_COUNTRY_FLAG_URL, "https://www.geoips.com//assets/img/flag/128h/us.png");

        return testValues;
    }

    static ContentValues createCurrencyToValues() {
        // Create a new map of values, where column names are the keys
        ContentValues testValues = new ContentValues();
        testValues.put(ForexContract.CurrencyEntry.COLUMN_CURRENCY_ID, "ZAR");
        testValues.put(ForexContract.CurrencyEntry.COLUMN_CURRENCY_NAME, "South African rand");
        testValues.put(ForexContract.CurrencyEntry.COLUMN_CURRENCY_SYMBOL, "R");
        testValues.put(ForexContract.CurrencyEntry.COLUMN_COUNTRY_CODE, "ZA");
        testValues.put(ForexContract.CurrencyEntry.COLUMN_COUNTRY_NAME, "South Africa");
        testValues.put(ForexContract.CurrencyEntry.COLUMN_COUNTRY_FLAG_URL, "https://www.geoips.com//assets/img/flag/128h/za.png");

        return testValues;
    }

    /*
        Students: You can uncomment this function once you have finished creating the
        LocationEntry part of the ForexContract as well as the ForexDbHelper.
     */
    static long insertCurrencyValues(Context context) {
        // insert our test records into the database
        ForexDbHelper dbHelper = new ForexDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues testValues = TestUtilities.createCurrencyFromValues();

        long locationRowId;
        locationRowId = db.insert(ForexContract.CurrencyEntry.TABLE_NAME, null, testValues);

        // Verify we got a row back.
        assertTrue("Error: Failure to insert USD Currency Values", locationRowId != -1);

        testValues = TestUtilities.createCurrencyToValues();

        locationRowId = 0;
        locationRowId = db.insert(ForexContract.CurrencyEntry.TABLE_NAME, null, testValues);

        // Verify we got a row back.
        assertTrue("Error: Failure to insert ZAR Currency Values", locationRowId != -1);

        return locationRowId;
    }

    static TestContentObserver getTestContentObserver() {
        return TestContentObserver.getTestContentObserver();
    }

    /*
        Students: The functions we provide inside of TestProvider use this utility class to test
        the ContentObserver callbacks using the PollingCheck class that we grabbed from the Android
        CTS tests.

        Note that this only tests that the onChange function is called; it does not test that the
        correct Uri is returned.
     */
    static class TestContentObserver extends ContentObserver {
        final HandlerThread mHT;
        boolean mContentChanged;

        private TestContentObserver(HandlerThread ht) {
            super(new Handler(ht.getLooper()));
            mHT = ht;
        }

        static TestContentObserver getTestContentObserver() {
            HandlerThread ht = new HandlerThread("ContentObserverThread");
            ht.start();
            return new TestContentObserver(ht);
        }

        // On earlier versions of Android, this onChange method is called
        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            mContentChanged = true;
        }

        public void waitForNotificationOrFail() {
            // Note: The PollingCheck class is taken from the Android CTS (Compatibility Test Suite).
            // It's useful to look at the Android CTS source for ideas on how to test your Android
            // applications.  The reason that PollingCheck works is that, by default, the JUnit
            // testing framework is not running on the main Android application thread.
            new PollingCheck(5000) {
                @Override
                protected boolean check() {
                    return mContentChanged;
                }
            }.run();
            mHT.quit();
        }
    }
}
