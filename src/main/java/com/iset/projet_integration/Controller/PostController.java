package com.iset.projet_integration.Controller;

import com.iset.projet_integration.Entities.Comment;
import com.iset.projet_integration.Entities.Post;
import com.iset.projet_integration.Entities.User;
import com.iset.projet_integration.Repository.UserRepository;
import com.iset.projet_integration.Service.CommentService;
import com.iset.projet_integration.Service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "http://localhost:4200")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserRepository userRepository;

    // ----------------- Posts -----------------
    @GetMapping
    public List<Post> getAllPosts() {
        return postService.getAllPosts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Post> getPostById(@PathVariable String id) {
        try {
            Post post = postService.getPostById(id);
            return ResponseEntity.ok(post);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<?> toggleLike(
            @PathVariable String postId,
            @RequestBody Map<String, String> payload) {

        String userId = payload.get("userId");
        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.badRequest().body("User ID is required");
        }

        return ResponseEntity.ok(postService.toggleLike(postId, userId));
    }

    @GetMapping("/{postId}/like/status")
    public ResponseEntity<?> getLikeStatus(
            @PathVariable String postId,
            @RequestParam String userId) {

        boolean hasLiked = postService.hasUserLiked(postId, userId);
        return ResponseEntity.ok(Map.of("hasLiked", hasLiked));
    }

    // ----------------- Commentaires -----------------
    @GetMapping("/{postId}/comments")
    public List<Comment> getComments(@PathVariable String postId) {
        return commentService.findByPostId(postId);
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable String postId,
            @RequestBody Map<String, String> payload) {

        String userId = payload.get("userId");
        String contenu = payload.get("contenu");

        if(userId == null || userId.isEmpty()) {
            return ResponseEntity.badRequest().body("User ID is required");
        }

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Comment comment = commentService.addComment(postId, user, contenu);
            return ResponseEntity.ok(comment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<?> updateComment(
            @PathVariable String commentId,
            @RequestBody Map<String, String> payload) {

        String contenu = payload.get("contenu");
        try {
            Comment updated = commentService.updateComment(commentId, contenu);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable String commentId) {
        try {
            commentService.deleteComment(commentId);
            return ResponseEntity.ok(Map.of("deleted", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}