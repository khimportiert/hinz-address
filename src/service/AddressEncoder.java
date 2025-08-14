package service;

import config.AppConfig;
import model.AddressMatch;
import model.ValidationResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.stream.Collectors;

public class AddressEncoder {
    public static String writeSingleMatchTcpResponse(ValidationResult result, BufferedWriter out) throws IOException {
        AddressMatch match = result.possibleMatches().getFirst();

        String lng = new DecimalFormat("000.000000").format(match.lng());
        String lat = new DecimalFormat("000.000000").format(match.lat());

        String tmp = "";
        tmp += "#0EA";
        tmp += String.format("04%02X%s", result.requestedAddress().id().length(), result.requestedAddress().id());

        if (match.isExactMatch() || !AppConfig.useDummy()) {
            tmp += String.format("0E%02X%s", 4, "0101"); // n vom m
            tmp += String.format("03%02X%s", match.formattedAddress().length(), match.formattedAddress()); // txt
            tmp += String.format("0715%s;%s", lng, lat); // lon;lat
        }
        else {
            tmp += String.format("0E%02X%s", 4, "0102"); // n vom m
            tmp += String.format("03%02X%s", match.formattedAddress().length(), match.formattedAddress()); // txt
            tmp += String.format("0715%s;%s", lng, lat); // lon;lat

            tmp += String.format("0E%02X%s", 4, "0202"); // n vom m
            tmp += String.format("03%02X%s", "Bitte nicht anklicken".length(), "Bitte nicht anklicken"); // txt
            tmp += String.format("0715%s;%s", "013.408333", "052.518611"); // lon;lat von Berlin
        }

        out.write(0x02);
        out.write(tmp);
        out.write(0x03);

        return tmp;
    }

    public static String writeMultiMatchTcpResponse(ValidationResult result, BufferedWriter out) throws IOException {
        out.write(0x02);

        StringBuilder msg = new StringBuilder();
        msg.append("#0EA");
        msg.append(String.format("04%02X%s", result.requestedAddress().id().length(), result.requestedAddress().id()));

        List<AddressMatch> possibleMatches = result.possibleMatches().stream()
                .distinct()
                .toList();

        int m = possibleMatches.size();

        for (int n = 0; n < m; n++) {
            AddressMatch match = possibleMatches.get(n);

            String lng = new DecimalFormat("000.000000").format(match.lng());
            String lat = new DecimalFormat("000.000000").format(match.lat());

            String n_von_m = new DecimalFormat("00").format(n + 1)
                    + new DecimalFormat("00").format(m) ; // 0101 (1 von 1)

            String res = "";

            res += String.format("0E%02X%s", 4, n_von_m); // n vom m


            if (match.name().isBlank()) {
                res += String.format("03%02X%s", match.formattedAddress().length(), match.formattedAddress()); // txt
            } else {
                res += String.format("03%02X%s, %s", (match.formattedAddress() + ", " + match.name()).length(), match.formattedAddress(), match.name()); // txt
            }

            res += String.format("0715%s;%s", lng, lat); // lon;lat

            if (msg.length() + res.length() < 1022) {
                msg.append(res);
            }
        }

        out.write(msg.toString());
        out.write(0x03);

        return msg.toString();
    }

    public static String writeNoMatchTcpResponse(ValidationResult result, BufferedWriter out) throws IOException {
        String tmp = "#0EA" + String.format("04%02X%s", result.requestedAddress().id().length(), result.requestedAddress().id()) + "0E040000";

        out.write(0x02);
        out.write(tmp);
        out.write(0x03);

        return tmp;
    }
}
