package com.aba.abms.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlySalaryCalculationConditionRepository
    extends JpaRepository<MonthlySalaryCalculationCondition, EmployeePaymentId> {
}
