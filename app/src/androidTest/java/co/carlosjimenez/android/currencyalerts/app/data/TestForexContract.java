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

import android.net.Uri;
import android.test.AndroidTestCase;

/*
    Students: This is NOT a complete test for the ForexContract --- just for the functions
    that we expect you to write.
 */
public class TestForexContract extends AndroidTestCase {

    // intentionally includes a slash to make sure Uri is getting quoted correctly
    static final String[] TEST_CURRENCIES = {"USD", "ZAR"};
    private static final long TEST_WEATHER_DATE = 1419033600L;  // December 20th, 2014

    /*
        Students: Uncomment this out to test your weather currency function.
     */
    public void testBuildRateCurrency() {
        Uri currencyUri = ForexContract.RateEntry.buildCurrencyRate(TEST_CURRENCIES);
        assertNotNull("Error: Null Uri returned.  You must fill-in buildRateCurrency in " +
                        "ForexContract.",
                currencyUri);

        String[] uriCurrencies = ForexContract.RateEntry.getCurrenciesFromUri(currencyUri);

        assertEquals("Error: Rate From currency not added properly to the Uri",
                TEST_CURRENCIES[0], uriCurrencies[0]);

        assertEquals("Error: Rate To currency not added properly to the Uri",
                TEST_CURRENCIES[1], uriCurrencies[1]);

        assertEquals("Error: Rate currency Uri doesn't match our expected result",
                currencyUri.toString(),
                "content://co.carlosjimenez.android.currencyalerts.app/rate/USD/ZAR");
    }
}
