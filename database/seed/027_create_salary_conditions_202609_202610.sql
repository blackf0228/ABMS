begin;

-- 2026-08の確定済み給与明細に対応する計算条件を基準にする。
-- 0028は2026-09時点では未入社のため、9月分から明示的に除外する。
delete from monthly_salary_details
where employee_no = '0028'
  and payment_month = '202609';

delete from monthly_salary_calculation_conditions
where employee_no = '0028'
  and payment_month = '202609';

insert into monthly_salary_calculation_conditions (
  employee_no,
  payment_month,
  base_salary,
  commuting_allowance,
  management_allowance,
  transportation_adjustment,
  overtime_pay,
  advance_expense,
  advance_expense2,
  other_amount,
  other_item_name
)
select
  source.employee_no,
  target.payment_month,
  source.base_salary,
  case when source.employee_no = '0028' then 0 else source.commuting_allowance end,
  source.management_allowance,
  case when source.employee_no = '0028' then 0 else source.transportation_adjustment end,
  source.overtime_pay,
  source.advance_expense,
  source.advance_expense2,
  source.other_amount,
  source.other_item_name
from monthly_salary_calculation_conditions source
join monthly_salary_details detail
  on detail.employee_no = source.employee_no
 and detail.payment_month = source.payment_month
cross join (values ('202609'), ('202610')) as target(payment_month)
where source.payment_month = '202608'
  and not (target.payment_month = '202609' and source.employee_no = '0028')
on conflict (employee_no, payment_month) do update set
  base_salary = excluded.base_salary,
  commuting_allowance = excluded.commuting_allowance,
  management_allowance = excluded.management_allowance,
  transportation_adjustment = excluded.transportation_adjustment,
  overtime_pay = excluded.overtime_pay,
  advance_expense = excluded.advance_expense,
  advance_expense2 = excluded.advance_expense2,
  other_amount = excluded.other_amount,
  other_item_name = excluded.other_item_name;

-- 今回の対象月は保険料率・社会保険等級・住民税が8月から変わらないため、
-- 既存の給与計算APIで確認した8月明細を対象月へ引き継ぐ。
insert into monthly_salary_details (
  employee_no,
  payment_month,
  employment_insurance_amount,
  pension_insurance_amount,
  health_insurance_amount,
  income_tax,
  child_support_amount,
  taxable_income,
  transfer_payment_amount,
  company_employment_insurance_amount,
  workers_accident_insurance_amount,
  company_pension_insurance_amount,
  company_health_insurance_amount,
  company_child_support_amount,
  company_total_amount
)
select
  source.employee_no,
  target.payment_month,
  coalesce(source.employment_insurance_amount, 0),
  coalesce(source.pension_insurance_amount, 0),
  coalesce(source.health_insurance_amount, 0),
  coalesce(source.income_tax, 0),
  coalesce(source.child_support_amount, 0),
  coalesce(source.taxable_income, 0),
  coalesce(source.transfer_payment_amount, 0),
  coalesce(source.company_employment_insurance_amount, 0),
  coalesce(source.workers_accident_insurance_amount, 0),
  coalesce(source.company_pension_insurance_amount, 0),
  coalesce(source.company_health_insurance_amount, 0),
  coalesce(source.company_child_support_amount, 0),
  coalesce(source.company_total_amount, 0)
from monthly_salary_details source
cross join (values ('202609'), ('202610')) as target(payment_month)
where source.payment_month = '202608'
  and not (target.payment_month = '202609' and source.employee_no = '0028')
on conflict (employee_no, payment_month) do update set
  employment_insurance_amount = excluded.employment_insurance_amount,
  pension_insurance_amount = excluded.pension_insurance_amount,
  health_insurance_amount = excluded.health_insurance_amount,
  income_tax = excluded.income_tax,
  child_support_amount = excluded.child_support_amount,
  taxable_income = excluded.taxable_income,
  transfer_payment_amount = excluded.transfer_payment_amount,
  company_employment_insurance_amount = excluded.company_employment_insurance_amount,
  workers_accident_insurance_amount = excluded.workers_accident_insurance_amount,
  company_pension_insurance_amount = excluded.company_pension_insurance_amount,
  company_health_insurance_amount = excluded.company_health_insurance_amount,
  company_child_support_amount = excluded.company_child_support_amount,
  company_total_amount = excluded.company_total_amount;

commit;
