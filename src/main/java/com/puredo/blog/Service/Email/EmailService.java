package com.puredo.blog.Service.Email;

public interface EmailService {
    void sendPasswordReset(String to, String resetLink);
    void sendStubPublished(String to, String subscriberUsername, String postTitle, String authorUsername, String postLink);
}
