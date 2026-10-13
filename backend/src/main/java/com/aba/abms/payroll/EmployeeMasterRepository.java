package com.aba.abms.payroll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface EmployeeMasterRepository extends JpaRepository<EmployeeMaster, EmployeeMasterId> {
  boolean existsByEmployeeNo(String employeeNo);
  List<EmployeeMaster> findByEmployeeNoOrderByEffectiveStartDateDesc(String employeeNo);

  @Query("select e from EmployeeMaster e where e.effectiveStartDate <= :today and e.effectiveEndDate >= :today order by e.employeeNo")
  List<EmployeeMaster> findCurrent(@Param("today") LocalDate today);

  // Serialize corrections before reading history, including concurrent future reservations.
  @Query(value = "select 1 from (select pg_advisory_xact_lock(hashtext(:employeeNo))) locked", nativeQuery = true)
  Integer lockEmployee(@Param("employeeNo") String employeeNo);
}
