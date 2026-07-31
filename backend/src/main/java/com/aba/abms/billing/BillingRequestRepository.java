package com.aba.abms.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BillingRequestRepository
    extends JpaRepository<BillingRequest, Long>, JpaSpecificationExecutor<BillingRequest> {
}
