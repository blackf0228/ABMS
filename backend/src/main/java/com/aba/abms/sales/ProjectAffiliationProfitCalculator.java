package com.aba.abms.sales;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
final class ProjectAffiliationProfitCalculator {
  static final Map<String,String> MEMBERS;
  static {
    Map<String,String> members=new LinkedHashMap<>();
    members.put("0001","王 超"); members.put("0002","小林 和成"); members.put("0010","毛 杰");
    members.put("0011","李 鴻民"); members.put("0006","孫 偉東");
    MEMBERS=Collections.unmodifiableMap(members);
  }
  private ProjectAffiliationProfitCalculator() {}
  static List<ProjectAffiliationProfitRow> calculate(List<ProfitContributionDetail> details) {
    Map<String,BigDecimal> owners=new LinkedHashMap<>(), proposers=new LinkedHashMap<>();
    MEMBERS.keySet().forEach(no -> { owners.put(no,BigDecimal.ZERO); proposers.put(no,BigDecimal.ZERO); });
    for (var detail:details) {
      BigDecimal owner=detail.profitAmount().multiply(new BigDecimal("0.8"));
      BigDecimal proposer=detail.profitAmount().subtract(owner);
      owners.computeIfPresent(detail.projectAffiliation(),(no,value)->value.add(owner));
      proposers.computeIfPresent(detail.engineerProposer(),(no,value)->value.add(proposer));
    }
    BigDecimal total=owners.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add)
        .add(proposers.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add));
    List<ProjectAffiliationProfitRow> rows=new ArrayList<>();
    MEMBERS.forEach((no,name)-> {
      BigDecimal profit=owners.get(no).add(proposers.get(no));
      BigDecimal percentage=total.signum()==0 ? BigDecimal.ZERO.setScale(2)
          : profit.multiply(new BigDecimal("100")).divide(total,2,RoundingMode.HALF_UP);
      rows.add(new ProjectAffiliationProfitRow(no,name,profit,percentage,owners.get(no),proposers.get(no)));
    });
    return rows;
  }
}
