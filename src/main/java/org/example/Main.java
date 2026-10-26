package org.example;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

public class Main {
    public static void main(String[] args) {
        JsonArray flights;
        JsonObject forecast;
        JsonObject jsonObject = null;

        try (JsonReader reader = Json.createReader(new FileReader("test_prog_FaF.json"))) {
            jsonObject = reader.readObject();
        } catch (IOException e) {
            System.out.println("Файл не найден");
        }

        if (jsonObject == null) {
            System.out.println("Файл пуст");
            return;
        }

        flights = jsonObject.getJsonArray("flights");
        forecast = jsonObject.getJsonObject("forecast");

        Map<String, Integer> utcOffsets = new HashMap<>();
        JsonArray cities = jsonObject.getJsonArray("cities");

        for (JsonValue cityValue : cities) {
            JsonObject city = cityValue.asJsonObject();
            utcOffsets.put(
                    city.getString("name"),
                    city.getInt("utcOffset")
            );
        }

        for (JsonValue i : flights) {
            Flight fly = new Flight(
                    i.asJsonObject().getString("no"),
                    i.asJsonObject().getInt("departure"),
                    i.asJsonObject().getString("from"),
                    i.asJsonObject().getString("to"),
                    i.asJsonObject().getInt("duration"),
                    forecast,
                    utcOffsets
            );

            fly.print();
        }
    }
}