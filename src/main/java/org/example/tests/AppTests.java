package org.example.tests;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.example.Flight;

public class AppTests {

    private static int passed = 0;
    private static int failed = 0;
    private static final List<String> failMessages = new ArrayList<>();

    public static void main(String[] args) {
        run("хорошая погода на обоих концах -> по расписанию",  AppTests::goodFlightIsScheduled);
        run("плохая погода на вылете -> отменён",               AppTests::badDepartureCancels);
        run("плохая погода на прилёте -> отменён",              AppTests::badArrivalCancels);
        run("граница wind = 30 -> по расписанию",               () -> boundaryWind(30, true));
        run("граница wind = 31 -> отменён",                     () -> boundaryWind(31, false));
        run("граница visibility = 200 -> по расписанию",        () -> boundaryVis(200, true));
        run("граница visibility = 199 -> отменён",              () -> boundaryVis(199, false));
        run("часовые пояса: сдвиг +4 меняет час прилёта",       AppTests::tzPlusFour);
        run("часовые пояса: переход через 24",                  AppTests::tzCrossMidnight);
        run("часовые пояса: переход через 0",                   AppTests::tzCrossZero);
        run("часовые пояса: одинаковый пояс не сдвигает",       AppTests::tzSame);
        run("неизвестный город -> IllegalArgumentException",    AppTests::unknownCityThrows);
        run("нет прогноза для города -> NullPointerException",  AppTests::missingForecastThrows);

        System.out.println();
        System.out.println("========================================");
        System.out.printf("Пройдено: %d, Провалено: %d%n", passed, failed);
        if (!failMessages.isEmpty()) {
            System.out.println("Сообщения о провалах:");
            failMessages.forEach(m -> System.out.println("  - " + m));
        }
        if (failed > 0) System.exit(1);
    }

    // ---------- мини-раннер и ассерты ----------

    private static void run(String name, Runnable test) {
        try {
            test.run();
            System.out.println("[OK]   " + name);
            passed++;
        } catch (Throwable e) {
            System.out.println("[FAIL] " + name + " -> " + e.getMessage());
            failMessages.add(name + ": " + e);
            failed++;
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) throw new AssertionError(message);
    }

    private static void assertThrows(Class<? extends Throwable> expected,
                                     Runnable action, String message) {
        try {
            action.run();
        } catch (Throwable t) {
            if (expected.isInstance(t)) return;
            throw new AssertionError(message + " (было " + t.getClass().getSimpleName() + ")");
        }
        throw new AssertionError(message + " (исключения не было)");
    }

    // ---------- утилиты для построения JSON-прогноза ----------

