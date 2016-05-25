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
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

/**
 * {@link ForexAdapter} exposes a list of currency rates
 * from a {@link android.database.Cursor} to a {@link android.support.v7.widget.RecyclerView}.
 */
public class ForexAdapter extends RecyclerView.Adapter<ForexAdapter.ForexAdapterViewHolder> {

    private static final String LOG_TAG = ForexAdapter.class.getSimpleName();

    private static final int VIEW_TYPE_DAY = 0;

    final private Context mContext;
    final private ForexAdapterOnClickHandler mClickHandler;
    final private View mEmptyView;

    private double mMainAmount = 1;
    private double mMaxRateValue;
    private String mMaxRateSymbol;
    private String mMaxRateString;

    private Cursor mCursor;

    public ForexAdapter(Context context, ForexAdapterOnClickHandler dh, View emptyView) {
        mContext = context;
        mClickHandler = dh;
        mEmptyView = emptyView;
    }

    /*
        This takes advantage of the fact that the viewGroup passed to onCreateViewHolder is the
        RecyclerView that will be used to contain the view, so that it can get the current
        ItemSelectionManager from the view.

        One could implement this pattern without modifying RecyclerView by taking advantage
        of the view tag to store the ItemChoiceManager.
     */
    @Override
    public ForexAdapterViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewGroup instanceof RecyclerView) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_currencies, viewGroup, false);
            view.setFocusable(true);
            return new ForexAdapterViewHolder(view);
        } else {
            throw new RuntimeException("Not bound to RecyclerView");
        }
    }

    @Override
    public void onBindViewHolder(ForexAdapterViewHolder forexAdapterViewHolder, int position) {
        mCursor.moveToPosition(position);

        String currencyId = mCursor.getString(MainActivityFragment.COL_CURRENCY_TO_ID);
        String currencyName = mCursor.getString(MainActivityFragment.COL_CURRENCY_TO_NAME);
        String currencySymbol = mCursor.getString(MainActivityFragment.COL_CURRENCY_TO_SYMBOL);
        String countryFlag = mCursor.getString(MainActivityFragment.COL_COUNTRY_TO_FLAG);
        String countryName = mCursor.getString(MainActivityFragment.COL_COUNTRY_TO_NAME);
        double currencyRate = mCursor.getDouble(MainActivityFragment.COL_RATE_VAL);

        Glide.with(mContext)
                .load(countryFlag)
                .error(R.drawable.globe)
                .centerCrop()
                .crossFade()
                .into(forexAdapterViewHolder.mIconView);
        forexAdapterViewHolder.mIconView.setContentDescription(Utility.formatCountryFlagName(mContext, countryName));

        // Find TextView and set the name on it
        forexAdapterViewHolder.mCurrencyNameView.setText(currencyName);
        forexAdapterViewHolder.mCurrencyNameView.setContentDescription(currencyName);

        // Find TextView and set weather forecast on it
        forexAdapterViewHolder.mCurrencyIdView.setText(currencyId);
        forexAdapterViewHolder.mCurrencyIdView.setContentDescription(currencyId + " " + currencyName);

        // Read high temperature from cursor
        currencyRate = mMainAmount * currencyRate;
        String rateString = Utility.formatCurrencyRate(mContext, currencySymbol, currencyRate);
        forexAdapterViewHolder.mCurrencyRateView.setText(rateString);
        forexAdapterViewHolder.mCurrencyRateView.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                Utility.getRateFontPXSize(mMaxRateString.length()));
        forexAdapterViewHolder.mCurrencyRateView.setContentDescription(rateString);
    }

    public void setMaxRateVal(String maxRateSymbol, double maxRateValue) {
        this.mMaxRateSymbol = maxRateSymbol;
        this.mMaxRateValue = maxRateValue;

        this.mMaxRateString = Utility.formatCurrencyRate(mContext, this.mMaxRateSymbol, mMaxRateValue);
    }

    public void setMainAmount(double mainAmount) {
        this.mMainAmount = mainAmount;

        double calculatedMaxRate = mainAmount * this.mMaxRateValue;
        this.mMaxRateString = Utility.formatCurrencyRate(mContext, this.mMaxRateSymbol, calculatedMaxRate);

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return VIEW_TYPE_DAY;
    }

    @Override
    public int getItemCount() {
        if (null == mCursor) return 0;
        return mCursor.getCount();
    }

    public void swapCursor(Cursor newCursor) {
        mCursor = newCursor;
        notifyDataSetChanged();
        mEmptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    public Cursor getCursor() {
        return mCursor;
    }
    
    public interface ForexAdapterOnClickHandler {
        void onClick(String currencyId, String currencyName, ForexAdapterViewHolder vh);
    }

    /**
     * Cache of the children views for a forecast list item.
     */
    public class ForexAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final ImageView mIconView;
        public final TextView mCurrencyNameView;
        public final TextView mCurrencyIdView;
        public final TextView mCurrencyRateView;

        public ForexAdapterViewHolder(View view) {
            super(view);
            mIconView = (ImageView) view.findViewById(R.id.list_item_country_flag);
            mCurrencyNameView = (TextView) view.findViewById(R.id.list_item_currency_name_textview);
            mCurrencyIdView = (TextView) view.findViewById(R.id.list_item_currency_id_textview);
            mCurrencyRateView = (TextView) view.findViewById(R.id.list_item_currency_rate_textview);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            mCursor.moveToPosition(adapterPosition);
            String currencyId = mCursor.getString(MainActivityFragment.COL_CURRENCY_TO_ID);
            String currencyName = mCursor.getString(MainActivityFragment.COL_CURRENCY_TO_NAME);
            mClickHandler.onClick(currencyId, currencyName, this);
        }
    }
}