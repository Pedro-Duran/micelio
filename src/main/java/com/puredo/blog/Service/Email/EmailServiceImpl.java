package com.puredo.blog.Service.Email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Autowired
    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
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
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Falha ao enviar email para " + to, e);
        }
    }
}
