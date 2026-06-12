package com.puredo.blog.Service.Email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class EmailServiceImpl implements EmailService {

    private final RestClient restClient;

    @Value("${brevo.from-email}")
    private String fromEmail;

    public EmailServiceImpl(@Value("${brevo.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .defaultHeader("api-key", apiKey)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Override
    public void sendPasswordReset(String to, String resetLink) {
        String subject = "Redefinição de senha — Micélio";
        String html = """
                <div style="font-family:sans-serif;max-width:520px;margin:auto">
                  <h2>Redefinição de senha</h2>
                  <p>Recebemos uma solicitação para redefinir a senha da sua conta no Micélio.</p>
                  <p>Clique no botão abaixo para criar uma nova senha. O link expira em <strong>1 hora</strong>.</p>
                  <a href="%s"
                     style="display:inline-block;padding:12px 24px;background:#2d6a4f;color:#fff;
                            text-decoration:none;border-radius:6px;margin:16px 0">
                    Redefinir senha
                  </a>
                  <p style="color:#666;font-size:13px">
                    Se você não solicitou isso, ignore este email — sua senha permanece a mesma.
                  </p>
                </div>
                """.formatted(resetLink);
        sendHtml(to, subject, html);
    }

    @Override
    public void sendStubPublished(String to, String subscriberUsername, String postTitle, String authorUsername, String postLink) {
        String subject = String.format("\"%s\" foi publicado — Micélio", postTitle);
        String html = """
                <div style="font-family:sans-serif;max-width:520px;margin:auto">
                  <p>Olá, %s.</p>
                  <p>O post que você marcou para acompanhar acabou de ser escrito por <strong>%s</strong>:</p>
                  <p style="margin:16px 0;font-size:17px"><em>"%s"</em></p>
                  <a href="%s"
                     style="display:inline-block;padding:12px 24px;background:#2d6a4f;color:#fff;
                            text-decoration:none;border-radius:6px">
                    Leia agora
                  </a>
                </div>
                """.formatted(subscriberUsername, authorUsername, postTitle, postLink);
        sendHtml(to, subject, html);
    }

    private void sendHtml(String to, String subject, String html) {
        Map<String, Object> body = Map.of(
                "sender", Map.of("name", "Micelio", "email", fromEmail),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", html
        );

        try {
            restClient.post()
                    .uri("/smtp/email")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao enviar email para " + to + ": " + e.getMessage(), e);
        }
    }
}
