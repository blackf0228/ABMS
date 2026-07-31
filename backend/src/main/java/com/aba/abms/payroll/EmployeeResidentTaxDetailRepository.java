package com.aba.abms.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeResidentTaxDetailRepository
    extends JpaRepository<EmployeeResidentTaxDetail, EmployeePaymentId> {
}
