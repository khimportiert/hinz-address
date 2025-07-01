package model;

public record Address(
        String plz,
        String city,
        String street,
        String houseNumber
) {
    public String toString() {
        return String.format("%s %s, %s %s", street, houseNumber, plz, city);
    }
}