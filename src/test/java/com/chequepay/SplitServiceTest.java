package com.chequepay;

import com.chequepay.entity.Cheque;
import com.chequepay.repository.ChequeRepository;
import com.chequepay.service.ChequeService;
import com.chequepay.service.KeyManager;
import com.chequepay.util.AESUtil;
import com.chequepay.util.RSAUtil;
import com.chequepay.dto.ChequeSplitRequest;
import com.chequepay.dto.ChequeResponse;

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
public class SplitServiceTest {

    @Mock
    private ChequeRepository chequeRepository;

    @Mock
    private KeyManager keyManager;

    @InjectMocks
    private ChequeService chequeService;

    private Cheque parentCheque;

    @BeforeEach
    void setup() {
        parentCheque = Cheque.builder()
                .id(UUID.randomUUID())
                .amount(new BigDecimal("1000"))
                .payerUsername("alice")
                .payerRealname("Alice Smith")
                .payeeUsername("bob")
                .payeeRealname("Bob Johnson")
                .expiryDate(LocalDateTime.now().plusDays(5))
                .issueDate(LocalDateTime.now())
                .status("ISSUED")
                .parentChequeId(null)
                .build();

        when(chequeRepository.findById(parentCheque.getId()))
                .thenReturn(Optional.of(parentCheque));
    }

    @Test
    void splitSuccess() throws Exception {
        when(keyManager.getAesKey()).thenReturn(AESUtil.generateAESKey());
        when(keyManager.getRsaKeyPair()).thenReturn(RSAUtil.generateKeyPair(2048));

        when(chequeRepository.save(any(Cheque.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChequeSplitRequest request = new ChequeSplitRequest();
        request.setSplitAmounts(List.of(
                new BigDecimal("400"),
                new BigDecimal("300"),
                new BigDecimal("300")
        ));

        List<ChequeResponse> result = chequeService.splitCheque(parentCheque.getId(), request);

        assertEquals("SPLIT", parentCheque.getStatus());

        assertEquals(3, result.size());
        assertEquals(new BigDecimal("400"), result.get(0).getAmount());
        assertEquals(new BigDecimal("300"), result.get(1).getAmount());
        assertEquals(new BigDecimal("300"), result.get(2).getAmount());

        ArgumentCaptor<Cheque> captor = ArgumentCaptor.forClass(Cheque.class);
        verify(chequeRepository, times(4)).save(captor.capture());
        List<Cheque> savedCheques = captor.getAllValues();

        Cheque savedParent = savedCheques.stream()
                .filter(c -> c.getParentChequeId() == null)
                .findFirst()
                .orElseThrow();
        assertEquals("SPLIT", savedParent.getStatus());

        List<Cheque> children = savedCheques.stream()
                .filter(c -> c.getParentChequeId() != null)
                .toList();
        assertEquals(3, children.size());
        children.forEach(c -> assertEquals(parentCheque.getId(), c.getParentChequeId()));
    }

    @Test
    void splitChildChequeError() {
        parentCheque.setParentChequeId(UUID.randomUUID());

        ChequeSplitRequest request = new ChequeSplitRequest();
        request.setSplitAmounts(List.of(new BigDecimal("100")));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> chequeService.splitCheque(parentCheque.getId(), request)
        );
        assertEquals("Cannot split a child cheque", ex.getMessage());

        verify(chequeRepository, never()).save(any());
    }

    @Test
    void splitAmountMismatch() {
        ChequeSplitRequest request = new ChequeSplitRequest();
        request.setSplitAmounts(List.of(new BigDecimal("500"), new BigDecimal("200")));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> chequeService.splitCheque(parentCheque.getId(), request)
        );
        assertEquals("Split amounts must add up to the parent cheque amount", ex.getMessage());

        verify(chequeRepository, never()).save(any());
    }

    @Test
    void splitChequeNotIssued() {
        parentCheque.setStatus("REDEEMED");

        ChequeSplitRequest request = new ChequeSplitRequest();
        request.setSplitAmounts(List.of(new BigDecimal("500"), new BigDecimal("500")));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> chequeService.splitCheque(parentCheque.getId(), request)
        );

        assertEquals("Only ISSUED cheques can be split", ex.getMessage());

        verify(chequeRepository, never()).save(any());
    }
}
