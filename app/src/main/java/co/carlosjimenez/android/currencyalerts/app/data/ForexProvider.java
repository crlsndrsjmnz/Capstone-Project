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

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class ForexProvider extends ContentProvider {

    public static final String LOG_TAG = ForexProvider.class.getSimpleName();

    static final int RATE = 100;
    static final int RATE_WITH_CURRENCY = 101;
    static final int RATE_ALL_WITH_CURRENCY = 102;
    static final int RATE_WITH_CURRENCY_AND_DATE = 103;
    static final int CURRENCY = 300;

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private static final SQLiteQueryBuilder sRateByCurrencyQueryBuilder;

    //currency_from_id = ? AND currency_to_id = ?
    private static final String sCurrencySelection =
            ForexContract.RateEntry.TABLE_NAME + "." +
                    ForexContract.RateEntry.COLUMN_RATE_FROM_KEY + " = ? AND " +
                    ForexContract.RateEntry.TABLE_NAME + "." +
                    ForexContract.RateEntry.COLUMN_RATE_TO_KEY + " = ?";
    //currency_from_id = ?
    private static final String sStartCurrencySelection =
            ForexContract.RateEntry.TABLE_NAME + "." +
                    ForexContract.RateEntry.COLUMN_RATE_FROM_KEY + " = ?";
    //currency_from_id = ? AND currency_to_id = ? AND date >= ?
    private static final String sCurrencyWithStartDateSelection =
            ForexContract.RateEntry.TABLE_NAME + "." +
                    ForexContract.RateEntry.COLUMN_RATE_FROM_KEY + " = ? AND " +
                    ForexContract.RateEntry.TABLE_NAME + "." +
                    ForexContract.RateEntry.COLUMN_RATE_TO_KEY + " = ? AND " +
                    ForexContract.RateEntry.COLUMN_RATE_DATE + " >= ? ";
    //currency_from_id = ? AND date >= ?
    private static final String sStartCurrencyWithDateSelection =
            ForexContract.RateEntry.TABLE_NAME + "." +
                    ForexContract.RateEntry.COLUMN_RATE_FROM_KEY + " = ? AND " +
                    ForexContract.RateEntry.COLUMN_RATE_DATE + " = ? ";
    //currency_from_id = ? AND currency_to_id = ? AND date = ?
    private static final String sCurrencyAndDaySelection =
            ForexContract.RateEntry.TABLE_NAME + "." +
                    ForexContract.RateEntry.COLUMN_RATE_FROM_KEY + " = ? AND " +
                    ForexContract.RateEntry.TABLE_NAME + "." +
                    ForexContract.RateEntry.COLUMN_RATE_TO_KEY + " = ? AND " +
                    ForexContract.RateEntry.COLUMN_RATE_DATE + " = ? ";

    static {
        sRateByCurrencyQueryBuilder = new SQLiteQueryBuilder();

        //This is an inner join which looks like
        //rate INNER JOIN currency ON rate.currency_id = currency._id
        sRateByCurrencyQueryBuilder.setTables(
                ForexContract.RateEntry.TABLE_NAME +
                        " INNER JOIN " +
                        ForexContract.CurrencyEntry.TABLE_NAME + " AS " +
                        ForexContract.RateEntry.JOIN_RATE_FROM_ALIAS +
                        " ON " + ForexContract.RateEntry.TABLE_NAME +
                        "." + ForexContract.RateEntry.COLUMN_RATE_FROM_KEY +
                        " = " + ForexContract.RateEntry.JOIN_RATE_FROM_ALIAS +
                        "." + ForexContract.CurrencyEntry.COLUMN_CURRENCY_ID +
                        " INNER JOIN " +
                        ForexContract.CurrencyEntry.TABLE_NAME + " AS " +
                        ForexContract.RateEntry.JOIN_RATE_TO_ALIAS +
                        " ON " + ForexContract.RateEntry.TABLE_NAME +
                        "." + ForexContract.RateEntry.COLUMN_RATE_TO_KEY +
                        " = " + ForexContract.RateEntry.JOIN_RATE_TO_ALIAS +
                        "." + ForexContract.CurrencyEntry.COLUMN_CURRENCY_ID);
    }

    private ForexDbHelper mOpenHelper;

    static UriMatcher buildUriMatcher() {
        // I know what you're thinking.  Why create a UriMatcher when you can use regular
        // expressions instead?  Because you're not crazy, that's why.

        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = ForexContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, ForexContract.PATH_RATE, RATE);
        matcher.addURI(authority, ForexContract.PATH_RATE + "/*", RATE_ALL_WITH_CURRENCY);
        matcher.addURI(authority, ForexContract.PATH_RATE + "/*/*", RATE_WITH_CURRENCY);
        matcher.addURI(authority, ForexContract.PATH_RATE + "/*/*/#", RATE_WITH_CURRENCY_AND_DATE);

        matcher.addURI(authority, ForexContract.PATH_CURRENCY, CURRENCY);
        return matcher;
    }

    private Cursor getRateByCurrency(Uri uri, String[] projection, String sortOrder) {
        String[] currencies = ForexContract.RateEntry.getCurrenciesFromUri(uri);
        long startDate = ForexContract.RateEntry.getStartDateFromUri(uri);

        String[] selectionArgs;
        String selection;

        if (startDate == 0) {
            selection = sCurrencySelection;
            selectionArgs = currencies;
        } else {
            selectionArgs = new String[]{currencies[0], currencies[1], Long.toString(startDate)};
            selection = sCurrencyWithStartDateSelection;
        }

        return sRateByCurrencyQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    private Cursor getAllRatesByCurrency(Uri uri, String[] projection, String sortOrder) {
        String currency = ForexContract.RateEntry.getCurrencyFromUri(uri);
        long startDate = ForexContract.RateEntry.getStartDateFromUri(uri);

        String[] selectionArgs;
        String selection;

        if (startDate == 0) {
            selection = sStartCurrencySelection;
            selectionArgs = new String[]{currency};
        } else {
            selectionArgs = new String[]{currency, Long.toString(startDate)};
            selection = sStartCurrencyWithDateSelection;
        }

        return sRateByCurrencyQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    private Cursor getRateByCurrencyAndDate(
            Uri uri, String[] projection, String sortOrder) {
        String[] currencies = ForexContract.RateEntry.getCurrenciesFromUri(uri);
        long date = ForexContract.RateEntry.getDateFromUri(uri);

        return sRateByCurrencyQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sCurrencyAndDaySelection,
                new String[]{currencies[0], currencies[1], Long.toString(date)},
                null,
                null,
                sortOrder
        );
    }

    /*
        Students: We've coded this for you.  We just create a new ForexDbHelper for later use
        here.
     */
    @Override
    public boolean onCreate() {
        mOpenHelper = new ForexDbHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            // Student: Uncomment and fill out these two cases
            case RATE_WITH_CURRENCY_AND_DATE:
                return ForexContract.RateEntry.CONTENT_ITEM_TYPE;
            case RATE_ALL_WITH_CURRENCY:
                return ForexContract.RateEntry.CONTENT_ITEM_TYPE;
            case RATE_WITH_CURRENCY:
                return ForexContract.RateEntry.CONTENT_TYPE;
            case RATE:
                return ForexContract.RateEntry.CONTENT_TYPE;
            case CURRENCY:
                return ForexContract.CurrencyEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;

        switch (sUriMatcher.match(uri)) {
            // "rate/*/*/*"
            case RATE_WITH_CURRENCY_AND_DATE: {
                retCursor = getRateByCurrencyAndDate(uri, projection, sortOrder);
                break;
            }
            // "rate/*/*"
            case RATE_WITH_CURRENCY: {
                retCursor = getRateByCurrency(uri, projection, sortOrder);
                break;
            }
            // "rate/*"
            case RATE_ALL_WITH_CURRENCY: {
                retCursor = getAllRatesByCurrency(uri, projection, sortOrder);
                break;
            }
            // "rate"
            case RATE: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        ForexContract.RateEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "currency"
            case CURRENCY: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        ForexContract.CurrencyEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    /*
        Student: Add the ability to insert Currencies to the implementation of this function.
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case RATE: {
                normalizeDate(values);
                long _id = db.insert(ForexContract.RateEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = ForexContract.RateEntry.buildRateUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case CURRENCY: {
                long _id = db.insert(ForexContract.CurrencyEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = ForexContract.CurrencyEntry.buildCurrencyUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        // this makes delete all rows return the number of rows deleted
        if (null == selection) selection = "1";
        switch (match) {
            case RATE:
                rowsDeleted = db.delete(
                        ForexContract.RateEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case CURRENCY:
                rowsDeleted = db.delete(
                        ForexContract.CurrencyEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    private void normalizeDate(ContentValues values) {
        // normalize the date value
        if (values.containsKey(ForexContract.RateEntry.COLUMN_RATE_DATE)) {
            long dateValue = values.getAsLong(ForexContract.RateEntry.COLUMN_RATE_DATE);
            values.put(ForexContract.RateEntry.COLUMN_RATE_DATE, ForexContract.normalizeDate(dateValue));
        }
    }

    @Override
    public int update(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case RATE:
                normalizeDate(values);
                rowsUpdated = db.update(ForexContract.RateEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case CURRENCY:
                rowsUpdated = db.update(ForexContract.CurrencyEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case RATE:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        normalizeDate(value);
                        long _id = db.insert(ForexContract.RateEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }

    // You do not need to call this method. This is a method specifically to assist the testing
    // framework in running smoothly. You can read more at:
    // http://developer.android.com/reference/android/content/ContentProvider.html#shutdown()
    @Override
    @TargetApi(11)
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }
}