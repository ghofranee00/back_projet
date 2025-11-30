package com.iset.projet_integration.Repository;

import com.iset.projet_integration.Entities.Donation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DonationRepository extends MongoRepository<Donation, String> {}