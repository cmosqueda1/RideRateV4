package com.chris.uberratebanner;
public class RideRecord {
    public long id, createdAt;
    public double payout, hourly;
    public int pickupMinutes, tripMinutes, totalMinutes, baseTotalMinutes, deliveryBufferMinutes;
    public int stopCount, stopBufferMinutes;
    public String type, source, thumbPath, ocrText;
    public boolean archived;
}
