package com.chequepay.repository;

import com.chequepay.entity.Cheque;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChequeRepository extends JpaRepository<Cheque, UUID> {
    List<Cheque> findByStatus(String status);
}