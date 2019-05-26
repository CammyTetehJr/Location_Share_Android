package com.example.android2_project.model;

import android.location.Location;

public class User {

    private String firstname;
    private String lastname;
    private Location location;
    public String url;

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public Location getLocation() {
        return location;
    }

    public String getUrl() {
        return url;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public User(String firstname, String lastname, String url) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.location = null;
        this.url = url;
    }

    public User()
    {

    }
}
