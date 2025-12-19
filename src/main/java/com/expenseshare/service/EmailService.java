package com.expenseshare.service;

import com.expenseshare.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.name:SplitEase}")
    private String appName;

    @Value("${spring.mail.username:noreply@splitease.com}")
    private String fromEmail;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    /**
     * Send notification when a new expense is added.
     */
    @Async
    public void sendExpenseNotification(Expense expense) {
        if (!emailEnabled) {
            log.info("Email disabled. Would have sent expense notification for: {}", expense.getDescription());
            return;
        }

        try {
            for (ExpenseSplit split : expense.getSplits()) {
                // Don't notify the person who paid
                if (!split.getUser().getId().equals(expense.getPaidBy().getId())) {
                    String subject = String.format("New expense in %s - %s",
                            expense.getGroup().getName(), expense.getDescription());

                    String htmlBody = buildExpenseEmailHtml(
                            split.getUser().getName(),
                            expense.getPaidBy().getName(),
                            expense.getGroup().getName(),
                            expense.getDescription(),
                            expense.getAmount(),
                            split.getAmount());

                    sendHtmlEmail(split.getUser().getEmail(), subject, htmlBody);
                }
            }
        } catch (Exception e) {
            log.error("Failed to send expense notification", e);
        }
    }

    /**
     * Send notification when a settlement is recorded.
     */
    @Async
    public void sendSettlementNotification(Settlement settlement) {
        if (!emailEnabled) {
            log.info("Email disabled. Would have sent settlement notification for {} to {}",
                    settlement.getPayer().getName(), settlement.getPayee().getName());
            return;
        }

        try {
            String subject = String.format("Payment received from %s", settlement.getPayer().getName());

            String htmlBody = buildSettlementEmailHtml(
                    settlement.getPayee().getName(),
                    settlement.getPayer().getName(),
                    settlement.getAmount(),
                    settlement.getGroup().getName());

            sendHtmlEmail(settlement.getPayee().getEmail(), subject, htmlBody);

        } catch (Exception e) {
            log.error("Failed to send settlement notification", e);
        }
    }

    /**
     * Send a payment reminder.
     */
    @Async
    public void sendReminder(User fromUser, User toUser, BigDecimal amount, ExpenseGroup group) {
        if (!emailEnabled) {
            log.info("Email disabled. Would have sent reminder from {} to {} for {}",
                    fromUser.getName(), toUser.getName(), amount);
            return;
        }

        try {
            String subject = String.format("Payment reminder from %s", fromUser.getName());

            String htmlBody = buildReminderEmailHtml(
                    toUser.getName(),
                    fromUser.getName(),
                    amount,
                    group.getName());

            sendHtmlEmail(toUser.getEmail(), subject, htmlBody);

        } catch (Exception e) {
            log.error("Failed to send reminder", e);
        }
    }

    /**
     * Build HTML email for expense notification
     */
    private String buildExpenseEmailHtml(String recipientName, String paidByName, String groupName,
            String description, BigDecimal totalAmount, BigDecimal shareAmount) {
        return String.format(
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        </head>
                        <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f5f5;">
                            <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                                <!-- Header -->
                                <div style="background: linear-gradient(135deg, #1a73e8 0%%, #4285f4 100%%); border-radius: 16px 16px 0 0; padding: 32px; text-align: center;">
                                    <div style="width: 60px; height: 60px; background: white; border-radius: 12px; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 16px;">
                                        <span style="font-size: 24px; font-weight: 700; color: #1a73e8;">SE</span>
                                    </div>
                                    <h1 style="color: white; margin: 0; font-size: 24px; font-weight: 600;">New Expense Added</h1>
                                </div>

                                <!-- Content -->
                                <div style="background: white; padding: 32px; border-radius: 0 0 16px 16px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
                                    <p style="color: #202124; font-size: 16px; margin: 0 0 24px 0;">
                                        Hi <strong>%s</strong>,
                                    </p>

                                    <p style="color: #5f6368; font-size: 15px; margin: 0 0 24px 0;">
                                        <strong style="color: #1a73e8;">%s</strong> added a new expense in <strong>%s</strong>:
                                    </p>

                                    <!-- Expense Card -->
                                    <div style="background: #f8f9fa; border-radius: 12px; padding: 24px; margin-bottom: 24px; border-left: 4px solid #1a73e8;">
                                        <div style="font-size: 18px; font-weight: 600; color: #202124; margin-bottom: 8px;">%s</div>
                                        <div style="display: flex; justify-content: space-between; margin-top: 16px;">
                                            <div>
                                                <div style="font-size: 12px; color: #5f6368; text-transform: uppercase; letter-spacing: 0.5px;">Total Amount</div>
                                                <div style="font-size: 20px; font-weight: 600; color: #202124;">%s</div>
                                            </div>
                                            <div style="text-align: right;">
                                                <div style="font-size: 12px; color: #5f6368; text-transform: uppercase; letter-spacing: 0.5px;">Your Share</div>
                                                <div style="font-size: 24px; font-weight: 700; color: #ea4335;">%s</div>
                                            </div>
                                        </div>
                                    </div>

                                    <!-- CTA Button -->
                                    <div style="text-align: center; margin: 32px 0;">
                                        <a href="http://localhost:8088/dashboard" style="display: inline-block; background: #1a73e8; color: white; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: 500; font-size: 15px;">
                                            View Details
                                        </a>
                                    </div>

                                    <hr style="border: none; border-top: 1px solid #e8eaed; margin: 24px 0;">

                                    <p style="color: #5f6368; font-size: 13px; margin: 0; text-align: center;">
                                        This email was sent by %s. If you didn't expect this email, you can ignore it.
                                    </p>
                                </div>

                                <!-- Footer -->
                                <div style="text-align: center; padding: 24px; color: #5f6368; font-size: 12px;">
                                    <p style="margin: 0;">© 2024 %s. Split expenses with ease.</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                recipientName,
                paidByName,
                groupName,
                description,
                currencyFormat.format(totalAmount),
                currencyFormat.format(shareAmount),
                appName,
                appName);
    }

    /**
     * Build HTML email for settlement notification
     */
    private String buildSettlementEmailHtml(String recipientName, String payerName, BigDecimal amount,
            String groupName) {
        return String.format(
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        </head>
                        <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f5f5;">
                            <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                                <!-- Header -->
                                <div style="background: linear-gradient(135deg, #34a853 0%%, #0f9d58 100%%); border-radius: 16px 16px 0 0; padding: 32px; text-align: center;">
                                    <div style="width: 60px; height: 60px; background: white; border-radius: 50%%; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 16px;">
                                        <span style="font-size: 28px;">✓</span>
                                    </div>
                                    <h1 style="color: white; margin: 0; font-size: 24px; font-weight: 600;">Payment Received!</h1>
                                </div>

                                <!-- Content -->
                                <div style="background: white; padding: 32px; border-radius: 0 0 16px 16px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
                                    <p style="color: #202124; font-size: 16px; margin: 0 0 24px 0;">
                                        Hi <strong>%s</strong>,
                                    </p>

                                    <p style="color: #5f6368; font-size: 15px; margin: 0 0 24px 0;">
                                        Great news! You've received a payment.
                                    </p>

                                    <!-- Payment Card -->
                                    <div style="background: linear-gradient(135deg, #e8f5e9 0%%, #c8e6c9 100%%); border-radius: 12px; padding: 24px; text-align: center; margin-bottom: 24px;">
                                        <div style="font-size: 14px; color: #5f6368; margin-bottom: 8px;">Amount Received</div>
                                        <div style="font-size: 36px; font-weight: 700; color: #34a853;">%s</div>
                                        <div style="font-size: 14px; color: #5f6368; margin-top: 8px;">from <strong>%s</strong></div>
                                        <div style="font-size: 13px; color: #5f6368; margin-top: 4px;">in %s</div>
                                    </div>

                                    <!-- CTA Button -->
                                    <div style="text-align: center; margin: 32px 0;">
                                        <a href="http://localhost:8088/dashboard" style="display: inline-block; background: #34a853; color: white; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: 500; font-size: 15px;">
                                            View Dashboard
                                        </a>
                                    </div>

                                    <hr style="border: none; border-top: 1px solid #e8eaed; margin: 24px 0;">

                                    <p style="color: #5f6368; font-size: 13px; margin: 0; text-align: center;">
                                        This email was sent by %s.
                                    </p>
                                </div>

                                <!-- Footer -->
                                <div style="text-align: center; padding: 24px; color: #5f6368; font-size: 12px;">
                                    <p style="margin: 0;">© 2024 %s. Split expenses with ease.</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                recipientName,
                currencyFormat.format(amount),
                payerName,
                groupName,
                appName,
                appName);
    }

    /**
     * Build HTML email for payment reminder
     */
    private String buildReminderEmailHtml(String recipientName, String fromName, BigDecimal amount, String groupName) {
        return String.format(
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        </head>
                        <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f5f5;">
                            <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                                <!-- Header -->
                                <div style="background: linear-gradient(135deg, #fbbc04 0%%, #f9ab00 100%%); border-radius: 16px 16px 0 0; padding: 32px; text-align: center;">
                                    <div style="width: 60px; height: 60px; background: white; border-radius: 50%%; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 16px;">
                                        <span style="font-size: 28px;">🔔</span>
                                    </div>
                                    <h1 style="color: white; margin: 0; font-size: 24px; font-weight: 600;">Payment Reminder</h1>
                                </div>

                                <!-- Content -->
                                <div style="background: white; padding: 32px; border-radius: 0 0 16px 16px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
                                    <p style="color: #202124; font-size: 16px; margin: 0 0 24px 0;">
                                        Hi <strong>%s</strong>,
                                    </p>

                                    <p style="color: #5f6368; font-size: 15px; margin: 0 0 24px 0;">
                                        <strong style="color: #1a73e8;">%s</strong> sent you a friendly reminder about a pending payment.
                                    </p>

                                    <!-- Reminder Card -->
                                    <div style="background: #fff8e1; border-radius: 12px; padding: 24px; text-align: center; margin-bottom: 24px; border: 2px dashed #fbbc04;">
                                        <div style="font-size: 14px; color: #5f6368; margin-bottom: 8px;">Amount Due</div>
                                        <div style="font-size: 36px; font-weight: 700; color: #ea4335;">%s</div>
                                        <div style="font-size: 14px; color: #5f6368; margin-top: 8px;">for %s</div>
                                    </div>

                                    <!-- CTA Button -->
                                    <div style="text-align: center; margin: 32px 0;">
                                        <a href="http://localhost:8088/dashboard" style="display: inline-block; background: #1a73e8; color: white; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: 500; font-size: 15px;">
                                            Settle Now
                                        </a>
                                    </div>

                                    <hr style="border: none; border-top: 1px solid #e8eaed; margin: 24px 0;">

                                    <p style="color: #5f6368; font-size: 13px; margin: 0; text-align: center;">
                                        This email was sent by %s.
                                    </p>
                                </div>

                                <!-- Footer -->
                                <div style="text-align: center; padding: 24px; color: #5f6368; font-size: 12px;">
                                    <p style="margin: 0;">© 2024 %s. Split expenses with ease.</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                recipientName,
                fromName,
                currencyFormat.format(amount),
                groupName,
                appName,
                appName);
    }

    /**
     * Send an HTML email.
     */
    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("[" + appName + "] " + subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("HTML email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to: {}", to, e);
        }
    }
}
