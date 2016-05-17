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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import co.carlosjimenez.android.currencyalerts.app.data.ForexContract.CurrencyEntry;
import co.carlosjimenez.android.currencyalerts.app.data.ForexContract.RateEntry;

/**
 * Manages a local database for weather data.
 */
public class ForexDbHelper extends SQLiteOpenHelper {

    static final String DATABASE_NAME = "currency.db";
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;

    public ForexDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // Create a table to hold currencies.  A currency consists of the string supplied in the
        // currency setting, the city name, and the latitude and longitude
        final String SQL_CREATE_CURRENCY_TABLE = "CREATE TABLE " + CurrencyEntry.TABLE_NAME + " (" +
                CurrencyEntry._ID + " INTEGER PRIMARY KEY," +
                CurrencyEntry.COLUMN_CURRENCY_ID + " TEXT UNIQUE NOT NULL, " +
                CurrencyEntry.COLUMN_CURRENCY_NAME + " TEXT NOT NULL, " +
                CurrencyEntry.COLUMN_CURRENCY_SYMBOL + " TEXT NOT NULL, " +
                CurrencyEntry.COLUMN_COUNTRY_CODE + " TEXT NOT NULL, " +
                CurrencyEntry.COLUMN_COUNTRY_NAME + " TEXT NOT NULL, " +
                CurrencyEntry.COLUMN_COUNTRY_FLAG_URL + " TEXT NOT NULL " +
                " );";

        final String SQL_CREATE_RATE_TABLE = "CREATE TABLE " + RateEntry.TABLE_NAME + " (" +
                // Why AutoIncrement here, and not above?
                // Unique keys will be auto-generated in either case.  But for rate
                // forecasting, it's reasonable to assume the user will want information
                // for a certain date and all dates *following*, so the forecast data
                // should be sorted accordingly.
                RateEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                // the ID of the currency entry associated with this rate data
                RateEntry.COLUMN_RATE_FROM_KEY + " TEXT NOT NULL, " +
                RateEntry.COLUMN_RATE_TO_KEY + " TEXT NOT NULL, " +
                RateEntry.COLUMN_RATE_DATE + " INTEGER NOT NULL, " +
                RateEntry.COLUMN_RATE_VALUE + " REAL NOT NULL, " +

                // Set up the currency column as a foreign key to currency table.
                " FOREIGN KEY (" + RateEntry.COLUMN_RATE_FROM_KEY + ") REFERENCES " +
                CurrencyEntry.TABLE_NAME + " (" + CurrencyEntry.COLUMN_CURRENCY_ID + "), " +

                " FOREIGN KEY (" + RateEntry.COLUMN_RATE_TO_KEY + ") REFERENCES " +
                CurrencyEntry.TABLE_NAME + " (" + CurrencyEntry.COLUMN_CURRENCY_ID + "), " +

                // To assure the application have just one rate entry per day
                // per currency, it's created a UNIQUE constraint with REPLACE strategy
                " UNIQUE (" + RateEntry.COLUMN_RATE_DATE + ", " + RateEntry.COLUMN_RATE_FROM_KEY + ", " +
                RateEntry.COLUMN_RATE_TO_KEY + ") ON CONFLICT REPLACE);";

        sqLiteDatabase.execSQL(SQL_CREATE_CURRENCY_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_RATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        // Note that this only fires if you change the version number for your database.
        // It does NOT depend on the version number for your application.
        // If you want to update the schema without wiping data, commenting out the next 2 lines
        // should be your top priority before modifying this method.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + CurrencyEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + RateEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
