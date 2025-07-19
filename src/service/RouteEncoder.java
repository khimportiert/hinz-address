package service;

import model.RouteResult;

import java.io.BufferedWriter;
import java.io.IOException;

public class RouteEncoder {
    public static void writeResponse(RouteResult result, BufferedWriter out) throws IOException {
        out.write("#0FA");
        out.write(String.format("04%02X%s", result.id().length(), result.id())); // id
        out.write(String.format("1308%08.2f", result.distanceKm())); // km 00000.00
        out.write(String.format("1405%05d", (result.durationSec() + 59) / 60)); // min 00000
    }
}
