package com.aba.abms.sales;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

final class ProfitContributionService {
  private final NamedParameterJdbcTemplate jdbc;
  ProfitContributionService(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

  record Result(List<ProjectAffiliationProfitRow> rows, List<ProfitContributionDetail> details,
                List<String> warnings, BigDecimal unallocatedProfit) {}

  Result calculate(YearMonth start, YearMonth end) {
    List<String> warnings = new ArrayList<>();
    List<ProfitContributionDetail> details = new ArrayList<>();
    // Aggregate actual invoices before joining contracts, so multiple invoice lines cannot duplicate costs.
    jdbc.query("""
        with actuals as (
          select engineer_name as engineer_no, date_trunc('month', billing_month)::date as month,
            coalesce(sum(billing_amount) filter (where billing_source_company_id = '0000'), 0) as sales,
            coalesce(sum(billing_amount) filter (where billing_destination_company_id = '0000'
              and billing_source_company_id <> '0000'), 0) as bp_payment,
            count(*) filter (where billing_destination_company_id = '0000'
              and billing_source_company_id <> '0000') as bp_payment_count,
            count(*) filter (where billing_amount is null) as missing_invoice_amount_count
          from sales_imports
          where billing_month between :start and :end
            and nullif(trim(engineer_name), '') is not null
            and (billing_source_company_id = '0000' or billing_destination_company_id = '0000')
            and coalesce(engineer_name, '') not in ('0001','0002','0010','0011','0006')
          group by engineer_name, date_trunc('month', billing_month)::date
        ), payroll as (
          select d.employee_no,
            (to_date(replace(d.payment_month, '-', '') || '01', 'YYYYMMDD') - interval '1 month')::date as month,
            count(*) as payroll_count,
            coalesce(sum(d.transfer_payment_amount), 0) as individual_payment,
            sum(coalesce(c.base_salary, e.base_salary, 0) + coalesce(c.commuting_allowance, 0)
              + coalesce(c.management_allowance, 0) + coalesce(c.transportation_adjustment, 0)
              + coalesce(c.overtime_pay, 0) + coalesce(c.advance_expense, 0)
              + coalesce(c.advance_expense2, 0) + coalesce(d.company_total_amount, 0)) as employee_cost,
            count(*) filter (where c.employee_no is null) as missing_condition_count,
            count(*) filter (where d.transfer_payment_amount is null) as missing_transfer_count,
            count(*) filter (where d.company_total_amount is null) as missing_company_cost_count
          from monthly_salary_details d
          left join monthly_salary_calculation_conditions c
            on c.employee_no = d.employee_no and c.payment_month = d.payment_month
          left join lateral employee_masters_for_month(d.payment_month) e on e.employee_no = d.employee_no
          group by d.employee_no, (to_date(replace(d.payment_month, '-', '') || '01', 'YYYYMMDD') - interval '1 month')::date
        )
        select a.*, c.contract_no, c.project_affiliation, c.engineer_proposer,
          c.contract_amount, c.engineer_unit_price, c.customer_name, c.engineer_company_name,
          coalesce(c.contract_count, 0) as contract_count,
          coalesce(e.employee_name, a.engineer_no, '未登録') as engineer_display_name,
          case when a.engineer_no = '0028' and a.month < date '2026-09-01' then '2'
            when a.engineer_no in ('B001', 'B019') then '1'
            else e.employee_type end as employee_type,
          case a.engineer_no when 'B001' then '0007' when 'B019' then '0030'
            else a.engineer_no end as payroll_employee_no,
          coalesce(p.payroll_count, 0) as payroll_count, coalesce(p.individual_payment, 0) as individual_payment,
          coalesce(p.employee_cost, 0) as employee_cost, coalesce(p.missing_condition_count, 0) as missing_condition_count,
          coalesce(p.missing_transfer_count, 0) as missing_transfer_count,
          coalesce(p.missing_company_cost_count, 0) as missing_company_cost_count
        from actuals a
        left join lateral (
          select ct.*, count(*) over () as contract_count from contracts ct
          where ct.engineer_name = a.engineer_no
            and ct.contract_start_date <= a.month + interval '1 month' - interval '1 day'
            and (ct.contract_end_date is null or ct.contract_end_date >= a.month)
          order by ct.contract_no limit 1
        ) c on true
        left join lateral employee_masters_for_period(a.month,
          (a.month + interval '1 month' - interval '1 day')::date) e on e.employee_no = a.engineer_no
        left join payroll p on p.employee_no = case a.engineer_no
          when 'B001' then '0007' when 'B019' then '0030' else a.engineer_no end and p.month = a.month
        order by a.month, a.engineer_no
        """, Map.of("start", start.atDay(1), "end", end.atEndOfMonth()), (rs, rowNum) -> {
      String engineer = rs.getString("engineer_no");
      YearMonth month = YearMonth.from(rs.getDate("month").toLocalDate());
      String label = month + " " + rs.getString("engineer_display_name") + "（" + engineer + "）";
      if (rs.getInt("missing_invoice_amount_count") > 0) {
        warnings.add(label + "：金額未登録の売上・支払明細があるため集計対象外"); return 0;
      }
      if (rs.getInt("contract_count") != 1) {
        warnings.add(label + "：該当契約が" + rs.getInt("contract_count") + "件のため集計対象外");
        return 0;
      }
      String type = rs.getString("employee_type");
      if (!List.of("0", "1", "2").contains(type == null ? "" : type)) {
        warnings.add(label + "：社員区分が確定できないため集計対象外"); return 0;
      }
      if ("2".equals(type) && rs.getInt("bp_payment_count") == 0) {
        warnings.add(label + "：BP支払明細がないため集計対象外"); return 0;
      }
      if (!"2".equals(type) && rs.getInt("payroll_count") != 1) {
        warnings.add(label + "：翌月の給与明細が" + rs.getInt("payroll_count") + "件のため集計対象外"); return 0;
      }
      if ("0".equals(type) && rs.getInt("missing_condition_count") > 0) {
        warnings.add(label + "：翌月の給与計算条件がないため集計対象外"); return 0;
      }
      if (("1".equals(type) && rs.getInt("missing_transfer_count") > 0)
          || ("0".equals(type) && rs.getInt("missing_company_cost_count") > 0)) {
        warnings.add(label + "：翌月給与の支払額または会社負担額が未登録のため集計対象外"); return 0;
      }
      BigDecimal payment = rs.getBigDecimal("2".equals(type) ? "bp_payment"
          : "1".equals(type) ? "individual_payment" : "employee_cost");
      String owner = rs.getString("project_affiliation");
      String proposer = rs.getString("engineer_proposer");
      BigDecimal allowance = BigDecimal.ZERO;
      if (!"0".equals(type) && ProjectAffiliationProfitCalculator.MEMBERS.containsKey(owner)
          && !"株式会社CMCソフト".equals(rs.getString("engineer_company_name") == null ? ""
              : rs.getString("engineer_company_name").trim())) {
        if (rs.getBigDecimal("contract_amount") == null || rs.getBigDecimal("engineer_unit_price") == null) {
          warnings.add(label + "：手当算出用の契約金額または技術者単価が未登録のため集計対象外"); return 0;
        }
        allowance = SalesController.calculateContributionAllowance(engineer, month,
            rs.getBigDecimal("contract_amount"), rs.getBigDecimal("engineer_unit_price"), rs.getString("customer_name"));
      }
      if (!ProjectAffiliationProfitCalculator.MEMBERS.containsKey(owner)
          || !ProjectAffiliationProfitCalculator.MEMBERS.containsKey(proposer)) {
        warnings.add(label + "：案件所属または提案者が対象5名に未設定（該当配分は未配分）");
      }
      BigDecimal sales = rs.getBigDecimal("sales");
      details.add(new ProfitContributionDetail(month.toString(), engineer, rs.getString("engineer_display_name"),
          type, rs.getInt("contract_no"), owner, proposer, sales, payment, allowance,
          sales.subtract(payment).subtract(allowance), rs.getString("payroll_employee_no"), rs.getString("customer_name")));
      return 0;
    });
    List<ProjectAffiliationProfitRow> rows = ProjectAffiliationProfitCalculator.calculate(details);
    BigDecimal actual = details.stream().map(ProfitContributionDetail::profitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal allocated = rows.stream().map(ProjectAffiliationProfitRow::profitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new Result(rows, details, warnings, actual.subtract(allocated));
  }
}