    private static Map<String, Integer> cities(String... pairs) {
        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            result.put(pairs[i], Integer.parseInt(pairs[i + 1]));
        }
        return result;
    }

    private static JsonObject forecast(Map<String, List<int[]>> cityToRows) {
        JsonObjectBuilder root = Json.createObjectBuilder();
        for (Map.Entry<String, List<int[]>> e : cityToRows.entrySet()) {
            JsonArrayBuilder arr = Json.createArrayBuilder();
            for (int[] row : e.getValue()) {
                arr.add(Json.createObjectBuilder()
                        .add("time", row[0])
                        .add("wind", row[1])
                        .add("visibility", row[2]));
            }
            root.add(e.getKey(), arr);
        }
        return root.build();
    }

    /** 25 часов с одинаковыми значениями. */
    private static List<int[]> constant(int wind, int visibility) {
        List<int[]> rows = new ArrayList<>(25);
        for (int t = 0; t <= 24; t++) rows.add(new int[]{t, wind, visibility});
        return rows;
    }

    /** Все часы (10, 500), кроме hour — там подставляем (wind, visibility). */
    private static List<int[]> atHour(int hour, int wind, int visibility) {
        List<int[]> rows = new ArrayList<>(25);
        for (int t = 0; t <= 24; t++) {
            rows.add(t == hour ? new int[]{t, wind, visibility} : new int[]{t, 10, 500});
        }
        return rows;
    }

    private static Flight newFlight(int departure, String from, String to, int duration,
                                    JsonObject forecast, Map<String, Integer> utcOffsets) {
        return new Flight("TEST", departure, from, to, duration, forecast, utcOffsets);
    }

    /**
     * Flight.check() — private, поэтому перехватываем вывод print().
     * Если сделать check() package-private (одна строка в Flight.java),
     * хелпер можно удалить и писать assertTrue(f.check(), "...").
     */
    private static boolean isScheduled(Flight flight) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            flight.print();
        } finally {
            System.setOut(original);
        }
        return buffer.toString(StandardCharsets.UTF_8).contains("по расписанию");
    }

    // ---------- сами тесты ----------

    private static void goodFlightIsScheduled() {
        JsonObject fc = forecast(Map.of(
                "moscow",      constant(10, 500),
                "novosibirsk", constant(10, 500)));
        Flight f = newFlight(5, "moscow", "novosibirsk", 4, fc,
                cities("moscow", "3", "novosibirsk", "7"));
        assertTrue(isScheduled(f), "ожидался статус 'по расписанию'");
    }

    private static void badDepartureCancels() {
        JsonObject fc = forecast(Map.of(
                "moscow",      atHour(5, 50, 500),
                "novosibirsk", constant(10, 500)));
        Flight f = newFlight(5, "moscow", "novosibirsk", 4, fc,
                cities("moscow", "3", "novosibirsk", "7"));
        assertFalse(isScheduled(f), "ожидался статус 'отменён'");
    }

    private static void badArrivalCancels() {
        // прилёт = 5 + 4 + (7-3) = 13
        JsonObject fc = forecast(Map.of(
                "moscow",      constant(10, 500),
                "novosibirsk", atHour(13, 10, 50)));
        Flight f = newFlight(5, "moscow", "novosibirsk", 4, fc,
                cities("moscow", "3", "novosibirsk", "7"));
        assertFalse(isScheduled(f), "прилёт в 13ч, ожидался 'отменён'");
    }

    private static void boundaryWind(int wind, boolean expectedScheduled) {
        JsonObject fc = forecast(Map.of(
                "moscow", constant(wind, 500),
                "omsk",   constant(wind, 500)));
        Flight f = newFlight(5, "moscow", "omsk", 3, fc,
                cities("moscow", "3", "omsk", "6"));
        if (expectedScheduled) assertTrue (isScheduled(f), "wind=" + wind);
        else                   assertFalse(isScheduled(f), "wind=" + wind);
    }

    private static void boundaryVis(int visibility, boolean expectedScheduled) {
        JsonObject fc = forecast(Map.of(
                "moscow", constant(10, visibility),
                "omsk",   constant(10, visibility)));
        Flight f = newFlight(5, "moscow", "omsk", 3, fc,
                cities("moscow", "3", "omsk", "6"));
        if (expectedScheduled) assertTrue (isScheduled(f), "visibility=" + visibility);
        else                   assertFalse(isScheduled(f), "visibility=" + visibility);
    }

    private static void tzPlusFour() {
        // 5 + 4 + 4 = 13; если бы пояс не учитывался, смотрели бы 9-й час (хороший)
        JsonObject fc = forecast(Map.of(
                "moscow",      constant(10, 500),
                "novosibirsk", atHour(13, 10, 50)));
        Flight f = newFlight(5, "moscow", "novosibirsk", 4, fc,
                cities("moscow", "3", "novosibirsk", "7"));
        assertFalse(isScheduled(f), "должен проверяться 13-й час");
    }

    private static void tzCrossMidnight() {
        // 22 + 5 + 4 = 31 -> нормализуется в 7
        JsonObject fc = forecast(Map.of(
                "moscow",      atHour(22, 10, 500),
                "novosibirsk", atHour(7,  10, 50)));
        Flight f = newFlight(22, "moscow", "novosibirsk", 5, fc,
                cities("moscow", "3", "novosibirsk", "7"));
        assertFalse(isScheduled(f), "должен проверяться 7-й час");
    }

    private static void tzCrossZero() {
        // 1 + 1 + (3-7) = -2 -> нормализуется в 22
        JsonObject fc = forecast(Map.of(
                "novosibirsk", atHour(1,  10, 500),
                "moscow",      atHour(22, 10, 50)));
        Flight f = newFlight(1, "novosibirsk", "moscow", 1, fc,
                cities("moscow", "3", "novosibirsk", "7"));
        assertFalse(isScheduled(f), "должен проверяться 22-й час");
    }

    private static void tzSame() {
        // kazan(+3) -> moscow(+3): 5 + 1 = 6
        JsonObject fc = forecast(Map.of(
                "kazan",  constant(10, 500),
                "moscow", atHour(6, 10, 50)));
        Flight f = newFlight(5, "kazan", "moscow", 1, fc,
                cities("kazan", "3", "moscow", "3"));
        assertFalse(isScheduled(f), "должен проверяться 6-й час");
    }

    private static void unknownCityThrows() {
        JsonObject fc = forecast(Map.of("moscow", constant(10, 500)));
        Flight f = newFlight(5, "moscow", "atlantis", 4, fc,
                cities("moscow", "3"));
        assertThrows(IllegalArgumentException.class, f::print,
                "ожидался IllegalArgumentException для неизвестного города");
    }

    private static void missingForecastThrows() {
        JsonObject fc = forecast(Map.of("moscow", constant(10, 500)));
        Flight f = newFlight(5, "moscow", "omsk", 4, fc,
                cities("moscow", "3", "omsk", "6"));
        assertThrows(NullPointerException.class, f::print,
                "ожидался NullPointerException при отсутствии прогноза");
    }
}