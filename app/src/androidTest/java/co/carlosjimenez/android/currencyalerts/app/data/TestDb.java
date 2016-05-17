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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import java.util.HashSet;

public class TestDb extends AndroidTestCase {

    public static final String LOG_TAG = TestDb.class.getSimpleName();

    // Since we want each test to start with a clean slate
    void deleteTheDatabase() {
        mContext.deleteDatabase(ForexDbHelper.DATABASE_NAME);
    }

    /*
        This function gets called before each test is executed to delete the database.  This makes
        sure that we always have a clean test.
     */
    public void setUp() {
        deleteTheDatabase();
    }

    /*
        Students: Uncomment this test once you've written the code to create the Currency
        table.  Note that you will have to have chosen the same column names that I did in
        my solution for this test to compile, so if you haven't yet done that, this is
        a good time to change your column names to match mine.

        Note that this only tests that the Currency table has the correct columns, since we
        give you the code for the rate table.  This test does not look at the
     */
    public void testCreateDb() throws Throwable {
        // build a HashSet of all of the table names we wish to look for
        // Note that there will be another table in the DB that stores the
        // Android metadata (db version information)
        final HashSet<String> tableNameHashSet = new HashSet<String>();
        tableNameHashSet.add(ForexContract.CurrencyEntry.TABLE_NAME);
        tableNameHashSet.add(ForexContract.RateEntry.TABLE_NAME);

        mContext.deleteDatabase(ForexDbHelper.DATABASE_NAME);
        SQLiteDatabase db = new ForexDbHelper(
                this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());

        // have we created the tables we want?
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        assertTrue("Error: This means that the database has not been created correctly",
                c.moveToFirst());

        // verify that the tables have been created
        do {
            tableNameHashSet.remove(c.getString(0));
        } while (c.moveToNext());

        // if this fails, it means that your database doesn't contain both the location entry
        // and rate entry tables
        assertTrue("Error: Your database was created without both the location entry and rate entry tables",
                tableNameHashSet.isEmpty());

        // now, do our tables contain the correct columns?
        c = db.rawQuery("PRAGMA table_info(" + ForexContract.CurrencyEntry.TABLE_NAME + ")",
                null);

        assertTrue("Error: This means that we were unable to query the database for table information.",
                c.moveToFirst());

        // Build a HashSet of all of the column names we want to look for
        final HashSet<String> locationColumnHashSet = new HashSet<String>();
        locationColumnHashSet.add(ForexContract.CurrencyEntry._ID);
        locationColumnHashSet.add(ForexContract.CurrencyEntry.COLUMN_CURRENCY_ID);
        locationColumnHashSet.add(ForexContract.CurrencyEntry.COLUMN_CURRENCY_NAME);
        locationColumnHashSet.add(ForexContract.CurrencyEntry.COLUMN_CURRENCY_SYMBOL);
        locationColumnHashSet.add(ForexContract.CurrencyEntry.COLUMN_COUNTRY_CODE);
        locationColumnHashSet.add(ForexContract.CurrencyEntry.COLUMN_COUNTRY_NAME);
        locationColumnHashSet.add(ForexContract.CurrencyEntry.COLUMN_COUNTRY_FLAG_URL);

        int columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            locationColumnHashSet.remove(columnName);
        } while (c.moveToNext());

