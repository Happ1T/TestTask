package org.example;

import java.util.Map;

import javax.json.JsonObject;

import lombok.Getter;
import lombok.Setter;

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
    private JsonObject forecast;
    private Map<String, Integer> utcOffsets;

    public Flight(String no,
                  Integer departure,
                  String from,
                  String to,
                  Integer duration,
                  JsonObject forecast,
                  Map<String, Integer> utcOffsets) {
        this.departure = departure;
        this.no = no;
        this.from = from;
        this.to = to;
        this.duration = duration;
        this.forecast = forecast;
        this.utcOffsets = utcOffsets;
    }

    public void print() {
        String status = check() ? "по расписанию" : "отменён";
        String output = String.format("%-10s | %-12s -> %-12s | %s", no, from, to, status);
        System.out.println(output);
    }

    private boolean check() {
        return checkFrom() && checkTo();
    }

    private boolean checkFrom() {
        return getSmf(from, "wind", departure) <= MAX_WIND
                && getSmf(from, "visibility", departure) >= MAX_VISIBILITY;
    }

    private boolean checkTo() {
        int arrivalLocalTime = departure + duration + diff(from, to);

        return getSmf(to, "wind", arrivalLocalTime) <= MAX_WIND
                && getSmf(to, "visibility", arrivalLocalTime) >= MAX_VISIBILITY;
    }

    private Integer getSmf(String city, String smf, Integer time) {
        int hour = normalizeHour(time);
        return forecast.get(city)
                .asJsonArray()
                .get(hour)
                .asJsonObject()
                .getInt(smf);
    }

    private int normalizeHour(int time) {
        if (time >= 0 && time <= 24) {
            return time;
        }

        int hour = time % 24;
        return hour < 0 ? hour + 24 : hour;
    }

    private int diff(String from, String to) {
        return getUtcOffset(to) - getUtcOffset(from);
    }

    private int getUtcOffset(String city) {
        Integer offset = utcOffsets.get(city);

        if (offset == null) {
            throw new IllegalArgumentException("Не задан часовой пояс для города: " + city);
        }

        return offset;
    }
}