package com.example.android.bluetoothlegatt;


public class Data {


    private String Tag ;
    private String Litters;
    private String Total_Milking_time;
    private String FlowRate;


    public Data()
    {}

    public  Data(String data)

    {
        this.Tag  = data.substring(0,2);
        this.Litters = data.substring(3,5);
        this.Total_Milking_time = data.substring(8,9);
        this.FlowRate = data.substring(13);

    }
    public String getTag() {
        return Tag;
    }

    public void setTag(String Tag) {
        this.Tag = Tag;
    }

    public String getLitters() {
        return Litters;
    }

    public void setLitters(String Litters) {
        this.Litters = Litters;
    }

    public String getTotal_Milking_time() {
        return Total_Milking_time;
    }

    public void setTotal_Milking_time(String Total_Milking) {
        this.Total_Milking_time = Total_Milking_time;
    }
    public String getFlowRate() {
        return FlowRate;
    }

    public void setFlowRate(String Flow) {
        this.FlowRate = Flow;

    }

}