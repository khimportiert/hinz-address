package service;

import model.RouteResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class RouteEncoder {
    public static String writeResponse(RouteResult result, BufferedWriter out) throws IOException {
        String distance = new DecimalFormat("00000.00").format(result.distanceKm());
        String duration = new DecimalFormat("00000").format((result.durationSec() + 59) / 60);

        String tmp = "";
        tmp += "#0FA";
        tmp += String.format("04%02X%s", result.id().length(), result.id()); // id
        tmp += String.format("1308%s", distance); // km 00000.00
        tmp += String.format("1405%s", duration); // min 00000

        out.write(0x02);
        out.write(tmp);
        out.write(0x03);

        return tmp;
    }
}
