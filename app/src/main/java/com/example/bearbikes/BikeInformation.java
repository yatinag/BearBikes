package com.example.bearbikes;

public class BikeInformation {

    private String id;
    private Double latitude;
    private Double longitude;
    private String origin;
    private String color;

    public BikeInformation(){
        this.id = "1234";
        this.latitude = 40.0232297;
        this.longitude = -75.2564858;
        this.origin = "Unknown";
        this.color = "blue";
    }

    public BikeInformation(String id, double latitude, double longitude, String origin, String color){
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.origin = origin;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
