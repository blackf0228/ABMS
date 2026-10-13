# Run only against the dedicated copied verification DB on port 9001.
$ErrorActionPreference = 'Stop'
$employeeTestApi = 'http://localhost:9001/api'
function Assert-History($condition, $message) {
  if (-not $condition) { throw $message }
}
function Send-HistoryRequest($method, $path, $body) {
  $jsonBytes = [Text.Encoding]::UTF8.GetBytes(($body | ConvertTo-Json))
  Invoke-RestMethod -Uri "$employeeTestApi/$path" -Method $method -ContentType 'application/json; charset=utf-8' -Body $jsonBytes
}
$beforeContracts = Invoke-RestMethod "$employeeTestApi/contracts"
$beforeSlips = Invoke-RestMethod "$employeeTestApi/payroll/slips?paymentMonth=202606"
$request = @{
  employeeNo='HISTORY_API_TEST'; employeeName='History Test'; lastName='History'; firstName='Test';
  employeeType='0'; baseSalary=100000; dependentCount=0; deductionCount=0;
  consumptionTaxRate=0; deletionFlag='0'; effectiveStartDate='2025-10-01'; effectiveEndDate='2025-10-02'
}
$created = Send-HistoryRequest 'Post' 'employees' $request
Assert-History ($created.effectiveEndDate -eq '9999-12-31') 'Creation did not force open-ended end date'
$request.effectiveStartDate = '2026-01-01'
$request.baseSalary = 200000
$null = Send-HistoryRequest 'Put' 'employees/HISTORY_API_TEST' $request
$request.effectiveStartDate = '2099-01-01'
$request.baseSalary = 300000
$null = Send-HistoryRequest 'Put' 'employees/HISTORY_API_TEST' $request
$history = Invoke-RestMethod "$employeeTestApi/employees/HISTORY_API_TEST/history"
Assert-History ($history.Count -eq 3) 'Expected 3 history versions'
Assert-History ($history[0].effectiveStartDate -eq '2099-01-01') 'Future history not first'
Assert-History ($history[1].effectiveEndDate -eq '2098-12-31') 'Previous end date not prior day'
Assert-History ($history[2].effectiveEndDate -eq '2025-12-31') 'Year boundary end date incorrect'
$current = @((Invoke-RestMethod "$employeeTestApi/employees") | Where-Object employeeNo -eq 'HISTORY_API_TEST')
Assert-History ($current.Count -eq 1 -and $current[0].baseSalary -eq 200000) 'Current list incorrectly selected future version'
$rejected = $false
try { $null = Send-HistoryRequest 'Put' 'employees/HISTORY_API_TEST' $request }
catch {
  if ([int]$_.Exception.Response.StatusCode -eq 400) { $rejected=$true } else { throw }
}
Assert-History $rejected 'Same-date correction was not rejected'
$request.employeeNo='HISTORY_FUTURE'; $request.effectiveStartDate='2099-01-01'
$null = Send-HistoryRequest 'Post' 'employees' $request
Assert-History (@((Invoke-RestMethod "$employeeTestApi/employees") | Where-Object employeeNo -eq 'HISTORY_FUTURE').Count -eq 0) 'Future-only employee appeared in current list'
# Add a genuine historic revision in the copied data to test downstream joins.
$employee = (Invoke-RestMethod "$employeeTestApi/employees") | Where-Object employeeNo -eq '0001'
$employee.effectiveStartDate='2026-03-15'; $employee.baseSalary=650000; $employee.deductionCount=3
$null = Send-HistoryRequest 'Put' 'employees/0001' $employee
$copiedConditions = Invoke-RestMethod "$employeeTestApi/payroll/calculations/conditions/copy-previous?paymentMonth=202604"
$copiedEmployee = $copiedConditions.rows | Where-Object employeeNo -eq '0001'
Assert-History ($copiedEmployee.baseSalary -eq 650000) 'Previous-month copy did not use the target-month master salary'
$afterContracts = Invoke-RestMethod "$employeeTestApi/contracts"
$afterSlips = Invoke-RestMethod "$employeeTestApi/payroll/slips?paymentMonth=202606"
Assert-History ($beforeContracts.totalElements -eq $afterContracts.totalElements) 'Contract history join duplicated records'
Assert-History ($beforeSlips.Count -eq $afterSlips.Count) 'Payroll history join duplicated records'
$null = Invoke-RestMethod "$employeeTestApi/payroll/slips/0001/202606"
$conditions = Invoke-RestMethod "$employeeTestApi/payroll/calculations/conditions?paymentMonth=202606"
$null = Invoke-RestMethod "$employeeTestApi/payroll/calculations/salary-details/draft?paymentMonth=202606"
$null = Invoke-RestMethod "$employeeTestApi/payroll/shareholder-salaries?employeeNo=0001"
$null = Invoke-RestMethod "$employeeTestApi/attendance?workingMonth=202606"
Write-Output 'Employee history API checks passed: registration, corrections, future dates, history ordering, date rejection, payroll and contract joins, target-month salary copy.'
