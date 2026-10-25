package com.allst.springboot.invoice.repo;

import com.allst.springboot.invoice.domain.InvoiceApply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceApplyRepository extends JpaRepository<InvoiceApply, Long> {

    Optional<InvoiceApply> findByInvoiceNo(String invoiceNo);

    boolean existsByInvoiceNo(String invoiceNo);
}
