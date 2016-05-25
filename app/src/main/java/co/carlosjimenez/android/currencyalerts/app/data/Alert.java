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

/**
 * Class that encapsulates the Alert sent to the user
 */
public class Alert {

    private boolean enabled;
    private Currency currencyFrom;
    private Currency currencyTo;
    private int period;
    private float fluctuation;
    private boolean clearAverage;
    private boolean sendNotifications;
    private double currentRate;
    private double rateAverage;
    private boolean positiveFluctuation;

    public Alert() {

    }

    public Alert(boolean enabled, Currency currencyFrom, Currency currencyTo, int period, float fluctuation, boolean clearAverage) {
        this.enabled = enabled;
        this.currencyFrom = currencyFrom;
        this.currencyTo = currencyTo;
        this.period = period;
        this.fluctuation = fluctuation;
        this.clearAverage = clearAverage;
    }

    public Alert(boolean enabled, Currency currencyFrom, Currency currencyTo, int period, float fluctuation, boolean clearAverage, boolean sendNotifications, double rateAverage) {
        this.enabled = enabled;
        this.currencyFrom = currencyFrom;
        this.currencyTo = currencyTo;
        this.period = period;
        this.fluctuation = fluctuation;
        this.clearAverage = clearAverage;
        this.sendNotifications = sendNotifications;
        this.rateAverage = rateAverage;
    }

    @Override
    public String toString() {
        return "Alert{" +
                "enabled=" + enabled +
                ", currencyFrom=" + currencyFrom +
                ", currencyTo=" + currencyTo +
                ", period=" + period +
                ", fluctuation=" + fluctuation +
                ", clearAverage=" + clearAverage +
                ", sendNotifications=" + sendNotifications +
                ", currentRate=" + currentRate +
                ", rateAverage=" + rateAverage +
                '}';
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Currency getCurrencyFrom() {
        return currencyFrom;
    }

    public void setCurrencyFrom(Currency currencyFrom) {
        this.currencyFrom = currencyFrom;
    }

    public Currency getCurrencyTo() {
        return currencyTo;
    }

    public void setCurrencyTo(Currency currencyTo) {
        this.currencyTo = currencyTo;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public float getFluctuation() {
        return fluctuation;
    }

    public void setFluctuation(float fluctuation) {
        this.fluctuation = fluctuation;
    }

    public boolean isClearAverage() {
        return clearAverage;
    }

    public void clearAverage() {
        this.clearAverage = true;
    }

    public boolean sendNotifications() {
        return sendNotifications;
    }

    public void setSendNotifications(boolean sendNotifications) {
        this.sendNotifications = sendNotifications;
    }

    public double getCurrentRate() {
        return currentRate;
    }

    public void setCurrentRate(double currentRate) {
        this.currentRate = currentRate;
    }

    public double getRateAverage() {
        return rateAverage;
    }

    public void setRateAverage(double rateAverage) {
        this.rateAverage = rateAverage;
    }

    public double getCurrentFluctuation() {
        double returnValue = -1;
        positiveFluctuation = false;
        if (currentRate > 0 && rateAverage > 0) {
            returnValue = 100 - ((currentRate * 100) / rateAverage);

            if (returnValue < 0) {
                returnValue = returnValue * -1;
                positiveFluctuation = true;
            }
        }
        return returnValue;
    }

    public boolean isPositiveFluctuation() {
        return positiveFluctuation;
    }

}
