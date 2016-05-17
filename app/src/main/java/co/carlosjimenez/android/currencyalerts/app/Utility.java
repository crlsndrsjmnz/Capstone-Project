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

package co.carlosjimenez.android.currencyalerts.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.RectF;
import android.preference.PreferenceManager;

import java.text.SimpleDateFormat;

import co.carlosjimenez.android.currencyalerts.app.data.Currency;
import co.carlosjimenez.android.currencyalerts.app.sync.ForexSyncAdapter;
import co.carlosjimenez.android.currencyalerts.app.sync.LoadCurrencyTask;

public class Utility {

    public static final String DATE_FORMAT = "yyyy/MM/dd HH:mm";
    private static final String LOG_TAG = Utility.class.getSimpleName();

    /**
     * @param c Context used to get the SharedPreferences
     * @return the currency load status integer type
     */
    @SuppressWarnings("ResourceType")
    static public
    @LoadCurrencyTask.CurrencyStatus
    int getCurrencyStatus(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return sp.getInt(c.getString(R.string.pref_currency_status_key), LoadCurrencyTask.CURRENCY_STATUS_UNKNOWN);
    }

    /**
     * @param c Context used to get the SharedPreferences
     * @return the forex sync status integer type
     */
    @SuppressWarnings("ResourceType")
    static public
    @ForexSyncAdapter.ForexStatus
    int getForexStatus(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return sp.getInt(c.getString(R.string.pref_forex_status_key), ForexSyncAdapter.FOREX_STATUS_UNKNOWN);
    }

    /**
     * @param c Context used to get the SharedPreferences
     * @return the forex sync status integer type
     */
    static public Currency getMainCurrency(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return new Currency(
                sp.getString(c.getString(R.string.pref_main_currency_id_key), c.getString(R.string.us_currency_id)),
                sp.getString(c.getString(R.string.pref_main_currency_name_key), c.getString(R.string.us_currency_name)),
                sp.getString(c.getString(R.string.pref_main_currency_symbol_key), c.getString(R.string.us_currency_symbol)),
                sp.getString(c.getString(R.string.pref_main_country_code_key), c.getString(R.string.us_country_code)),
                sp.getString(c.getString(R.string.pref_main_country_name_key), c.getString(R.string.us_country_name)),
                sp.getString(c.getString(R.string.pref_main_country_flag_key), c.getString(R.string.us_country_flag)));
    }

    /**
     * Helper method to convert the database representation of the date into something to display
     * to users.  As classy and polished a user experience as "20140102" is, we can do better.
     *
     * @param context      Context to use for resource localization
     * @param dateInMillis The date in milliseconds
     * @return a user-friendly representation of the date.
     */
    public static String getDateString(Context context, long dateInMillis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(Utility.DATE_FORMAT);
        return dateFormat.format(dateInMillis);
    }

    public static String formatCurrencyRate(Context context, String currency_symbol, double currencyRate) {

        String rate = context.getString(R.string.format_currency_rate, currency_symbol, currencyRate);
        String formattedRate = "";

        int prefixLen = currency_symbol.length();
        int j = 0;

        if (rate.startsWith(currency_symbol + " "))
            prefixLen++;

        for (int i = rate.length() - 1; i >= prefixLen; i--) {
            if (i <= rate.length() - 5) {
                if (j < 2) {
                    j++;
                } else {
                    formattedRate = "," + formattedRate;
                    j = 0;
                }
            }
            formattedRate = rate.charAt(i) + formattedRate;
        }

        formattedRate = rate.substring(0, prefixLen) + formattedRate;

        return formattedRate;
    }

    public static int getRateFontPXSize(int length) {
        if (length <= 10)
            return 91;
        else if (length == 11)
            return 82;
        else if (length == 12)
            return 74;
        else if (length == 13)
            return 70;
        else if (length == 14)
            return 65;
        else if (length == 15)
            return 60;
        else if (length == 16)
            return 55;
        else if (length == 17)
            return 53;
        else if (length == 18)
            return 51;
        else if (length == 19)
            return 47;
        else if (length == 20)
            return 44;
        else if (length == 21)
            return 43;
        else if (length == 22)
            return 42;
        else if (length == 23)
            return 39;
        else if (length == 24)
            return 36;
        else if (length == 25)
            return 35;
        else if (length == 26)
            return 35;
        else if (length == 27)
            return 33;
        else if (length == 28)
            return 31;
        else if (length == 29)
            return 30;
        else if (length == 30)
            return 29;
        else if (length == 31)
            return 29;
        else if (length == 32)
            return 27;
        else if (length == 33)
            return 27;
        else if (length == 34)
            return 26;
        else if (length >= 35)
            return 26;

        return -1;
    }

    private interface SizeTester {
        /**
         * @param suggestedSize  Size of text to be tested
         * @param availableSpace available space in which text must fit
         * @return an integer < 0 if after applying {@code suggestedSize} to
         * text, it takes less space than {@code availableSpace}, > 0
         * otherwise
         */
        int onTestSize(int suggestedSize, RectF availableSpace);
    }
}
