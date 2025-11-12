package com.chequepay;

import com.chequepay.entity.Cheque;
import com.chequepay.repository.ChequeRepository;
import com.chequepay.service.TransferService;
import com.chequepay.util.QRCodeUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QRCodeServiceTest {

    @Mock
    private ChequeRepository chequeRepository;

    @InjectMocks
    private TransferService transferService;

    private UUID chequeId;
    private Cheque cheque;

    @BeforeEach
    void setUp() {
        chequeId = UUID.randomUUID();
        cheque = Cheque.builder()
                .id(chequeId)
                .amount(new BigDecimal("3000"))
                .payerUsername("xxxibgdrgn")
                .payeeUsername("sabrinacarpenter")
                .status("ISSUED")
                .expiryDate(LocalDateTime.now().plusMonths(12))
                .build();
    }

    @Test
    void generateQRCodeSuccess() throws Exception {
        when(chequeRepository.findById(chequeId)).thenReturn(Optional.of(cheque));

        try (MockedStatic<QRCodeUtil> qrMock = Mockito.mockStatic(QRCodeUtil.class)) {
            qrMock.when(() -> QRCodeUtil.generateQRCodeBase64(anyString(), anyInt(), anyInt()))
                    .thenReturn("mockedBase64QRCode");

            String result = transferService.generateChequeQRCode(chequeId);

            assertNotNull(result);
            assertEquals("mockedBase64QRCode", result);

            qrMock.verify(() -> QRCodeUtil.generateQRCodeBase64(anyString(), eq(250), eq(250)), times(1));
        }
    }

    @Test
    void generateQRCodeChequeNotFound() {
        when(chequeRepository.findById(chequeId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transferService.generateChequeQRCode(chequeId));

        assertEquals("Cheque not found", ex.getMessage());
    }
}
