package challenge.com.cameraiqweatherchallenge.Models;

public class Day {
    private String date;
    private String weekDay;
    private String icon;
    private String temp;
    private boolean isCel;

    public Day(String date, String weekDay, String icon, String temp){
        this.date = date;
        this.weekDay = weekDay;
        this.icon = icon;
        this.temp = temp;
        isCel = true;
    }

    public void convert(){
        isCel = !isCel;
    }

    public String getDate() {
        return date;
    }

    public String getIcon() {
        return icon;
    }

    public String getTemp() {
        double tempVal = Double.parseDouble(temp);
        if(!isCel){
            String tempString = String.valueOf(tempVal * 1.8 + 32);
            return tempString.substring(0, tempString.indexOf(".")+2);
        }
        return temp;
    }

    public String getWeekDay() {
        return weekDay;
    }
}
