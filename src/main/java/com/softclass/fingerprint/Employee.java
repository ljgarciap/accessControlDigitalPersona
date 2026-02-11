package com.softclass.fingerprint;

public class Employee {
    public int id;
    public String name;
    public String document;
    public String fingerprintBase64;
    public boolean active = true;

    @Override
    public String toString() {
        return name + " (" + document + ")";
    }
}
