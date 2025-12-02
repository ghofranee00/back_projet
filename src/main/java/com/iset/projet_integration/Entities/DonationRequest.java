package com.iset.projet_integration.Entities;

import java.util.List;

public class DonationRequest {

    private String postId;
    private String donorId;
    private Donation.Categorie categorie;
    private String region;
    private String details;
    private List<String> images;

    public DonationRequest() {}

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



    public List<String> getImages() {
        return images;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public DonationRequest(String postId, String donorId, Donation.Categorie categorie, String region, String details, List<String> images) {
        postId = postId;
        this.donorId = donorId;
        this.categorie = categorie;
        this.region = region;
        this.details = details;
        this.images = images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }
}