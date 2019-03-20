package com.example.android.bluetoothlegatt;

import android.text.format.Time;

import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;

public class MilkingSession {

    private long Start_Time ;
    private Date End_Time;
    private double Total_Litters;
    // SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
    //  String currentDateandTime = sdf.format(new Date());
    public MilkingSession()
    {}

    public MilkingSession(long Start_Time, Date End_Time, double Litters )
    {

        this.Start_Time = Start_Time;
        this.End_Time = End_Time;
        this.Total_Litters = Litters;


    }
    public long getStart_Time() {
        return Start_Time;
    }

    public void setStart_Time(long Start_Time) {
        this.Start_Time = Start_Time;
    }

    public Date getEnd_Time() {
        return End_Time;
    }

    public void setEnd_Time(Date End_Time) {
        this.End_Time = End_Time;
    }

    public double getTotal_Litters() {
        return Total_Litters;
    }

    public void setTotal_Litters(double Total_Litters) {
        this.Total_Litters = Total_Litters;
    }




}


