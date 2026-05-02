package com.example.employee.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendRegistrationEmail(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Welcome to the EAC Careers Portal");
        message.setText("Hello!\n\n" +
                "Your applicant account has been created.\n\n" +
                "Sign-in details:\n" +
                "- Email (login ID): " + toEmail + "\n" +
                "- Password: use the password you chose at registration. For your security, we do not send passwords by email.\n\n" +
                "You can log in to the EAC Careers Portal to view open positions, submit applications, and track your status.\n\n" +
                "Virtus, Excelentia, Servitium.\n\n" +
                "Best regards,\n" +
                "Emilio Aguinaldo College HR Department");

        mailSender.send(message);
    }
}