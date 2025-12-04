package com.iset.projet_integration.Entities;

import java.util.List;
import java.util.Map;

public class DonationRequest {
    private String postId;
    private String donorId;
    private Donation.Categorie categorie;
    private String region;
    private String details;
    private List<String> images;

    // ✅ NOUVEAU : Map pour les champs spécifiques par catégorie
    private Map<String, Object> champsSpecifiques;

    public DonationRequest() {}

    public DonationRequest(String postId, String donorId, Donation.Categorie categorie,
                           String region, String details, List<String> images,
                           Map<String, Object> champsSpecifiques) {
        this.postId = postId;
        this.donorId = donorId;
        this.categorie = categorie;
        this.region = region;
        this.details = details;
        this.images = images;
        this.champsSpecifiques = champsSpecifiques;
    }

    // Getters et Setters
    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getDonorId() {
        return donorId;
    }

    public void setDonorId(String donorId) {
        this.donorId = donorId;
    }

    public Donation.Categorie getCategorie() {
        return categorie;
    }

    public void setCategorie(Donation.Categorie categorie) {
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

    // ✅ NOUVEAU : Getter et Setter pour champsSpecifiques
    public Map<String, Object> getChampsSpecifiques() {
        return champsSpecifiques;
    }

    public void setChampsSpecifiques(Map<String, Object> champsSpecifiques) {
        this.champsSpecifiques = champsSpecifiques;
    }

    @Override
    public String toString() {
        return "DonationRequest{" +
                "postId='" + postId + '\'' +
                ", donorId='" + donorId + '\'' +
                ", categorie=" + categorie +
                ", region='" + region + '\'' +
                ", details='" + details + '\'' +
                ", images=" + images +
                ", champsSpecifiques=" + champsSpecifiques +
                '}';
    }
}