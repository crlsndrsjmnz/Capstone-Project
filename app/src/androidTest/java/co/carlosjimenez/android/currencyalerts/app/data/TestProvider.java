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

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.test.AndroidTestCase;
import android.util.Log;

import co.carlosjimenez.android.currencyalerts.app.data.ForexContract.CurrencyEntry;
import co.carlosjimenez.android.currencyalerts.app.data.ForexContract.RateEntry;

/*
    Note: This is not a complete set of tests of the Sunshine ContentProvider, but it does test
    that at least the basic functionality has been implemented correctly.

    Students: Uncomment the tests in this class as you implement the functionality in your
    ContentProvider to make sure that you've implemented things reasonably correctly.
 */
public class TestProvider extends AndroidTestCase {

    public static final String LOG_TAG = TestProvider.class.getSimpleName();
    static private final int BULK_INSERT_RECORDS_TO_INSERT = 10;

    static ContentValues[] createBulkInsertRateValues() {
        long currentTestDate = TestUtilities.TEST_DATE;
        long millisecondsInADay = 1000 * 60 * 60 * 24;
        double rateValue = 15.1;
        ContentValues[] returnContentValues = new ContentValues[BULK_INSERT_RECORDS_TO_INSERT];

        for (int i = 0; i < BULK_INSERT_RECORDS_TO_INSERT; i++, currentTestDate += millisecondsInADay) {
            ContentValues rateValues = new ContentValues();
            rateValues.put(RateEntry.COLUMN_RATE_FROM_KEY, TestUtilities.TEST_CURRENCY_FROM);
            rateValues.put(RateEntry.COLUMN_RATE_TO_KEY, TestUtilities.TEST_CURRENCY_TO);
            rateValues.put(RateEntry.COLUMN_RATE_DATE, currentTestDate);
            rateValues.put(RateEntry.COLUMN_RATE_VALUE, rateValue * 1.01);
            returnContentValues[i] = rateValues;
        }
        return returnContentValues;
    }

