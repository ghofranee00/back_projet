package com.iset.projet_integration.Entities;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "donations")
public class Donation {

    @Id
    private String id;

    // Le post créé par un needy
    @DBRef(lazy = false)
    private Post post;

    // Le donneur
    @DBRef(lazy = false)
    private User donor;

    private LocalDateTime dateDonation = LocalDateTime.now();

    private Categorie categorie;

    private String region;

    public Donation(String id, Post post, User donor, LocalDateTime dateDonation, Categorie categorie, String region, String details, List<String> images, StatutDonation status) {
        this.id = id;
        this.post = post;
        this.donor = donor;
        this.dateDonation = dateDonation;
        this.categorie = categorie;
        this.region = region;
        this.details = details;
        this.images = images;
        this.status = status;
    }

    // Champs flexibles selon catégorie
    private String details;

    // Images optionnelles
    private List<String> images;
    private StatutDonation status = StatutDonation.EN_ATTENTE;

    public StatutDonation getStatus() {
        return status;
    }

    public void setStatus(StatutDonation status) {
        this.status = status;
    }

    public enum Categorie {
        NOURRITURE,
        LOGEMENT,
        ARGENT,
        VETEMENT,
        EDUCATION,
        SANTE
    }
    public enum StatutDonation {
        EN_ATTENTE,
        ACCEPTEE,
        REFUSEE
    }
    public Donation() {}

    // Constructor complet


    public Donation(String details, String id, Post post, User donor, LocalDateTime dateDonation, Categorie categorie, String region, List<String> images, StatutDonation status) {
        this.details = details;
        this.id = id;
        this.post = post;
        this.donor = donor;
        this.dateDonation = dateDonation;
        this.categorie = categorie;
        this.region = region;
        this.images = images;
        this.status = status;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getDonor() {
        return donor;
    }

    public void setDonor(User donor) {
        this.donor = donor;
    }

    public LocalDateTime getDateDonation() {
        return dateDonation;
    }

    public void setDateDonation(LocalDateTime dateDonation) {
        this.dateDonation = dateDonation;
    }

    public Categorie getCategorie() {
        return categorie;
    }

    public void setCategorie(Categorie categorie) {
        this.categorie = categorie;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }
}