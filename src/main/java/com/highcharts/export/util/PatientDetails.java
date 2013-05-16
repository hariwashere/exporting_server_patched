package com.highcharts.export.util;

public class PatientDetails {
    String patientName;
    String dob;
    String city;
    String state;

    public PatientDetails(String patientName, String dob, String city, String state) {
        this.patientName = patientName;
        this.dob = dob;
        this.city = city;
        this.state = state;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getDob() {
        return dob;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }
}
