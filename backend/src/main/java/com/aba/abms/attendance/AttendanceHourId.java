package com.aba.abms.attendance;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public class AttendanceHourId implements Serializable {

  private Integer contractNo;
  private String engineerName;
  private String workingMonth;
  private BigDecimal workingHours;
  private String remarks;

  public AttendanceHourId() {
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof AttendanceHourId other)) {
      return false;
    }
    return Objects.equals(contractNo, other.contractNo)
        && Objects.equals(engineerName, other.engineerName)
        && Objects.equals(workingMonth, other.workingMonth)
        && Objects.equals(workingHours, other.workingHours)
        && Objects.equals(remarks, other.remarks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contractNo, engineerName, workingMonth, workingHours, remarks);
  }
}
