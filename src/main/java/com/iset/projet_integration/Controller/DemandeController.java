package com.iset.projet_integration.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iset.projet_integration.Entities.Demande;
import com.iset.projet_integration.Entities.Notification;
import com.iset.projet_integration.Entities.User;
import com.iset.projet_integration.Repository.NotificationRepository;
import com.iset.projet_integration.Repository.PostRepository;
import com.iset.projet_integration.Repository.UserRepository;
import com.iset.projet_integration.Service.DemandeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demandes")
@CrossOrigin(origins = "*")
public class DemandeController {

    private final DemandeService demandeService;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DemandeController(DemandeService demandeService,
                             UserRepository userRepository,
                             PostRepository postRepository,
                             NotificationRepository notificationRepository) {
        this.demandeService = demandeService;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.notificationRepository = notificationRepository;
    }

    // ============================
    // AJOUTER UNE DEMANDE (NEEDY)
    // ============================
    @PostMapping(value = "/ajouter", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('NEEDY')")
    public ResponseEntity<?> ajouterDemande(
            @RequestPart("demande") Demande demande,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "videos", required = false) List<MultipartFile> videos,
            Authentication authentication
    ) {
        System.out.println("Authentication principal: " + authentication.getPrincipal());
        System.out.println("Authorities: " + authentication.getAuthorities());

        // 🔥 Récupérer le username de l'utilisateur connecté
        String username = authentication.getName();
        System.out.println("Username connecté: " + username);

        Demande saved = demandeService.creerDemandeAvecFichiers(demande, images, videos, username);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/ajouter-json")
    @PreAuthorize("hasRole('NEEDY')")
    public ResponseEntity<?> ajouterDemandeJson(@RequestBody Map<String, String> payload) {
        try {
            String contenu = payload.get("contenu");
            String typeDemande = payload.get("typeDemande");
            String userId = payload.get("userId");

            if (contenu == null || typeDemande == null || userId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Les champs 'contenu', 'typeDemande' et 'userId' sont requis"));
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Demande demande = new Demande();
            demande.setContenu(contenu);
            demande.setTypeDemande(Demande.TypeDemande.valueOf(typeDemande.toUpperCase()));
            demande.setUser(user);

            Demande saved = demandeService.creerDemande(demande);
            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la création de la demande : " + e.getMessage()));
        }
    }

    // ============================
    // LISTER DEMANDES (ADMIN)
    // ============================
    @GetMapping("/liste")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Demande> getAllDemandes() {
        return demandeService.listerDemandes();
    }

    @GetMapping("/etat/{etat}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Demande> getDemandesByEtat(@PathVariable String etat) {
        return demandeService.getDemandesByEtat(Demande.EtatDemande.valueOf(etat.toUpperCase()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDemandeById(@PathVariable String id) {
        try {
            Demande demande = demandeService.getDemandeById(id);
            return ResponseEntity.ok(demande);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================
    // MES DEMANDES (NEEDY)
    // ============================

    // 🔥 GET - Voir mes demandes en attente
    @GetMapping("/needy/mes-demandes/en-attente")
    @PreAuthorize("hasRole('NEEDY')")
    public ResponseEntity<?> getMesDemandesEnAttente(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("Username connecté (mes demandes): " + username);

            List<Demande> demandes = demandeService.getMesDemandesEnAttente(username);
            return ResponseEntity.ok(demandes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération des demandes : " + e.getMessage()));
        }
    }

    // 🔥 GET - Voir toutes mes demandes
    @GetMapping("/needy/mes-demandes")
    @PreAuthorize("hasRole('NEEDY')")
    public ResponseEntity<?> getMesDemandes(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("Username connecté (toutes mes demandes): " + username);

            List<Demande> demandes = demandeService.getMesDemandes(username);
            return ResponseEntity.ok(demandes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération des demandes : " + e.getMessage()));
        }
    }

    // 🔥 GET - Voir une demande spécifique (seulement si c'est la sienne)
    @GetMapping("/needy/{id}")
    @PreAuthorize("hasRole('NEEDY')")
    public ResponseEntity<?> getMaDemande(@PathVariable String id, Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("Username connecté (ma demande): " + username);

            Demande demande = demandeService.getMaDemande(id, username);
            return ResponseEntity.ok(demande);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération de la demande : " + e.getMessage()));
        }
    }

    // ============================
    // TRAITER DEMANDE (ADMIN)
    // ============================
    @PutMapping("/{id}/statut")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> traiterDemande(@PathVariable String id, @RequestParam String action) {
        try {
            List<Notification> notifications = demandeService.traiterDemande(id, action);
            return ResponseEntity.ok(Map.of(
                    "statut", action.equalsIgnoreCase("accepter") ? "ACCEPTEE" : "REFUSEE",
                    "notifications", notifications
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateDemande(@PathVariable String id, @RequestBody Demande demande) {
        try {
            Demande updated = demandeService.updateDemande(id, demande);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteDemande(@PathVariable String id) {
        demandeService.deleteDemande(id);
        return ResponseEntity.ok(Map.of("message", "Demande " + id + " supprimée avec succès"));
    }

    // ============================
    // MODIFIER / SUPPRIMER DEMANDE (NEEDY) - CORRIGÉ
    // ============================

    // 🔥 CORRECTION: Update pour needy avec Authentication
    @PutMapping("/needy/{id}")
    @PreAuthorize("hasRole('NEEDY')")
    public ResponseEntity<?> updateDemandeNeedy(
            @PathVariable String id,
            @RequestBody Demande demande,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            System.out.println("🔄 Modification demande " + id + " par: " + username);

            Demande updated = demandeService.updateDemandeNeedy(id, username, demande);
            return ResponseEntity.ok(updated);

        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la modification: " + e.getMessage()));
        }
    }

    // 🔥 CORRECTION: Delete pour needy avec Authentication
    @DeleteMapping("/needy/{id}")
    @PreAuthorize("hasRole('NEEDY')")
    public ResponseEntity<?> deleteDemandeNeedy(
            @PathVariable String id,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            System.out.println("🗑️ Suppression demande " + id + " par: " + username);

            demandeService.deleteDemandeNeedy(id, username);
            return ResponseEntity.ok(Map.of("message", "Demande supprimée avec succès"));

        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la suppression: " + e.getMessage()));
        }
    }
}