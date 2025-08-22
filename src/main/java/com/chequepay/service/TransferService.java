package com.chequepay.service;

import com.chequepay.entity.Cheque;
import com.chequepay.repository.ChequeRepository;
import com.chequepay.util.QRCodeUtil;
import com.google.zxing.WriterException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final ChequeRepository chequeRepository;
    private final JavaMailSender mailSender;

    public boolean sendChequeByEmail(UUID chequeId, String recipientEmail) {
        Optional<Cheque> chequeOpt = chequeRepository.findById(chequeId);
        if (chequeOpt.isEmpty()) throw new RuntimeException("Cheque not found");

        Cheque cheque = chequeOpt.get();

        try {
            String qrBase64 = generateChequeQRCode(chequeId);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom("cheque-pay@outlook.com");
            helper.setTo(recipientEmail);
            helper.setSubject("Electronic Cheque Transfer");

            String emailContent = String.format(
                    "<p>Hello %s,</p>" +
                            "<p>You have received an electronic cheque from <b>%s</b>.</p>" +
                            "<p>Amount: <b>%s</b><br/>Expiry Date: %s</p>" +
                            "<p>Scan this QR Code to redeem:</p>" +
                            "<img src='data:image/png;base64,%s'/>",
                    cheque.getPayeeRealname(),
                    cheque.getPayerRealname(),
                    cheque.getAmount(),
                    cheque.getExpiryDate(),
                    qrBase64
            );

            helper.setText(emailContent, true);

            mailSender.send(message);
            return true;
        } catch (MessagingException | IOException | WriterException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String generateChequeQRCode(UUID chequeId) throws IOException, WriterException {
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
