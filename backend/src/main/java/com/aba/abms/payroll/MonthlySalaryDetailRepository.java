package com.aba.abms.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlySalaryDetailRepository
    extends JpaRepository<MonthlySalaryDetail, EmployeePaymentId> {
}
