# Read-only: uses SELECT and the GET calculation preview endpoint. Does not save salaries.
$ErrorActionPreference = 'Stop'
$query = @'
SELECT json_build_object(
  'employeeNo',d.employee_no,'paymentMonth',d.payment_month,
  'storedTransfer',d.transfer_payment_amount,
  'storedEmploymentInsurance',d.employment_insurance_amount,
  'storedPension',d.pension_insurance_amount,'storedHealth',d.health_insurance_amount,
  'storedIncomeTax',d.income_tax,'storedChildSupport',d.child_support_amount)
FROM abms.monthly_salary_details d ORDER BY d.payment_month,d.employee_no;
'@
function Read-AuditDb($sql) {
  $lines = @(docker exec abms-postgres psql -U abms -d abms -At -v ON_ERROR_STOP=1 -c $sql)
  if ($LASTEXITCODE -ne 0) { throw 'DB read failed' }
  return $lines
}
$hashQuery = @'
SELECT 'conditions',count(*),md5(string_agg(to_jsonb(t)::text,'|' ORDER BY employee_no,payment_month))
FROM abms.monthly_salary_calculation_conditions t
UNION ALL
SELECT 'details',count(*),md5(string_agg(to_jsonb(t)::text,'|' ORDER BY employee_no,payment_month))
FROM abms.monthly_salary_details t;
'@
$beforeHash = @(Read-AuditDb $hashQuery)
$storedRows = @(Read-AuditDb $query | ForEach-Object { $_ | ConvertFrom-Json })
$draftMap = @{}
foreach ($month in @($storedRows.paymentMonth | Sort-Object -Unique)) {
  $drafts = Invoke-RestMethod "http://localhost:9000/api/payroll/calculations/salary-details/draft?paymentMonth=$month"
  foreach ($draft in $drafts) {
    $key = "$($draft.paymentMonth)/$($draft.employeeNo)"
    if ($draftMap.ContainsKey($key)) { throw "Duplicate draft: $key" }
    $draftMap[$key] = $draft
  }
}
$results = @(foreach ($stored in $storedRows) {
  $key = "$($stored.paymentMonth)/$($stored.employeeNo)"
  $draft = $draftMap[$key]
  if ($null -eq $draft) {
    [pscustomobject]@{paymentMonth=$stored.paymentMonth;employeeNo=$stored.employeeNo;status='NO_DRAFT';stored=$stored.storedTransfer}
    continue
  }
  $difference = if ($null -eq $stored.storedTransfer) { $null } else {
    [decimal]$draft.transferPaymentAmount - [decimal]$stored.storedTransfer
  }
  [pscustomobject]@{
    paymentMonth=$stored.paymentMonth; employeeNo=$stored.employeeNo
    employeeName=$draft.employeeName; employeeType=$draft.employeeType; baseSalary=$draft.baseSalary
    stored=$stored.storedTransfer; recalculated=$draft.transferPaymentAmount; difference=$difference
    status=$(if ($null -eq $stored.storedTransfer) {'NULL_STORED'} elseif ($difference -ne 0) {'MISMATCH'} else {'MATCH'})
    employmentDelta=([decimal]$draft.employmentInsuranceAmount-[decimal]$stored.storedEmploymentInsurance)
    pensionDelta=([decimal]$draft.pensionInsuranceAmount-[decimal]$stored.storedPension)
    healthDelta=([decimal]$draft.healthInsuranceAmount-[decimal]$stored.storedHealth)
    incomeTaxDelta=([decimal]$draft.incomeTax-[decimal]$stored.storedIncomeTax)
    childSupportDelta=([decimal]$draft.childSupportAmount-[decimal]$stored.storedChildSupport)
    residentTax=$draft.residentTaxAmount
  }
})
$afterHash = @(Read-AuditDb $hashQuery)
if (($beforeHash -join '|') -ne ($afterHash -join '|')) { throw 'Salary data changed during audit; rerun required' }
[pscustomobject]@{
  totalStored=$storedRows.Count; draftCount=$draftMap.Count; dataUnchanged=$true; hashes=$afterHash
  summary=@($results | Group-Object status | Select-Object Name,Count)
  months=@($results | Group-Object paymentMonth | ForEach-Object {
    [pscustomobject]@{month=$_.Name;total=$_.Count;
      mismatch=@($_.Group | Where-Object status -eq 'MISMATCH').Count;
      uncomparable=@($_.Group | Where-Object status -eq 'NO_DRAFT').Count}
  })
  columns=@('paymentMonth','employeeNo','employeeName','employeeType','baseSalary','stored','recalculated','difference','status','employmentDelta','pensionDelta','healthDelta','incomeTaxDelta','childSupportDelta','residentTax')
  allRows=@($results | ForEach-Object {
    ,@($_.paymentMonth,$_.employeeNo,$_.employeeName,$_.employeeType,$_.baseSalary,
      $_.stored,$_.recalculated,$_.difference,$_.status,$_.employmentDelta,
      $_.pensionDelta,$_.healthDelta,$_.incomeTaxDelta,$_.childSupportDelta,$_.residentTax)
  })
} | ConvertTo-Json -Depth 7 -Compress
