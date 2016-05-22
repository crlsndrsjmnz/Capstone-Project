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

package co.carlosjimenez.android.currencyalerts.app.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;

import java.util.concurrent.ExecutionException;

import co.carlosjimenez.android.currencyalerts.app.R;
import co.carlosjimenez.android.currencyalerts.app.Utility;
import co.carlosjimenez.android.currencyalerts.app.data.Currency;
import co.carlosjimenez.android.currencyalerts.app.data.ForexContract;

/**
 * RemoteViewsService controlling the data being shown in the scrollable weather detail widget
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class DetailWidgetRemoteViewsService extends RemoteViewsService {

    public final String LOG_TAG = DetailWidgetRemoteViewsService.class.getSimpleName();

    // These indices are tied to FOREX_COLUMNS.  If FOREX_COLUMNS changes, these
    // must change.
    static final int COL_RATE_ID = 0;
    static final int COL_CURRENCY_FROM_ID = 1;
    static final int COL_CURRENCY_FROM_NAME = 2;
    static final int COL_CURRENCY_FROM_SYMBOL = 3;
    static final int COL_COUNTRY_FROM_CODE = 4;
    static final int COL_COUNTRY_FROM_NAME = 5;
    static final int COL_COUNTRY_FROM_FLAG = 6;
    static final int COL_CURRENCY_TO_ID = 7;
    static final int COL_CURRENCY_TO_NAME = 8;
    static final int COL_CURRENCY_TO_SYMBOL = 9;
    static final int COL_COUNTRY_TO_CODE = 10;
    static final int COL_COUNTRY_TO_NAME = 11;
    static final int COL_COUNTRY_TO_FLAG = 12;
    static final int COL_RATE_DATE = 13;
    static final int COL_RATE_VAL = 14;

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();
                Context context = DetailWidgetRemoteViewsService.this;

                Currency mainCurrency = Utility.getMainCurrency(context);

                if (mainCurrency == null)
                    return;

                Uri rateUri = ForexContract.RateEntry.buildStartCurrencyWithDate(
                        mainCurrency.getId(),
                        Utility.getForexSyncDate(context));

                data = getContentResolver().query(rateUri,
                        ForexContract.RATE_CURRENCY_COLUMNS,
                        null,
                        null,
                        null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }

                Context context = DetailWidgetRemoteViewsService.this;

                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_detail_list_item);

                String mainCurrencyId = data.getString(COL_CURRENCY_FROM_ID);
                String currencyId = data.getString(COL_CURRENCY_TO_ID);
                String currencyName = data.getString(COL_CURRENCY_TO_NAME);
                String currencySymbol = data.getString(COL_CURRENCY_TO_SYMBOL);
                String countryFlag = data.getString(COL_COUNTRY_TO_FLAG);
                String countryName = data.getString(COL_COUNTRY_TO_NAME);
                double currencyRate = data.getDouble(COL_RATE_VAL);

                String countryFlagDescription = Utility.formatCountryFlagName(context, countryName);
                String formattedCurrencyRate = Utility.formatCurrencyRate(context, currencySymbol, currencyRate);

                Bitmap countryFlagImage = null;
                try {
                    countryFlagImage = Glide.with(context)
                            .load(countryFlag)
                            .asBitmap()
                            .error(R.drawable.globe)
                            .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get();
                } catch (InterruptedException | ExecutionException e) {
                    Log.e(LOG_TAG, "Error retrieving flag from " + countryFlag, e);
                }

                views.setImageViewBitmap(R.id.widget_country_flag, countryFlagImage);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    setRemoteContentDescription(views, countryFlagDescription);
                }
                views.setTextViewText(R.id.widget_currency_id_textview, currencyId);
                views.setTextViewText(R.id.widget_currency_name_textview, currencyName);
                views.setTextViewText(R.id.widget_currency_rate_textview, formattedCurrencyRate);

                final Intent fillInIntent = new Intent();
                String[] currencies = {mainCurrencyId, currencyId};
                Uri rateUri = ForexContract.RateEntry.buildCurrencyRateWithValue(currencies, 1);
                fillInIntent.setData(rateUri);
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
                return views;
            }

            @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
            private void setRemoteContentDescription(RemoteViews views, String description) {
                views.setContentDescription(R.id.widget_country_flag, description);
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_detail_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(COL_RATE_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }

        };
    }
}
