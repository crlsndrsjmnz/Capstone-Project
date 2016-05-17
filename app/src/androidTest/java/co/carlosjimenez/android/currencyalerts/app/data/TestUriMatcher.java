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

import android.content.UriMatcher;
import android.net.Uri;
import android.test.AndroidTestCase;

/*
    Uncomment this class when you are ready to test your UriMatcher.  Note that this class utilizes
    constants that are declared with package protection inside of the UriMatcher, which is why
    the test must be in the same data package as the Android app code.  Doing the test this way is
    a nice compromise between data hiding and testability.
 */
public class TestUriMatcher extends AndroidTestCase {
    static final String[] TEST_CURRENCIES = {"USD", "ZAR"};
    static final String TEST_CURRENCY_FROM = "USD";
    static final String TEST_CURRENCY_TO = "ZAR";

    private static final long TEST_DATE = 1419033600L;  // December 20th, 2014

    // content://co.carlosjimenez.android.currencyalerts.app/rate"
    private static final Uri TEST_RATE_DIR = ForexContract.RateEntry.CONTENT_URI;
    private static final Uri TEST_RATE_WITH_CURRENCY_DIR = ForexContract.RateEntry.buildCurrencyRate(TEST_CURRENCIES);
    private static final Uri TEST_RATE_WITH_CURRENCY_AND_DATE_DIR = ForexContract.RateEntry.buildCurrencyRateWithDate(TEST_CURRENCIES, TEST_DATE);
    // content://co.carlosjimenez.android.currencyalerts.app/currency"
    private static final Uri TEST_CURRENCY_DIR = ForexContract.CurrencyEntry.CONTENT_URI;

    /*
        Students: This function tests that your UriMatcher returns the correct integer value
        for each of the Uri types that our ContentProvider can handle.  Uncomment this when you are
        ready to test your UriMatcher.
     */
    public void testUriMatcher() {
        UriMatcher testMatcher = ForexProvider.buildUriMatcher();

        assertEquals("Error: The RATE URI was matched incorrectly.",
                testMatcher.match(TEST_RATE_DIR), ForexProvider.RATE);
        assertEquals("Error: The RATE WITH CURRENCY URI was matched incorrectly.",
                testMatcher.match(TEST_RATE_WITH_CURRENCY_DIR), ForexProvider.RATE_WITH_CURRENCY);
        assertEquals("Error: The RATE WITH CURRENCY AND DATE URI was matched incorrectly.",
                testMatcher.match(TEST_RATE_WITH_CURRENCY_AND_DATE_DIR), ForexProvider.RATE_WITH_CURRENCY_AND_DATE);
        assertEquals("Error: The CURRENCY URI was matched incorrectly.",
                testMatcher.match(TEST_CURRENCY_DIR), ForexProvider.CURRENCY);
    }
}
