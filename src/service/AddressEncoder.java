package service;

import model.AddressMatch;
import model.ValidationResult;

import java.io.BufferedWriter;
import java.io.IOException;

public class AddressEncoder {
    public static void writeSingleMatchTcpResponse(ValidationResult result, BufferedWriter out) throws IOException {
        AddressMatch exactMatch = result.possibleMatches().getFirst();

        out.write("EXAKTER MATCH GEFUNDEN:");
//        out.newLine();
//        out.write(result.source());
//        out.newLine();
//        out.write(String.format("Adresse: %s", exactMatch.formattedAddress()));
//        out.newLine();
//        out.write(String.format("Koordinaten: %.6f, %.6f", exactMatch.lat(), exactMatch.lng()));
//        out.newLine();
    }

    public static void writeMultiMatchTcpResponse(ValidationResult result, BufferedWriter out) throws IOException {
        out.write("KEIN EXAKTER MATCH. MÖGLICHE ADRESSEN:");
        out.newLine();
        out.write(result.source());
        out.newLine();
        for (AddressMatch match : result.possibleMatches()) {
            out.write(String.format("- %s (%.6f, %.6f)",
                    match.formattedAddress(),
                    match.lat(),
                    match.lng()));
            out.newLine();
        }
    }

    public static void writeNoMatchTcpResponse(ValidationResult result, BufferedWriter out) throws IOException {
        out.write("KEINE MÖGLICHEN ADRESSEN GEFUNDEN.");
//        out.newLine();
//        out.write(result.source());
//        out.newLine();
    }
}
