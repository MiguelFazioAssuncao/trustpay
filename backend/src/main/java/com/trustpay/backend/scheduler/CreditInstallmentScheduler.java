package com.trustpay.backend.scheduler;

import com.trustpay.backend.enums.TransactionStatus;
import com.trustpay.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreditInstallmentScheduler {

    private final TransactionRepository transactionRepository;

    @Scheduled(fixedRate = 300000)
    public void processNextInstallment() {
        transactionRepository
                .findTop1ByStatusOrderByCreatedAtAsc(TransactionStatus.PENDING)
                .ifPresent(tx -> {
                    tx.setStatus(TransactionStatus.COMPLETED);
                    transactionRepository.save(tx);
                });
    }
}
