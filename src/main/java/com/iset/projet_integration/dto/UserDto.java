package com.iset.projet_integration.dto;

public class UserDto {

    private String username;     // ⚠ OBLIGATOIRE POUR KEYCLOAK
    private String firstName;
    private String lastName;
    private String email;
    private String password;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public UserDto(String username, String firstName, String lastName, String email, String password, Role role, String phone, String identifiant) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.phone = phone;
        this.identifiant = identifiant;
    }

    private Role role;
    private String phone;
    private String identifiant;

    public enum Role {
        NEEDY,
        DONNATEUR,
        ASSOCIATION,
        ADMIN

        }

    // Getters / Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getIdentifiant() {
        return identifiant;
    }

    public void setIdentifiant(String identifiant) {
        this.identifiant = identifiant;
    }
}