    /*
       This helper function deletes all records from both database tables using the ContentProvider.
       It also queries the ContentProvider to make sure that the database has been successfully
       deleted, so it cannot be used until the Query and Delete functions have been written
       in the ContentProvider.

       Students: Replace the calls to deleteAllRecordsFromDB with this one after you have written
       the delete functionality in the ContentProvider.
     */
    public void deleteAllRecordsFromProvider() {
        mContext.getContentResolver().delete(
                RateEntry.CONTENT_URI,
                null,
                null
        );
        mContext.getContentResolver().delete(
                CurrencyEntry.CONTENT_URI,
                null,
                null
        );

        Cursor cursor = mContext.getContentResolver().query(
                RateEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals("Error: Records not deleted from Rate table during delete", 0, cursor.getCount());
        cursor.close();

        cursor = mContext.getContentResolver().query(
                CurrencyEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals("Error: Records not deleted from Currency table during delete", 0, cursor.getCount());
        cursor.close();
    }

    /*
        Student: Refactor this function to use the deleteAllRecordsFromProvider functionality once
        you have implemented delete functionality there.
     */
    public void deleteAllRecords() {
        deleteAllRecordsFromProvider();
    }

    // Since we want each test to start with a clean slate, run deleteAllRecords
    // in setUp (called by the test runner before each test).
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteAllRecords();
    }

    /*
        This test checks to make sure that the content provider is registered correctly.
        Students: Uncomment this test to make sure you've correctly registered the ForexProvider.
     */
    public void testProviderRegistry() {
        PackageManager pm = mContext.getPackageManager();

        // We define the component name based on the package name from the context and the
        // ForexProvider class.
        ComponentName componentName = new ComponentName(mContext.getPackageName(),
                ForexProvider.class.getName());
        try {
            // Fetch the provider info using the component name from the PackageManager
            // This throws an exception if the provider isn't registered.
            ProviderInfo providerInfo = pm.getProviderInfo(componentName, 0);

            // Make sure that the registered authority matches the authority from the Contract.
            assertEquals("Error: ForexProvider registered with authority: " + providerInfo.authority +
                            " instead of authority: " + ForexContract.CONTENT_AUTHORITY,
                    providerInfo.authority, ForexContract.CONTENT_AUTHORITY);
        } catch (PackageManager.NameNotFoundException e) {
            // I guess the provider isn't registered correctly.
            assertTrue("Error: ForexProvider not registered at " + mContext.getPackageName(),
                    false);
        }
    }

    /*
            This test doesn't touch the database.  It verifies that the ContentProvider returns
            the correct type for each type of URI that it can handle.
            Students: Uncomment this test to verify that your implementation of GetType is
            functioning correctly.
         */
    public void testGetType() {
        // content://co.carlosjimenez.android.currencyalerts.app/rate/
        String type = mContext.getContentResolver().getType(RateEntry.CONTENT_URI);
        // vnd.android.cursor.dir/co.carlosjimenez.android.currencyalerts.app/rate
        assertEquals("Error: the RateEntry CONTENT_URI should return RateEntry.CONTENT_TYPE",
                RateEntry.CONTENT_TYPE, type);

        String[] testCurrencies = {"USD", "ZAR"};
        // content://co.carlosjimenez.android.currencyalerts.app/rate/USD/ZAR
        type = mContext.getContentResolver().getType(
                RateEntry.buildCurrencyRate(testCurrencies));
        // vnd.android.cursor.dir/co.carlosjimenez.android.currencyalerts.app/rate
        assertEquals("Error: the RateEntry CONTENT_URI with location should return RateEntry.CONTENT_TYPE",
                RateEntry.CONTENT_TYPE, type);

        long testDate = 1419120000L; // December 21st, 2014
        // content://co.carlosjimenez.android.currencyalerts.app/rate/USD/ZAR/20140612
        type = mContext.getContentResolver().getType(
                RateEntry.buildCurrencyRateWithDate(testCurrencies, testDate));
        // vnd.android.cursor.item/co.carlosjimenez.android.currencyalerts.app/rate/1419120000
        assertEquals("Error: the RateEntry CONTENT_URI with location and date should return RateEntry.CONTENT_ITEM_TYPE",
                RateEntry.CONTENT_ITEM_TYPE, type);

        // content://co.carlosjimenez.android.currencyalerts.app/currency/
        type = mContext.getContentResolver().getType(CurrencyEntry.CONTENT_URI);
        // vnd.android.cursor.dir/co.carlosjimenez.android.currencyalerts.app/currency
        assertEquals("Error: the CurrencyEntry CONTENT_URI should return CurrencyEntry.CONTENT_TYPE",
                CurrencyEntry.CONTENT_TYPE, type);
    }

    /*
        This test uses the database directly to insert and then uses the ContentProvider to
        read out the data.  Uncomment this test to see if the basic rate query functionality
        given in the ContentProvider is working correctly.
     */
    public void testBasicRateQuery() {
        // insert our test records into the database
        ForexDbHelper dbHelper = new ForexDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues testValues = TestUtilities.createCurrencyFromValues();
        long currencyRowId = TestUtilities.insertCurrencyValues(mContext);

        // Fantastic.  Now that we have a currency, add some rate!
        ContentValues rateValues = TestUtilities.createRateValues();

        long rateRowId = db.insert(RateEntry.TABLE_NAME, null, rateValues);
        assertTrue("Unable to Insert RateEntry into the Database", rateRowId != -1);

        db.close();

        // Test the basic content provider query
        Cursor rateCursor = mContext.getContentResolver().query(
                RateEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        // Make sure we get the correct cursor out of the database
        TestUtilities.validateCursor("testBasicRateQuery", rateCursor, rateValues);
    }

    /*
        This test uses the database directly to insert and then uses the ContentProvider to
        read out the data.  Uncomment this test to see if your currency queries are
        performing correctly.
     */
    public void testBasicCurrencyQueries() {
        // insert our test records into the database
        ForexDbHelper dbHelper = new ForexDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues testValues = TestUtilities.createCurrencyFromValues();
        long currencyRowId = TestUtilities.insertCurrencyValues(mContext);

        // Test the basic content provider query
        Cursor currencyCursor = mContext.getContentResolver().query(
                CurrencyEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        // Make sure we get the correct cursor out of the database
        TestUtilities.validateCursor("testBasicCurrencyQueries, currency query", currencyCursor, testValues);

        // Has the NotificationUri been set correctly? --- we can only test this easily against API
        // level 19 or greater because getNotificationUri was added in API level 19.
        if (Build.VERSION.SDK_INT >= 19) {
            assertEquals("Error: Currency Query did not properly set NotificationUri",
                    currencyCursor.getNotificationUri(), CurrencyEntry.CONTENT_URI);
        }
    }

    /*
        This test uses the provider to insert and then update the data. Uncomment this test to
        see if your update currency is functioning correctly.
     */
    public void testUpdateCurrency() {
        // Create a new map of values, where column names are the keys
        ContentValues values = TestUtilities.createCurrencyFromValues();

        Uri currencyUri = mContext.getContentResolver().
                insert(CurrencyEntry.CONTENT_URI, values);
        long currencyRowId = ContentUris.parseId(currencyUri);

        // Verify we got a row back.
        assertTrue(currencyRowId != -1);
        Log.d(LOG_TAG, "New row id: " + currencyRowId);

        ContentValues updatedValues = new ContentValues(values);
        updatedValues.put(CurrencyEntry._ID, currencyRowId);
        updatedValues.put(CurrencyEntry.COLUMN_COUNTRY_NAME, "Santa's Village");

        // Create a cursor with observer to make sure that the content provider is notifying
        // the observers as expected
        Cursor currencyCursor = mContext.getContentResolver().query(CurrencyEntry.CONTENT_URI, null, null, null, null);

        TestUtilities.TestContentObserver tco = TestUtilities.getTestContentObserver();
        currencyCursor.registerContentObserver(tco);

        int count = mContext.getContentResolver().update(
                CurrencyEntry.CONTENT_URI, updatedValues, CurrencyEntry._ID + "= ?",
                new String[]{Long.toString(currencyRowId)});
        assertEquals(count, 1);

        // Test to make sure our observer is called.  If not, we throw an assertion.
        //
        // Students: If your code is failing here, it means that your content provider
        // isn't calling getContext().getContentResolver().notifyChange(uri, null);
        tco.waitForNotificationOrFail();

        currencyCursor.unregisterContentObserver(tco);
        currencyCursor.close();

        // A cursor is your primary interface to the query results.
        Cursor cursor = mContext.getContentResolver().query(
                CurrencyEntry.CONTENT_URI,
                null,   // projection
                CurrencyEntry._ID + " = " + currencyRowId,
                null,   // Values for the "where" clause
                null    // sort order
        );

        TestUtilities.validateCursor("testUpdateCurrency.  Error validating currency entry update.",
                cursor, updatedValues);

        cursor.close();
    }

    // Make sure we can still delete after adding/updating stuff
    //
    // Student: Uncomment this test after you have completed writing the insert functionality
    // in your provider.  It relies on insertions with testInsertReadProvider, so insert and
    // query functionality must also be complete before this test can be used.
    public void testInsertReadProvider() {
        ContentValues testFromValues = TestUtilities.createCurrencyFromValues();

        // Register a content observer for our insert.  This time, directly with the content resolver
        TestUtilities.TestContentObserver tco = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(CurrencyEntry.CONTENT_URI, true, tco);
        Uri currencyUri = mContext.getContentResolver().insert(CurrencyEntry.CONTENT_URI, testFromValues);

        // Did our content observer get called?  Students:  If this fails, your insert currency
        // isn't calling getContext().getContentResolver().notifyChange(uri, null);
        tco.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(tco);

        long currencyRowId = ContentUris.parseId(currencyUri);

        // Verify we got a row back.
        assertTrue(currencyRowId != -1);

        // *****************

        ContentValues testToValues = TestUtilities.createCurrencyToValues();

        // Register a content observer for our insert.  This time, directly with the content resolver
        mContext.getContentResolver().registerContentObserver(CurrencyEntry.CONTENT_URI, true, tco);
        currencyUri = mContext.getContentResolver().insert(CurrencyEntry.CONTENT_URI, testToValues);

        // Did our content observer get called?  Students:  If this fails, your insert currency
        // isn't calling getContext().getContentResolver().notifyChange(uri, null);
        tco.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(tco);

        currencyRowId = ContentUris.parseId(currencyUri);

        // Verify we got a row back.
        assertTrue(currencyRowId != -1);

        // *****************

        // Data's inserted.  IN THEORY.  Now pull some out to stare at it and verify it made
        // the round trip.

        // A cursor is your primary interface to the query results.
        Cursor cursor = mContext.getContentResolver().query(
                CurrencyEntry.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );

        TestUtilities.validateCursor("testInsertReadProvider. Error validating CurrencyEntry.",
                cursor, testFromValues);

        // Fantastic.  Now that we have a currency, add some rate!
        ContentValues rateValues = TestUtilities.createRateValues();
        // The TestContentObserver is a one-shot class
        tco = TestUtilities.getTestContentObserver();

        mContext.getContentResolver().registerContentObserver(RateEntry.CONTENT_URI, true, tco);

        Uri rateInsertUri = mContext.getContentResolver()
                .insert(RateEntry.CONTENT_URI, rateValues);
        assertTrue(rateInsertUri != null);

        // Did our content observer get called?  Students:  If this fails, your insert rate
        // in your ContentProvider isn't calling
        // getContext().getContentResolver().notifyChange(uri, null);
        tco.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(tco);

        // A cursor is your primary interface to the query results.
        Cursor rateCursor = mContext.getContentResolver().query(
                RateEntry.CONTENT_URI,  // Table to Query
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null // columns to group by
        );

        TestUtilities.validateCursor("testInsertReadProvider. Error validating RateEntry insert.",
                rateCursor, rateValues);

        // Add the currency values in with the rate data so that we can make
        // sure that the join worked and we actually get all the values back
        rateValues.putAll(testToValues);

        // Get the joined Rate and Currency data
        rateCursor = mContext.getContentResolver().query(
                RateEntry.buildCurrencyRate(TestUtilities.TEST_CURRENCIES),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );
        TestUtilities.validateCursor("testInsertReadProvider.  Error validating joined Rate and Currency Data.",
                rateCursor, rateValues);

        // Get the joined Rate and Currency data with a start date
        rateCursor = mContext.getContentResolver().query(
                RateEntry.buildCurrencyRateWithStartDate(
                        TestUtilities.TEST_CURRENCIES, TestUtilities.TEST_DATE),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );
        TestUtilities.validateCursor("testInsertReadProvider.  Error validating joined Rate and Currency Data with start date.",
                rateCursor, rateValues);

        // Get the joined Rate data for a specific date
        rateCursor = mContext.getContentResolver().query(
                RateEntry.buildCurrencyRateWithDate(TestUtilities.TEST_CURRENCIES, TestUtilities.TEST_DATE),
                null,
                null,
                null,
                null
        );
        TestUtilities.validateCursor("testInsertReadProvider.  Error validating joined Rate and Currency data for a specific date.",
                rateCursor, rateValues);
    }

    // Make sure we can still delete after adding/updating stuff
    //
    // Student: Uncomment this test after you have completed writing the delete functionality
    // in your provider.  It relies on insertions with testInsertReadProvider, so insert and
    // query functionality must also be complete before this test can be used.
    public void testDeleteRecords() {
        testInsertReadProvider();

        // Register a content observer for our currency delete.
        TestUtilities.TestContentObserver currencyObserver = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(CurrencyEntry.CONTENT_URI, true, currencyObserver);

        // Register a content observer for our rate delete.
        TestUtilities.TestContentObserver rateObserver = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(CurrencyEntry.CONTENT_URI, true, rateObserver);

        deleteAllRecordsFromProvider();

        // Students: If either of these fail, you most-likely are not calling the
        // getContext().getContentResolver().notifyChange(uri, null); in the ContentProvider
        // delete.  (only if the insertReadProvider is succeeding)
        currencyObserver.waitForNotificationOrFail();
        rateObserver.waitForNotificationOrFail();

        mContext.getContentResolver().unregisterContentObserver(currencyObserver);
        mContext.getContentResolver().unregisterContentObserver(rateObserver);
    }

    // Student: Uncomment this test after you have completed writing the BulkInsert functionality
    // in your provider.  Note that this test will work with the built-in (default) provider
    // implementation, which just inserts records one-at-a-time, so really do implement the
    // BulkInsert ContentProvider function.
    public void testBulkInsert() {
        // first, let's create a currency value
        ContentValues testValues = TestUtilities.createCurrencyFromValues();
        Uri currencyUri = mContext.getContentResolver().insert(CurrencyEntry.CONTENT_URI, testValues);
        long currencyRowId = ContentUris.parseId(currencyUri);

        // Verify we got a row back.
        assertTrue(currencyRowId != -1);

        // Data's inserted.  IN THEORY.  Now pull some out to stare at it and verify it made
        // the round trip.

        // A cursor is your primary interface to the query results.
        Cursor cursor = mContext.getContentResolver().query(
                CurrencyEntry.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );

        TestUtilities.validateCursor("testBulkInsert. Error validating CurrencyEntry.",
                cursor, testValues);

        testValues = TestUtilities.createCurrencyToValues();
        currencyUri = mContext.getContentResolver().insert(CurrencyEntry.CONTENT_URI, testValues);
        currencyRowId = ContentUris.parseId(currencyUri);

        // Verify we got a row back.
        assertTrue(currencyRowId != -1);

        // Now we can bulkInsert some rate.  In fact, we only implement BulkInsert for rate
        // entries.  With ContentProviders, you really only have to implement the features you
        // use, after all.
        ContentValues[] bulkInsertContentValues = createBulkInsertRateValues();

        // Register a content observer for our bulk insert.
        TestUtilities.TestContentObserver rateObserver = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(RateEntry.CONTENT_URI, true, rateObserver);

        int insertCount = mContext.getContentResolver().bulkInsert(RateEntry.CONTENT_URI, bulkInsertContentValues);

        // Students:  If this fails, it means that you most-likely are not calling the
        // getContext().getContentResolver().notifyChange(uri, null); in your BulkInsert
        // ContentProvider method.
        rateObserver.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(rateObserver);

        assertEquals(insertCount, BULK_INSERT_RECORDS_TO_INSERT);

        // A cursor is your primary interface to the query results.
        cursor = mContext.getContentResolver().query(
                RateEntry.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                RateEntry.COLUMN_RATE_DATE + " ASC"  // sort order == by DATE ASCENDING
        );

        // we should have as many records in the database as we've inserted
        assertEquals(cursor.getCount(), BULK_INSERT_RECORDS_TO_INSERT);

        // and let's make sure they match the ones we created
        cursor.moveToFirst();
        for (int i = 0; i < BULK_INSERT_RECORDS_TO_INSERT; i++, cursor.moveToNext()) {
            TestUtilities.validateCurrentRecord("testBulkInsert.  Error validating RateEntry " + i,
                    cursor, bulkInsertContentValues[i]);
        }
        cursor.close();
    }
}
