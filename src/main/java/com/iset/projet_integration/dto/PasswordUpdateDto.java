package com.iset.projet_integration.dto;

public class PasswordUpdateDto {
    private String currentPassword;
    private String newPassword;

    // Getters et Setters
    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}