package test;

import model.Address;
import model.ValidationResult;
import validation.AddressValidator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class TxtTester {
    public static void main(String[] args) {

        try (BufferedReader br = new BufferedReader(new FileReader("adressen.txt"))) {
            String zeile;
            while ((zeile = br.readLine()) != null) {
                testValidation(zeile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Google Count: " + AddressValidator.Google_Count);
    }

    private static void testValidation(String line) {
        String[] adr_comp = line.split(",");
        Address address = new Address("", "", adr_comp[2], adr_comp[3], adr_comp[0], adr_comp[1]);

        ValidationResult result = AddressValidator.validateWithBackup(address);

        // Ladeburger Str. 17, 16321 Bernau

        if (!result.isExactMatch()) {
            System.out.println(result.source() + ": " + address + " -> "  + result.possibleMatches().getFirst().formattedAddress());
        }
    }
}
