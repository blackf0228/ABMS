package com.aba.abms.payroll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmployeeMasterControllerTest {

  private EmployeeMasterRepository repository;
  private EmployeeMasterController controller;

  @BeforeEach
  void setUp() {
    repository = mock(EmployeeMasterRepository.class);
    controller = new EmployeeMasterController(repository);
  }

  @Test
  void nextNumberUsesHighestThreeDigitBpNumber() {
    when(repository.findAll()).thenReturn(List.of(
        employee("0001", "0"), employee("0002", "0"), employee("B024", "2"),
        employee("B029", "2"), employee("B999", "0")));

    assertThat(controller.nextNumber("2")).isEqualTo("B030");
    assertThat(controller.nextNumber("0")).isEqualTo("0003");
  }

  @Test
  void creatingBpAssignsNextBpNumberAndZerosDisabledFields() {
    when(repository.findAll()).thenReturn(List.of(employee("B029", "2")));
    when(repository.save(any(EmployeeMaster.class))).thenAnswer(call -> call.getArgument(0));
    EmployeeMasterDto request = new EmployeeMasterDto(
        "0001", "Test BP", "Test", "BP", "2", new BigDecimal("500000"),
        3, 2, new BigDecimal("0.10"), "0", LocalDate.of(2026, 11, 1), null);

    EmployeeMasterDto saved = controller.create(request);

    assertThat(saved.employeeNo()).isEqualTo("B030");
    assertThat(saved.employeeType()).isEqualTo("2");
    assertThat(saved.dependentCount()).isZero();
    assertThat(saved.deductionCount()).isZero();
    assertThat(saved.consumptionTaxRate()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(saved.effectiveStartDate()).isEqualTo(LocalDate.of(2026, 11, 1));
    assertThat(saved.effectiveEndDate()).isEqualTo(LocalDate.of(9999, 12, 31));
  }

  @Test
  void creatingNonBpKeepsExistingNumericNumberAndFields() {
    when(repository.save(any(EmployeeMaster.class))).thenAnswer(call -> call.getArgument(0));
    EmployeeMasterDto request = new EmployeeMasterDto(
        "0030", "Test Employee", "Test", "Employee", "0", new BigDecimal("300000"),
        1, 1, BigDecimal.ZERO, "0", LocalDate.of(2026, 10, 8), null);

    EmployeeMasterDto saved = controller.create(request);

    assertThat(saved.employeeNo()).isEqualTo("0030");
    assertThat(saved.dependentCount()).isEqualTo(1);
    assertThat(saved.deductionCount()).isEqualTo(1);
  }

  @Test
  void correctionClosesPreviousVersionAndKeepsItsSalary() {
    EmployeeMaster previous = employee("0001", "0");
    previous.setBaseSalary(new BigDecimal("500000"));
    previous.setEffectiveStartDate(LocalDate.of(2025, 1, 1));
    when(repository.findByEmployeeNoOrderByEffectiveStartDateDesc("0001")).thenReturn(List.of(previous));
    when(repository.save(any(EmployeeMaster.class))).thenAnswer(call -> call.getArgument(0));
    EmployeeMasterDto saved = controller.update("0001", request(LocalDate.of(2026, 1, 1)));
    assertThat(previous.getEffectiveEndDate()).isEqualTo(LocalDate.of(2025, 12, 31));
    assertThat(previous.getBaseSalary()).isEqualByComparingTo("500000");
    assertThat(saved.baseSalary()).isEqualByComparingTo("550000");
    assertThat(saved.effectiveStartDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(saved.effectiveEndDate()).isEqualTo(LocalDate.of(9999, 12, 31));
    verify(repository).lockEmployee("0001");
    verify(repository).saveAndFlush(previous);
  }

  @Test
  void correctionRejectsSameOrEarlierStartWithoutChangingHistory() {
    EmployeeMaster previous = employee("0001", "0");
    previous.setEffectiveStartDate(LocalDate.of(2026, 3, 1));
    when(repository.findByEmployeeNoOrderByEffectiveStartDateDesc("0001")).thenReturn(List.of(previous));
    for (LocalDate start : List.of(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 2, 28))) {
      assertThatThrownBy(() -> controller.update("0001", request(start)))
          .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
    assertThat(previous.getEffectiveEndDate()).isEqualTo(LocalDate.of(9999, 12, 31));
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void creationRequiresEffectiveStartDate() {
    assertThatThrownBy(() -> controller.create(request(null)))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void historyReturnsNewestFirstIncludingFutureRecords() {
    EmployeeMaster future = employee("0001", "0");
    future.setEffectiveStartDate(LocalDate.of(2026, 11, 1));
    EmployeeMaster old = employee("0001", "0");
    old.setEffectiveStartDate(LocalDate.of(1900, 1, 1));
    when(repository.findByEmployeeNoOrderByEffectiveStartDateDesc("0001")).thenReturn(List.of(future, old));
    assertThat(controller.history("0001")).extracting(EmployeeMasterDto::effectiveStartDate)
        .containsExactly(LocalDate.of(2026, 11, 1), LocalDate.of(1900, 1, 1));
  }

  private EmployeeMasterDto request(LocalDate start) {
    return new EmployeeMasterDto("0001", "Test Employee", "Test", "Employee", "0",
        new BigDecimal("550000"), 2, 1, BigDecimal.ZERO, "0", start, LocalDate.of(2027, 1, 1));
  }

  private static EmployeeMaster employee(String employeeNo, String employeeType) {
    EmployeeMaster employee = new EmployeeMaster();
    employee.setEmployeeNo(employeeNo);
    employee.setEmployeeType(employeeType);
    return employee;
  }
}
