package com.aba.abms.attendance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "attendance_hours")
@IdClass(AttendanceHourId.class)
public class AttendanceHour {

  @Id
  @Column(name = "contract_no")
  private Integer contractNo;

  @Id
  @Column(name = "engineer_name", length = 100)
  private String engineerName;

  @Id
  @Column(name = "working_month", length = 6)
  private String workingMonth;

  @Id
  @Column(name = "working_hours", precision = 10, scale = 2)
  private BigDecimal workingHours;

  @Id
  @Column(name = "remarks", length = 2000)
  private String remarks;

  public AttendanceHour() {
  }
}
