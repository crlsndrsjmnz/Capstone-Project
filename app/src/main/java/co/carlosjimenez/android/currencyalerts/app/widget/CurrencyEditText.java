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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.EditText;

import co.carlosjimenez.android.currencyalerts.app.R;

/**
 * An {@link EditText} subclass that shows a prefix, this case the currency symbol.
 */
public class CurrencyEditText extends EditText {

    private String mPrefix;
    private Rect mPrefixRect = new Rect(); // actual prefix size
    private int color;

    public CurrencyEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

        color = context.getResources().getColor(R.color.primary_text_main);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.CurrencyEditText,
                0, 0);
        try {
            mPrefix = a.getString(R.styleable.CurrencyEditText_prefix);
        } finally {
            a.recycle();
        }
    }

    public String getPrefix() {
        return mPrefix;
    }

    public void setPrefix(String prefix) {
        this.mPrefix = prefix;

        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mPrefix != null && mPrefix.length() > 0) {
            getPaint().setColor(color);
            getPaint().getTextBounds(mPrefix, 0, mPrefix.length(), mPrefixRect);
            mPrefixRect.right += getPaint().measureText(" "); // add some offset
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mPrefix != null && mPrefix.length() > 0) {
            canvas.drawText(mPrefix, super.getCompoundPaddingLeft(), getBaseline(), getPaint());
        }
        super.onDraw(canvas);
    }

    @Override
    public int getCompoundPaddingLeft() {
        return super.getCompoundPaddingLeft() + mPrefixRect.width();
    }

}
