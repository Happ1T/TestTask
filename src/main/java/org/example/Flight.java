package org.example;

import lombok.Getter;
import lombok.Setter;

import javax.json.JsonObject;

@Setter
@Getter
public class Flight {
    private static final Integer MAX_WIND = 30;
    private static final Integer MAX_VISIBILITY = 200;
    private String no;
    private Integer departure;
    private String from;
    private String to;
    private Integer duration;
    JsonObject forecast;
    public Flight(String no,
                  Integer departure,
                  String from,
                  String to,
                  Integer duration,
                  JsonObject forecast
    ){
        this.departure = departure;
        this.no = no;
        this.from = from;
        this.to = to;
        this.duration = duration;
        this.forecast = forecast;
    }

    public void print(){
        String status = check() ? "по расписанию" : "отменён";
        String output = String.format("%-10s | %-12s -> %-12s | %s", no, from, to, status);
        System.out.println(output);
    }
    private boolean check(){
        return checkFrom() && checkTo();
    }
    private boolean checkFrom(){
        return getSmf(from,"wind", departure) <= MAX_WIND
                &&  getSmf( from,"visibility", departure) >= MAX_VISIBILITY;
    }
    private boolean checkTo(){
        return getSmf(to,"wind", departure+duration) <= MAX_WIND
                &&  getSmf(to,"visibility", departure+duration) >= MAX_VISIBILITY;
    }
    private Integer getSmf( String city, String smf, Integer time){
        return forecast.get(city).asJsonArray().get(time).asJsonObject().getInt(smf);
    }
}
