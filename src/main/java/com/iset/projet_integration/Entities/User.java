package com.iset.projet_integration.Entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document(collection = "users")
public class User {

    @Id
    private String id; // MongoDB utilise String pour l'ID
    private String firstName;
    private String lastName;

    private String phone;

    public String getPhone() {
        return phone;
    }

    public User(String id, String firstName, String lastName, String phone, String identifiant, String email, String password, String photoUrl, Role role, Date dateCreation, List<Demande> demandes, List<Notification> notificationsEnvoyees, List<Notification> notificationsRecues) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.identifiant = identifiant;
        this.email = email;
        this.password = password;
        this.photoUrl = photoUrl;
        this.role = role;
        this.dateCreation = dateCreation;
        this.demandes = demandes;
        this.notificationsEnvoyees = notificationsEnvoyees;
        this.notificationsRecues = notificationsRecues;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    private String identifiant;
    private String email;
    private String password;
    private String photoUrl;
    private Role role = Role.NEEDY;
    private Date dateCreation = new Date();

    // Références aux autres collections
    @DBRef(lazy = false)
    private List<Demande> demandes;

    @DBRef(lazy = false)
    private List<Notification> notificationsEnvoyees;

    @DBRef(lazy = false)
    private List<Notification> notificationsRecues;

    public enum Role {
        ADMIN,
        NEEDY,
        DONNATEUR,
        ASSOCIATION
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public User() {}

    public User(String id, String firstName, String lastName, String identifiant, String email, String password, String photoUrl, Role role, List<Demande> demandes, List<Notification> notificationsEnvoyees, List<Notification> notificationsRecues) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.identifiant = identifiant;
        this.email = email;
        this.password = password;
        this.photoUrl = photoUrl;
        this.role = role;
        this.demandes = demandes;
        this.notificationsEnvoyees = notificationsEnvoyees;
        this.notificationsRecues = notificationsRecues;
    }

    // Getters & Setters

    public Date getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Date dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getId() { return id; }
    public String getIdentifiant() { return identifiant; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getPhotoUrl() { return photoUrl; }
    public Role getRole() { return role; }
    public List<Demande> getDemandes() { return demandes; }
    public List<Notification> getNotificationsEnvoyees() { return notificationsEnvoyees; }
    public List<Notification> getNotificationsRecues() { return notificationsRecues; }

    public void setId(String id) { this.id = id; }
    public void setIdentifiant(String identifiant) { this.identifiant = identifiant; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public void setRole(Role role) { this.role = role; }
    public void setDemandes(List<Demande> demandes) { this.demandes = demandes; }
    public void setNotificationsEnvoyees(List<Notification> notificationsEnvoyees) { this.notificationsEnvoyees = notificationsEnvoyees; }
    public void setNotificationsRecues(List<Notification> notificationsRecues) { this.notificationsRecues = notificationsRecues; }
}