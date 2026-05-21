package io.github.blakedunaway.authserver.business.service;

import freemarker.template.Configuration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    private final Configuration freeMarkerConfiguration;

    @Value("${auth-server.support-email}")
    private String fromEmailAddress;

    public void sendEmail(final String to,
                          final String subject,
                          final String text)  {
        sendEmail(to, subject, text, Map.of());
    }

    public void sendEmail(final String to,
                          final String subject,
                          final String text,
                          final Map<String, Resource> inlineResources)  {
        MimeMessagePreparator msg = mimeMessage -> {
            final MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setText(text, true);
            helper.setSubject(subject);
            helper.setFrom(fromEmailAddress);
            helper.setTo(to);
            inlineResources.forEach((contentId, resource) -> {
                try {
                    helper.addInline(contentId, resource, "image/png");
                } catch (final Exception ex) {
                    throw new IllegalStateException("Unable to attach inline email resource " + contentId, ex);
                }
            });
        };
        this.mailSender.send(msg);
    }

    public void sendTemplateEmail(final String to,
                                  final String subject,
                                  final String templateName,
                                  final Map<String, Object> model) {
        final String htmlBody;
        try {
            htmlBody = FreeMarkerTemplateUtils.processTemplateIntoString(
                    freeMarkerConfiguration.getTemplate(templateName),
                    model
            );
        } catch (final Exception ex) {
            throw new IllegalStateException("Unable to render email template " + templateName, ex);
        }
        sendEmail(to, subject, htmlBody);
    }

    public void sendTemplateEmail(final String to,
                                  final String subject,
                                  final String templateName,
                                  final Map<String, Object> model,
                                  final Map<String, Resource> inlineResources) {
        final String htmlBody;
        try {
            htmlBody = FreeMarkerTemplateUtils.processTemplateIntoString(
                    freeMarkerConfiguration.getTemplate(templateName),
                    model
            );
        } catch (final Exception ex) {
            throw new IllegalStateException("Unable to render email template " + templateName, ex);
        }
        sendEmail(to, subject, htmlBody, inlineResources);
    }

}
