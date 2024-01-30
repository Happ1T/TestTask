package org.example;

import javax.json.*;
import java.io.FileReader;
import java.io.IOException;


public class Main {
    public static void main(String[] args) {
        JsonArray flights = null;
        JsonObject forecast;
        JsonObject jsonObject = null;
        try (JsonReader reader = Json.createReader(new FileReader("flights_and_forecast.json"))) {
            jsonObject = reader.readObject();

        } catch (IOException e) {
            System.out.println("Файл не найден");
        }
        if(jsonObject == null) {
            System.out.println("Файл пуст");
            return;
        }
        flights = jsonObject.getJsonArray("flights");
        forecast = jsonObject.getJsonObject("forecast");
        for(JsonValue i :flights )
        {
            Flight fly = new Flight(
                    i.asJsonObject().getString("no"),
                    i.asJsonObject().getInt("departure"),
                    i.asJsonObject().getString("from"),
                    i.asJsonObject().getString("to"),
                    i.asJsonObject().getInt("duration"),
                    forecast
            );
            fly.print();
        }


    }
}