        // if this fails, it means that your database doesn't contain all of the required location
        // entry columns
        assertTrue("Error: The database doesn't contain all of the required location entry columns",
                locationColumnHashSet.isEmpty());
        db.close();
    }

    /*
        Students:  Here is where you will build code to test that we can insert and query the
        location database.  We've done a lot of work for you.  You'll want to look in TestUtilities
        where you can uncomment out the "createCurrencyFromValues" function.  You can
        also make use of the ValidateCurrentRecord function from within TestUtilities.
    */
    public void testCurrencyTable() {
        insertCurrency();
    }

    /*
        Students:  Here is where you will build code to test that we can insert and query the
        database.  We've done a lot of work for you.  You'll want to look in TestUtilities
        where you can use the "createWeatherValues" function.  You can
        also make use of the validateCurrentRecord function from within TestUtilities.
     */
    public void testRateTable() {
        // First insert the location, and then use the locationRowId to insert
        // the rate. Make sure to cover as many failure cases as you can.

        // Instead of rewriting all of the code we've already written in testCurrencyTable
        // we can move this code to insertCurrency and then call insertCurrency from both
        // tests. Why move it? We need the code to return the ID of the inserted location
        // and our testCurrencyTable can only return void because it's a test.

        long locationRowId = insertCurrency();

        // Make sure we have a valid row ID.
        assertFalse("Error: Currency Not Inserted Correctly", locationRowId == -1L);

        // First step: Get reference to writable database
        // If there's an error in those massive SQL table creation Strings,
        // errors will be thrown here when you try to get a writable database.
        ForexDbHelper dbHelper = new ForexDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Second Step (Weather): Create rate values
        ContentValues rateValues = TestUtilities.createRateValues();

        // Third Step (Weather): Insert ContentValues into database and get a row ID back
        long rateRowId = db.insert(ForexContract.RateEntry.TABLE_NAME, null, rateValues);
        assertTrue(rateRowId != -1);

        // Fourth Step: Query the database and receive a Cursor back
        // A cursor is your primary interface to the query results.
        Cursor rateCursor = db.query(
                ForexContract.RateEntry.TABLE_NAME,  // Table to Query
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null  // sort order
        );

        // Move the cursor to the first valid database row and check to see if we have any rows
        assertTrue("Error: No Records returned from location query", rateCursor.moveToFirst());

        // Fifth Step: Validate the location Query
        TestUtilities.validateCurrentRecord("testInsertReadDb weatherEntry failed to validate",
                rateCursor, rateValues);

        // Move the cursor to demonstrate that there is only one record in the database
        assertFalse("Error: More than one record returned from rate query",
                rateCursor.moveToNext());

        // Sixth Step: Close cursor and database
        rateCursor.close();
        dbHelper.close();
    }


    /*
        Students: This is a helper method for the testRateTable quiz. You can move your
        code from testCurrencyTable to here so that you can call this code from both
        testRateTable and testCurrencyTable.
     */
    public long insertCurrency() {
        // First step: Get reference to writable database
        // If there's an error in those massive SQL table creation Strings,
        // errors will be thrown here when you try to get a writable database.
        ForexDbHelper dbHelper = new ForexDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Second Step: Create ContentValues of what you want to insert
        // (you can use the createCurrencyFromValues if you wish)
        ContentValues testValues = TestUtilities.createCurrencyFromValues();

        // Third Step: Insert ContentValues into database and get a row ID back
        long currencyRowId;
        currencyRowId = db.insert(ForexContract.CurrencyEntry.TABLE_NAME, null, testValues);

        // Verify we got a row back.
        assertTrue(currencyRowId != -1);

        // Data's inserted.  IN THEORY.  Now pull some out to stare at it and verify it made
        // the round trip.

        // Fourth Step: Query the database and receive a Cursor back
        // A cursor is your primary interface to the query results.
        Cursor cursor = db.query(
                ForexContract.CurrencyEntry.TABLE_NAME,  // Table to Query
                null, // all columns
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null // sort order
        );

        // Move the cursor to a valid database row and check to see if we got any records back
        // from the query
        assertTrue("Error: No Records returned from currency query", cursor.moveToFirst());

        // Fifth Step: Validate data in resulting Cursor with the original ContentValues
        // (you can use the validateCurrentRecord function in TestUtilities to validate the
        // query if you like)
        TestUtilities.validateCurrentRecord("Error: Currency Query Validation Failed",
                cursor, testValues);

        // Move the cursor to demonstrate that there is only one record in the database
        assertFalse("Error: More than one record returned from currency query",
                cursor.moveToNext());

        testValues = TestUtilities.createCurrencyToValues();

        // Third Step: Insert ContentValues into database and get a row ID back
        currencyRowId = 0;
        currencyRowId = db.insert(ForexContract.CurrencyEntry.TABLE_NAME, null, testValues);

        // Verify we got a row back.
        assertTrue(currencyRowId != -1);


        // Sixth Step: Close Cursor and Database
        cursor.close();
        db.close();
        return currencyRowId;
    }
}
