package com.iset.projet_integration.Service;

import com.iset.projet_integration.Entities.Comment;
import com.iset.projet_integration.Entities.Post;
import com.iset.projet_integration.Entities.User;
import com.iset.projet_integration.Repository.CommentRepository;
import com.iset.projet_integration.Repository.PostRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }

    public Comment addComment(String postId, User user, String contenu) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContenu(contenu);

        Comment saved = commentRepository.save(comment);

        // Update post comments count
        post.setCommentsCount(post.getCommentsCount() + 1);
        postRepository.save(post);

        return saved;
    }
    public Comment getCommentById(String commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with ID: " + commentId));
    }
    public List<Comment> findByPostId(String postId) {
        return commentRepository.findByPostId(postId);
    }

    public Comment updateComment(String commentId, String contenu) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        comment.updateContent(contenu);
        return commentRepository.save(comment);
    }

    public void deleteComment(String commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        Post post = comment.getPost();
        commentRepository.delete(comment);

        if(post != null) {
            post.setCommentsCount(Math.max(0, post.getCommentsCount() - 1));
            postRepository.save(post);
        }
    }
}
