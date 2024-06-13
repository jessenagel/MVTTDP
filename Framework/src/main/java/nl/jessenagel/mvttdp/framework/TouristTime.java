package nl.jessenagel.mvttdp.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TouristTime {
    public int day, hour, minute;

    public TouristTime(String timeString) {
        this.parseString(timeString);
    }

    public static List<TouristTime> getAllTimesBetween(TouristTime openTime, TouristTime closeTime) {
        if(TouristTime.greater(openTime, closeTime)){
            System.err.println("Requesting times with start time greater than close time");
            return new ArrayList<>();
        }
        List<TouristTime> times = new ArrayList<>();
        int currentTimeInMinutes = openTime.toMinutes();
        while(currentTimeInMinutes <= closeTime.toMinutes()){
            TouristTime nextTime = new TouristTime("0:0:"+ currentTimeInMinutes);
            nextTime.rebalance();
            times.add(nextTime);
            currentTimeInMinutes +=1;
        }
        return times;
    }

    private void parseString(String timeString) {
        boolean minuteSet, hourSet, daySet;
        minuteSet = hourSet = daySet = false;
        String reverseString = new StringBuilder(timeString).reverse().toString();
        Scanner reverseStringScanner = new Scanner(reverseString);
        reverseStringScanner.useDelimiter(":");
        while (reverseStringScanner.hasNext()) {
            if (!minuteSet) {
                minuteSet = true;
                this.minute = Integer.parseInt(new StringBuilder(reverseStringScanner.next()).reverse().toString());
            } else if (!hourSet) {
                hourSet = true;
                this.hour = Integer.parseInt(new StringBuilder(reverseStringScanner.next()).reverse().toString());
            } else if (!daySet) {
                daySet = true;
                this.day = Integer.parseInt(new StringBuilder(reverseStringScanner.next()).reverse().toString());
            }
        }
        if (!minuteSet) {
            this.minute = 0;
        }
        if (!hourSet) {
            this.hour = 0;
        }
        if (!daySet) {
            this.day = 0;
        }
        this.rebalance();
    }

    public void rebalance() {
        if (this.minute < 0) {
            this.hour -= 1;
            this.minute = 60 + this.minute;
        }
        if (this.minute >= 60) {
            this.hour += (int) Math.floor((double) this.minute / 60);
            this.minute = this.minute % 60;
        }
        if (this.hour >= 24) {
            this.day += (int) Math.floor((double) this.hour / 24);
            this.hour = this.hour % 24;
        }
    }

    public TouristTime copy(){
        return new TouristTime(this.day + ":" + this.hour + ":" + this.minute);
    }

    public TouristTime increaseBy(TouristTime touristTime){
        TouristTime output = this.copy();
        output.day += touristTime.day;
        output.hour += touristTime.hour;
        output.minute += touristTime.minute;
        output.rebalance();
        return output;
    }
    public void print() {
        System.out.println(this.day + ":" + this.hour + ":" + this.minute);
    }

    public static boolean leq(TouristTime one, TouristTime two) {
        if (one.day < two.day){
            return true;
        }
        if (one.day > two.day){
            return false;
        }

        if (one.hour < two.hour){
            return true;
        }
        if (one.hour > two.hour){
            return false;
        }

        if (one.minute < two.minute){
            return true;
        }
        return one.minute <= two.minute;
    }

    public static boolean geq(TouristTime one, TouristTime two) {
        if (one.day > two.day){
            return true;
        }
        if (one.day < two.day){
            return false;
        }

        if (one.hour > two.hour){
            return true;
        }
        if (one.hour < two.hour){
            return false;
        }

        if (one.minute > two.minute){
            return true;
        }
        return one.minute >= two.minute;
    }

    public static boolean greater(TouristTime one, TouristTime two) {
        if (one.day > two.day){
            return true;
        }
        if (one.day < two.day){
            return false;
        }

        if (one.hour > two.hour){
            return true;
        }
        if (one.hour < two.hour){
            return false;
        }

        return one.minute > two.minute;
    }
    public static boolean less(TouristTime one, TouristTime two) {
        if (one.day < two.day){
            return true;
        }
        if (one.day > two.day){
            return false;
        }

        if (one.hour < two.hour){
            return true;
        }
        if (one.hour > two.hour){
            return false;
        }

        return one.minute < two.minute;
    }

    public int toMinutes() {
        return 1440 * this.day + 60 * this.hour + this.minute;
    }

    @Override
    public String toString(){
        return this.day + ":" + this.hour + ":"+this.minute;
    }

    public static TouristTime difference(TouristTime one, TouristTime two){
        TouristTime diff = new TouristTime("0:0:0");
        diff.hour = one.hour - two.hour;
        diff.day = one.day - two.day;
        diff.minute = one.minute - two.minute;
        if(diff.minute < 0){
            diff.minute = 60 +diff.minute;
            diff.hour--;
        }
        diff.rebalance();
        return diff;
    }

    public TouristTime decreaseBy(TouristTime touristTime) {
        TouristTime output = this.copy();
        output.day -= touristTime.day;
        output.hour -= touristTime.hour;
        output.minute -= touristTime.minute;
        output.rebalance();
        return output;
    }
}
