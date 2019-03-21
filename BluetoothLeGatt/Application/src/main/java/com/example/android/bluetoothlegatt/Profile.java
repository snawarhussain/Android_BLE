package com.example.android.bluetoothlegatt;

public class Profile {

    private String rfid;
    private int age;
    private String breed;
    private int totalYield;

    public Profile()
    {}

    public Profile(String rfid, int age, String breed, int toalYield)
    {
        this.rfid = rfid;
        this.age = age;
        this.breed = breed;
        this.totalYield = totalYield;



    }
    public String getRfid()
    {

        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = rfid;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getTotalYield() {
        return totalYield;
    }

    public void setTotalYield(int totalYield) {
        this.totalYield = totalYield;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }
}
