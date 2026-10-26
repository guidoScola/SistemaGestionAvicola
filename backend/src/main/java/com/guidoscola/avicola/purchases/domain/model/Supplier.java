package com.guidoscola.avicola.purchases.domain.model;

import com.guidoscola.avicola.shared.domain.InvalidInputException;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public class Supplier {

    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private final UUID id;
    private String businessName;
    private Cuit cuit;
    private VatCondition vatCondition;
    private String address;
    private String city;
    private String province;
    private String phone;
    private String email;
    private boolean active;

    public Supplier(UUID id, String businessName, Cuit cuit, VatCondition vatCondition, String address, String city,
                    String province, String phone, String email, boolean active) {
        this.id = Objects.requireNonNull(id);
        this.active = active;
        update(businessName, cuit, vatCondition, address, city, province, phone, email);
    }

    public static Supplier create(String businessName, Cuit cuit, VatCondition vatCondition, String address,
                                  String city, String province, String phone, String email) {
        return new Supplier(UUID.randomUUID(), businessName, cuit, vatCondition, address, city, province, phone, email,
                true);
    }

    public void update(String businessName, Cuit cuit, VatCondition vatCondition, String address, String city,
                       String province, String phone, String email) {
        this.businessName = required(businessName, "Business name", 150);
        this.cuit = Objects.requireNonNull(cuit);
        if (vatCondition == null) {
            throw new InvalidInputException("VAT condition is required");
        }
        this.vatCondition = vatCondition;
        this.address = optional(address, "Address", 150);
        this.city = optional(city, "City", 100);
        this.province = optional(province, "Province", 100);
        this.phone = optional(phone, "Phone", 50);
        this.email = optional(email, "Email", 150);
        if (this.email != null && !EMAIL.matcher(this.email).matches()) {
            throw new InvalidInputException("Invalid email");
        }
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    private static String required(String value, String field, int maxLength) {
        String trimmed = optional(value, field, maxLength);
        if (trimmed == null) {
            throw new InvalidInputException(field + " is required");
        }
        return trimmed;
    }

    private static String optional(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new InvalidInputException(field + " must be at most " + maxLength + " characters");
        }
        return trimmed;
    }

    public UUID id() {
        return id;
    }

    public String businessName() {
        return businessName;
    }

    public Cuit cuit() {
        return cuit;
    }

    public VatCondition vatCondition() {
        return vatCondition;
    }

    public String address() {
        return address;
    }

    public String city() {
        return city;
    }

    public String province() {
        return province;
    }

    public String phone() {
        return phone;
    }

    public String email() {
        return email;
    }

    public boolean isActive() {
        return active;
    }
}
