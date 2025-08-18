package com.chequepay.service;

import com.chequepay.entity.Cheque;
import com.chequepay.repository.ChequeRepository;
import com.chequepay.util.QRCodeUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final ChequeRepository chequeRepository;
    private final JavaMailSender mailSender;

    public boolean sendChequeByEmail(UUID chequeId, String recipientEmail) {
        Optional<Cheque> chequeOpt = chequeRepository.findById(chequeId);
        if (chequeOpt.isEmpty()) return false;

        Cheque cheque = chequeOpt.get();
        String payload = buildPayload(cheque);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom("cheque-pay@outlook.com");
            helper.setTo(recipientEmail);
            helper.setSubject("Electronic Cheque Transfer");

            String emailContent = String.format(
                    "Hello %s,\n\n" +
                            "You have received an electronic cheque from %s.\n\n" +
                            "Amount: %s\n" +
                            "Expiry Date: %s\n\n" +
                            "Please click the link below to view and verify the cheque:\n" +
                            "https://your-domain.com/cheques/redeem?id=%s",
                    cheque.getPayeeUsername(),
                    cheque.getPayerUsername(),
                    cheque.getAmount(),
                    cheque.getExpiryDate(),
                    cheque.getId()
            );
            helper.setText(emailContent);

            mailSender.send(message);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String generateChequeQRCode(UUID chequeId) throws Exception {
        Optional<Cheque> chequeOpt = chequeRepository.findById(chequeId);
        if (chequeOpt.isEmpty()) throw new RuntimeException("Cheque not found");

        Cheque cheque = chequeOpt.get();
        String payload = buildPayload(cheque);
        return QRCodeUtil.generateQRCodeBase64(payload, 250, 250);
    }

    public String transferChequeP2P(UUID chequeId) {
        Optional<Cheque> chequeOpt = chequeRepository.findById(chequeId);
        if (chequeOpt.isEmpty()) throw new RuntimeException("Cheque not found");

        return buildPayload(chequeOpt.get());
    }

    private String buildPayload(Cheque cheque) {
        return "{"
                + "\"chequeId\":\"" + cheque.getId() + "\","
                + "\"encryptedKey\":\"" + cheque.getEncryptedKey() + "\","
                + "\"encryptedData\":\"" + cheque.getEncryptedData() + "\","
                + "\"signature\":\"" + cheque.getSignature() + "\","
                + "\"nonce\":\"" + cheque.getNonce() + "\""
                + "}";
    }
}
