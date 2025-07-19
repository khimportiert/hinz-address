package service;

import model.AddressMatch;
import model.ValidationResult;

import java.io.BufferedWriter;
import java.io.IOException;

public class AddressEncoder {
    public static void writeSingleMatchTcpResponse(ValidationResult result, BufferedWriter out) throws IOException {
        throw new IOException();
    }

    public static void writeMultiMatchTcpResponse(ValidationResult result, BufferedWriter out) throws IOException {
        out.write("#0EA");
        out.write(String.format("04%02X%s", result.requestedAddress().id().length(), result.requestedAddress().id()));

        int m = result.possibleMatches().size();
        for (int n = 0; n < m; n++) {
            AddressMatch match = result.possibleMatches().get(n);

            out.write(String.format("0E%02X%s%s", 4, n + 1, m)); // n vom m
            out.write(String.format("03%02X%s", match.formattedAddress().length(), match.formattedAddress())); // txt
            out.write(String.format("0715%09.6f;%09.6f", match.lng(), match.lat())); // lon;lat

        }
    }

    public static void writeNoMatchTcpResponse(ValidationResult result, BufferedWriter out) throws IOException {
        out.write("#0EA");
        out.write(String.format("04%02X%s", result.requestedAddress().id().length(), result.requestedAddress().id()));

        out.write("0E040000"); // 0000
    }
}
