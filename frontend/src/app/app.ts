import { CommonModule } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, forkJoin, map, of } from 'rxjs';

type ViewName = 'register' | 'search' | 'employmentRates' | 'socialPremiums' | 'salaryConditions' | 'salaryDetailPreview' | 'payrollSearch' | 'payrollDetail' | 'shareholderSalaryList' | 'contractList' | 'attendanceSearch' | 'attendanceBulk' | 'salesList' | 'salesAnalysis' | 'profitAnalysis' | 'fiscalProfitConfirmation' | 'allowanceManagement' | 'customerList' | 'employeeList' | 'offBookExpenseList';

interface BillingRequest {
  no?: number;
  requestSource: string;
  requestDestination: string;
  requestMonth: string;
  engineer: string;
  amount: number | null;
  remarks: string;
}

interface EmploymentInsuranceRate {
  no: number;
  fiscalYear: string;
  effectiveStartDate: string;
  effectiveEndDate: string;
  businessCategory: string;
  workerRate: number;
  employerRate: number;
  totalRate: number;
  remarks: string;
}

interface SocialInsurancePremiumAmount {
  no: number;
  effectiveMonth: string;
  paymentMonth: string;
  prefecture: string;
  grade: number;
  pensionGrade: number | null;
  standardMonthlyRemuneration: number;
  monthlyRemunerationFrom: number | null;
  monthlyRemunerationTo: number | null;
  healthInsuranceRate: number | null;
  healthInsuranceTotalAmount: number | null;
  healthInsuranceEmployeeAmount: number | null;
  healthInsuranceEmployerAmount: number | null;
  careInsuranceRate: number | null;
  careInsuranceTotalAmount: number | null;
  careInsuranceEmployeeAmount: number | null;
  careInsuranceEmployerAmount: number | null;
  childSupportRate: number | null;
  childSupportTotalAmount: number | null;
  childSupportEmployeeAmount: number | null;
  childSupportEmployerAmount: number | null;
  pensionInsuranceRate: number | null;
  pensionInsuranceTotalAmount: number | null;
  pensionInsuranceEmployeeAmount: number | null;
  pensionInsuranceEmployerAmount: number | null;
}

interface PayrollSlipSearchForm {
  paymentMonth: string;
  employeeNo: string;
  employeeName: string;
}

interface PayrollSlipListItem {
  employeeNo: string;
  employeeName: string;
  paymentMonth: string;
  baseSalary: number | null;
  commutingAllowance: number | null;
  managementAllowance: number | null;
  transportationAdjustment: number | null;
  overtimePay: number | null;
  advanceExpense: number | null;
  advanceExpense2: number | null;
  otherAmount: number | null;
  transferPaymentAmount: number | null;
}

interface PayrollSlipDetail extends PayrollSlipListItem {
  employeeType: string;
  otherItemName: string | null;
  employmentInsuranceAmount: number | null;
  pensionInsuranceAmount: number | null;
  healthInsuranceAmount: number | null;
  incomeTax: number | null;
  childSupportAmount: number | null;
  residentTaxAmount: number | null;
  taxableIncome: number | null;
}

interface PayrollPdfResponse {
  fileName: string;
  filePath: string;
}

interface OrderDocumentResponse {
  fileName: string;
  filePath: string;
}

interface OrderDocumentBulkResponse {
  createdCount: number;
  fileNames: string[];
  outputDirectory: string;
}

interface InvoiceDocumentResponse {
  fileName: string;
  filePath: string;
  detailCount: number;
}

interface InvoiceDocumentBulkResponse {
  createdCount: number;
  fileNames: string[];
  outputDirectory: string;
}

interface OrderDocumentDialog {
  title: string;
  message: string;
  fileName: string;
  filePath: string;
}

type DocumentCreationAction =
  | { type: 'order'; bulk: false; row: ContractListItem }
  | { type: 'order'; bulk: true }
  | { type: 'invoice'; bulk: false; row: ContractListItem }
  | { type: 'invoice'; bulk: true };

interface SalaryConditionRow {
  employeeNo: string;
  employeeName: string;
  employeeType: string;
  paymentMonth: string;
  baseSalary: number | null;
  consumptionTaxRate: number | null;
  commutingAllowance: number | null;
  managementAllowance: number | null;
  transportationAdjustment: number | null;
  overtimePay: number | null;
  advanceExpense: number | null;
  advanceExpense2: number | null;
  otherAmount: number | null;
  otherItemName: string;
  editableIdentity?: boolean;
}

interface SalaryConditionResponse {
  paymentMonth: string;
  previousPaymentMonth: string;
  hasData: boolean;
  rows: SalaryConditionRow[];
}

interface SalaryDetailDraftRow {
  employeeNo: string;
  employeeName: string;
  employeeType: string;
  paymentMonth: string;
  baseSalary: number | null;
  remunerationAmount: number | null;
  supportTotalAmount: number | null;
  employmentInsuranceAmount: number | null;
  pensionInsuranceAmount: number | null;
  healthInsuranceAmount: number | null;
  residentTaxAmount: number | null;
  childSupportAmount: number | null;
  taxableIncome: number | null;
  incomeTax: number | null;
  deductionTotalAmount: number | null;
  transferPaymentAmount: number | null;
  otherAmount: number | null;
}

interface CustomerOption {
  companyId: string;
  companyName: string;
}

interface ContractSearchForm {
  engineerName: string;
  operationStatus: string;
  contractStartDateFrom: string;
  contractEndDateTo: string;
  effectiveMonthFrom: string;
  effectiveMonthTo: string;
  customerId: string;
  engineerCompanyId: string;
  projectAffiliation: string;
  engineerProposer: string;
}

interface ContractListItem {
  contractNo: number;
  engineerType: string | null;
  engineerName: string;
  contractAmount: number | null;
  customerName: string;
  engineerUnitPrice: number | null;
  engineerCompanyName: string | null;
  operationStatus: string;
  contractStartDate: string | null;
  contractEndDate: string | null;
  projectAffiliation: string;
  engineerProposer: string;
  settlementLowerHour: number | null;
  settlementUpperHour: number | null;
}

interface ContractPageResponse {
  items: ContractListItem[];
  totalElements: number;
  page: number;
  size: number;
  totalPages: number;
}

interface AttendanceItem {
  contractNo: number;
  engineerName: string;
  workingMonth: string;
  engineerCompanyName: string | null;
  customerName: string | null;
  workingHours: number | null;
  remarks: string | null;
}

interface AttendanceBulkRow {
  contractNo: number;
  engineerName: string;
  customerName: string | null;
  workingHours: number | null;
  remarks: string;
}

interface OffBookExpense {
  expenseNo: number;
  receivedDate: string;
  handlerEmployeeNo: string;
  handlerName: string;
  amount: number | null;
  currency: string;
  reason: string;
}

interface OffBookExpenseForm {
  receivedDate: string;
  handlerEmployeeNo: string;
  amount: number | null;
  currency: string;
  reason: string;
}

interface SalesSearchForm {
  startMonth: string;
  endMonth: string;
  billingDestinationCompanyId: string;
}

interface SalesAnalysisSearchForm {
  startMonth: string;
  endMonth: string;
  billingDestinationCompanyId: string;
}

interface ProfitAnalysisSearchForm {
  startMonth: string;
  endMonth: string;
}

interface SalesSummaryItem {
  no: number;
  billingMonth: string;
  billingDestinationCompanyId: string;
  billingDestinationCompanyName: string;
  billingAmount: number | null;
}

interface SalesAnalysisItem {
  billingDestinationCompanyId: string;
  billingDestinationCompanyName: string;
  workingPersonCount: number | null;
  salesAmount: number | null;
}

interface SalesMonthlyAnalysisItem {
  billingMonth: string;
  workingPersonCount: number | null;
  salesAmount: number | null;
}

interface ProfitAnalysisItem {
  postingMonth: string;
  workingPersonCount: number | null;
  profitAmount: number | null;
}

interface FiscalProfitAnalysisRow {
  itemName: string;
  monthlyAmounts: number[];
  totalAmount: number | null;
}

interface ProjectAffiliationProfitRow {
  employeeNo: string;
  projectAffiliation: string;
  profitAmount: number | null;
  percentage: number | null;
}

interface FiscalProfitAnalysisResponse {
  fiscalStartMonth: string;
  fiscalEndMonth: string;
  months: string[];
  rows: FiscalProfitAnalysisRow[];
  companyProfitTotal: number | null;
  projectAffiliationProfits: ProjectAffiliationProfitRow[];
}

interface AllowanceAnalysisRow {
  employeeNo: string;
  employeeName: string;
  allowanceName: string;
  monthlyAmounts: { month: string | null; amount: number | null }[];
  subtotal: number | null;
  total: number | null;
}

interface AllowanceDisplayRow extends AllowanceAnalysisRow {
  isRecruitment: boolean;
  isEmployeeGroupStart: boolean;
  employeeTotal: number;
}

interface AllowanceAnalysisResponse {
  fiscalStartMonth: string;
  fiscalEndMonth: string;
  months: string[];
  rows: AllowanceAnalysisRow[];
  recruitmentAmounts?: Record<string, Record<string, number>>;
}

interface AllowanceAnalysisDetail {
  month: string;
  contractNo: number;
  engineerName: string;
  contractAmount: number | null;
  engineerUnitPrice: number | null;
  allowanceAmount: number | null;
}

interface ShareholderSalaryRow {
  paymentMonth: string;
  contractAmount: number | null;
  overtimeAmount: number | null;
  baseSalary: number | null;
  advanceExpense2: number | null;
  companyTotalAmount: number | null;
  overUnder: number | null;
  companyTotalManual: boolean;
}

interface ShareholderSalaryResponse {
  employeeNo: string;
  employeeName: string;
  fiscalStartMonth: string;
  fiscalEndMonth: string;
  rows: ShareholderSalaryRow[];
  totalOverUnder: number | null;
}

interface SalesDetailItem {
  engineerName: string;
  billingAmount: number | null;
}

interface SalesDetailResponse {
  billingMonth: string;
  billingDestinationCompanyId: string;
  billingDestinationCompanyName: string;
  items: SalesDetailItem[];
}

interface EmployeeMaster {
  employeeNo: string;
  employeeName: string;
  lastName: string;
  firstName: string;
  employeeType: string;
  baseSalary: number | null;
  dependentCount: number | null;
  deductionCount: number | null;
  consumptionTaxRate: number | null;
  deletionFlag: string;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class AppComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly billingApiUrl = 'http://localhost:9000/api/billing-requests';
  private readonly insuranceApiUrl = 'http://localhost:9000/api/insurance';
  private readonly payrollApiUrl = 'http://localhost:9000/api/payroll/slips';
  private readonly salaryCalculationApiUrl = 'http://localhost:9000/api/payroll/calculations';
  private readonly customersApiUrl = 'http://localhost:9000/api/customers';
  private readonly contractsApiUrl = 'http://localhost:9000/api/contracts';
  private readonly salesApiUrl = 'http://localhost:9000/api/sales';
  private readonly employeesApiUrl = 'http://localhost:9000/api/employees';
  private readonly attendanceApiUrl = 'http://localhost:9000/api/attendance';
  private readonly offBookExpensesApiUrl = 'http://localhost:9000/api/off-book-expenses';
  private readonly shareholderSalariesApiUrl = 'http://localhost:9000/api/payroll/shareholder-salaries';

  activeView: ViewName = 'contractList';
  registerMessage = '';
  searchMessage = '';
  masterMessage = '';
  registerErrors: string[] = [];
  searchResults: BillingRequest[] = [];
  employmentRates: EmploymentInsuranceRate[] = [];
  socialPremiums: SocialInsurancePremiumAmount[] = [];
  payrollMessage = '';
  payrollResults: PayrollSlipListItem[] = [];
  selectedPayrollSlip: PayrollSlipDetail | null = null;
  payrollBulkPrinting = false;
  salaryConditionMessage = '';
  salaryConditionPaymentMonth = '';
  salaryConditionRows: SalaryConditionRow[] = [];
  salaryDetailRows: SalaryDetailDraftRow[] = [];
  salaryConditionsSaved = false;
  salaryDetailsCreated = false;
  salaryDetailsRegistered = false;
  salaryConditionsDirty = false;
  editingSalaryDetail: SalaryDetailDraftRow | null = null;
  editingSalaryDetailIndex: number | null = null;
  private readonly employeeTypeByNo = new Map<string, string>();
  private readonly employeeMasterByNo = new Map<string, EmployeeMaster>();
  contractMessage = '';
  orderDocumentDialog: OrderDocumentDialog | null = null;
  documentCreationAction: DocumentCreationAction | null = null;
  documentCreationMonth = this.currentMonthInput();
  documentCreationMonthOptions = this.createMonthOptions();
  customerOptions: CustomerOption[] = [];
  contractResults: ContractListItem[] = [];
  editingContract: ContractListItem | null = null;
  contractModalMode: 'create' | 'edit' | null = null;
  contractTotalElements = 0;
  contractPage = 0;
  contractSize = 30;
  contractTotalPages = 0;
  readonly contractStaffEmployeeNos = ['0001', '0002', '0006', '0010', '0011'];
  readonly contractEngineerTypeOptions = [
    { value: '0', label: '0:正社員' },
    { value: '1', label: '1:BP' },
    { value: '2', label: '2:個人事業主' }
  ];
  salesMessage = '';
  salesResults: SalesSummaryItem[] = [];
  selectedSalesDetail: SalesDetailResponse | null = null;
  salesAnalysisMode: 'postingMonth' | 'customer' = 'postingMonth';
  salesAnalysisMessage = '';
  salesAnalysisResults: SalesAnalysisItem[] = [];
  salesMonthlyAnalysisResults: SalesMonthlyAnalysisItem[] = [];
  profitAnalysisMessage = '';
  profitAnalysisResults: ProfitAnalysisItem[] = [];
  fiscalProfitMessage = '';
  fiscalProfitStartMonth = '';
  fiscalProfitEndMonth = '';
  fiscalProfitMonths: string[] = [];
  fiscalProfitRows: FiscalProfitAnalysisRow[] = [];
  fiscalProfitCompanyTotal = 0;
  projectAffiliationProfitRows: ProjectAffiliationProfitRow[] = [];
  projectAffiliationProfitTotal = 0;
  allowanceAnalysisMessage = '';
  allowanceAnalysisMonths: string[] = [];
  allowanceAnalysisResults: AllowanceAnalysisRow[] = [];
  allowanceAnalysisDisplayResults: AllowanceDisplayRow[] = [];
  allowanceAnalysisDetails: AllowanceAnalysisDetail[] = [];
  allowanceAnalysisDetailTitle = '';
  shareholderSalaryEmployeeNo = '0010';
  shareholderSalaryEmployeeName = '';
  shareholderSalaryFiscalStartMonth = '';
  shareholderSalaryFiscalEndMonth = '';
  shareholderSalaryRows: ShareholderSalaryRow[] = [];
  shareholderSalaryTotal = 0;
  shareholderSalaryMessage = '';
  readonly shareholderSalaryEmployees = [
    { employeeNo: '0010', employeeName: '毛 杰' },
    { employeeNo: '0001', employeeName: '王 超' },
    { employeeNo: '0002', employeeName: '小林 和成' },
    { employeeNo: '0006', employeeName: '孫 偉東' },
    { employeeNo: '0011', employeeName: '李 鴻民' }
  ];
  employeeMessage = '';
  employees: EmployeeMaster[] = [];
  includeBpEmployees = false;
  editingEmployee: EmployeeMaster | null = null;
  employeeCreateMode = false;
  customerMessage = '';
  editingCustomer: CustomerOption | null = null;
  customerCreateMode = false;
  attendanceMessage = '';
  attendanceResults: AttendanceItem[] = [];
  attendanceRegisterRows: AttendanceBulkRow[] = [];
  offBookExpenseMessage = '';
  offBookExpenses: OffBookExpense[] = [];
  editingOffBookExpense: (OffBookExpenseForm & { expenseNo: number }) | null = null;
  offBookExpenseCreateMode = false;
  offBookExpenseSearchForm = {
    receivedDateFrom: '',
    receivedDateTo: '',
    handlerEmployeeNo: ''
  };

  registerForm: BillingRequest = this.createEmptyForm();
  searchForm: BillingRequest = this.createEmptyForm();
  payrollSearchForm: PayrollSlipSearchForm = {
    paymentMonth: '',
    employeeNo: '',
    employeeName: ''
  };
  contractSearchForm: ContractSearchForm = {
    engineerName: '',
    operationStatus: '',
    contractStartDateFrom: '',
    contractEndDateTo: '',
    effectiveMonthFrom: '',
    effectiveMonthTo: '',
    customerId: '',
    engineerCompanyId: '',
    projectAffiliation: '',
    engineerProposer: ''
  };
  salesSearchForm: SalesSearchForm = {
    startMonth: this.currentMonthInput(),
    endMonth: this.currentMonthInput(),
    billingDestinationCompanyId: ''
  };
  salesAnalysisSearchForm: SalesAnalysisSearchForm = {
    startMonth: this.currentMonthInput(),
    endMonth: this.currentMonthInput(),
    billingDestinationCompanyId: ''
  };
  profitAnalysisSearchForm: ProfitAnalysisSearchForm = {
    startMonth: this.currentMonthInput(),
    endMonth: this.currentMonthInput()
  };
  attendanceSearchForm = {
    engineerName: '',
    workingMonth: ''
  };
  attendanceRegisterMonth = this.currentMonthInput();

  ngOnInit(): void {
    this.loadCustomerOptions();
    this.loadEmployeeTypes();
  }

  switchView(view: ViewName): void {
    this.activeView = view;
    this.registerMessage = '';
    this.searchMessage = '';
    this.masterMessage = '';
    this.payrollMessage = '';
    this.salaryConditionMessage = '';
    this.contractMessage = '';
    this.orderDocumentDialog = null;
    this.documentCreationAction = null;
    this.salesMessage = '';
    this.salesAnalysisMessage = '';
    this.profitAnalysisMessage = '';
    this.fiscalProfitMessage = '';
    this.allowanceAnalysisMessage = '';
    this.shareholderSalaryMessage = '';
    this.allowanceAnalysisDetails = [];
    this.employeeMessage = '';
    this.offBookExpenseMessage = '';
    this.attendanceMessage = '';
    this.editingSalaryDetail = null;
    this.editingContract = null;
    this.contractModalMode = null;

    if (view === 'employmentRates') {
      this.loadEmploymentRates();
    }
    if (view === 'socialPremiums') {
      this.loadSocialPremiums();
    }
    if (view === 'salaryConditions') {
      this.loadSalaryConditions();
    }
    if (view === 'shareholderSalaryList') {
      this.loadShareholderSalaries();
    }
    if (view === 'contractList') {
      this.loadCustomerOptions();
      this.loadEmployeeTypes();
      this.contractResults = [];
      this.editingContract = null;
      this.contractTotalElements = 0;
      this.contractPage = 0;
      this.contractTotalPages = 0;
    }
    if (view === 'salesList') {
      this.loadCustomerOptions();
    }
    if (view === 'salesAnalysis') {
      this.loadCustomerOptions();
    }
    if (view === 'fiscalProfitConfirmation') {
      this.loadFiscalProfitAnalysis();
    }
    if (view === 'allowanceManagement') {
      this.searchAllowanceAnalysis();
    }
    if (view === 'employeeList') {
      this.loadEmployees();
    }
    if (view === 'customerList') {
      this.loadCustomerList();
    }
    if (view === 'offBookExpenseList') {
      this.loadEmployees();
      this.loadOffBookExpenses();
    }
    if (view === 'attendanceSearch' || view === 'attendanceBulk') {
      this.attendanceResults = [];
      this.attendanceRegisterRows = [];
    }
  }

  isContractMenuActive(): boolean {
    return this.activeView === 'contractList';
  }

  isSalesMenuActive(): boolean {
    return this.activeView === 'salesList'
      || this.activeView === 'search'
      || this.activeView === 'register'
      || this.activeView === 'salesAnalysis'
      || this.activeView === 'profitAnalysis'
      || this.activeView === 'fiscalProfitConfirmation'
      || this.activeView === 'allowanceManagement';
  }

  isPayrollMenuActive(): boolean {
    return this.activeView === 'salaryConditions' || this.activeView === 'salaryDetailPreview' || this.activeView === 'payrollSearch' || this.activeView === 'payrollDetail' || this.activeView === 'shareholderSalaryList';
  }

  isCommonMenuActive(): boolean {
    return this.activeView === 'customerList'
      || this.activeView === 'employeeList'
      || this.activeView === 'offBookExpenseList'
      || this.activeView === 'employmentRates'
      || this.activeView === 'socialPremiums';
  }

  isAttendanceMenuActive(): boolean {
    return this.activeView === 'attendanceSearch' || this.activeView === 'attendanceBulk';
  }

  searchAttendance(): void {
    this.attendanceMessage = '';
    let params = new HttpParams();
    if (this.attendanceSearchForm.engineerName.trim()) {
      params = params.set('engineerName', this.attendanceSearchForm.engineerName.trim());
    }
    if (this.attendanceSearchForm.workingMonth) {
      params = params.set('workingMonth', this.attendanceSearchForm.workingMonth);
    }
    this.http.get<AttendanceItem[]>(this.attendanceApiUrl, { params }).subscribe({
      next: (results) => {
        this.attendanceResults = results;
        this.attendanceMessage = `${results.length}件見つかりました。`;
      },
      error: () => {
        this.attendanceMessage = '勤怠時間の検索に失敗しました。';
      }
    });
  }

  loadAttendanceRegisterRows(): void {
    this.attendanceMessage = '';
    if (!this.attendanceRegisterMonth) {
      this.attendanceMessage = '稼働年月を選択してください。';
      return;
    }
    this.http.get<AttendanceItem[]>(`${this.attendanceApiUrl}/active-contracts`, {
      params: new HttpParams().set('workingMonth', this.attendanceRegisterMonth)
    }).subscribe({
      next: (rows) => {
        this.attendanceRegisterRows = rows.map((row) => ({
          contractNo: row.contractNo,
          engineerName: row.engineerName,
          customerName: row.customerName,
          workingHours: row.workingHours ?? 0,
          remarks: row.remarks ?? ''
        }));
        this.attendanceMessage = `${rows.length}件の契約中技術者を読み込みました。`;
      },
      error: () => {
        this.attendanceMessage = '契約中技術者の取得に失敗しました。';
      }
    });
  }

  saveAttendanceBulk(): void {
    if (!this.attendanceRegisterMonth || this.attendanceRegisterRows.length === 0) {
      this.attendanceMessage = '稼働年月と登録対象を確認してください。';
      return;
    }
    this.http.post<void>(`${this.attendanceApiUrl}/bulk`, {
      workingMonth: this.attendanceRegisterMonth,
      rows: this.attendanceRegisterRows.map((row) => ({
        contractNo: row.contractNo,
        engineerName: row.engineerName,
        workingHours: row.workingHours,
        remarks: row.remarks
      }))
    }).subscribe({
      next: () => {
        this.attendanceSearchForm.workingMonth = this.attendanceRegisterMonth;
        this.searchAttendance();
        this.attendanceMessage = '勤怠時間を一括登録しました。';
      },
      error: () => {
        this.attendanceMessage = '勤怠時間の一括登録に失敗しました。';
      }
    });
  }

  clearAttendanceSearch(): void {
    this.attendanceSearchForm = { engineerName: '', workingMonth: '' };
    this.attendanceResults = [];
    this.attendanceMessage = '';
  }

  saveAttendance(row: AttendanceItem): void {
    this.attendanceMessage = '';
    this.http.put<AttendanceItem>(`${this.attendanceApiUrl}/${row.contractNo}`, {
      workingMonth: row.workingMonth,
      workingHours: row.workingHours,
      remarks: row.remarks ?? ''
    }).subscribe({
      next: (updated) => {
        this.attendanceResults = this.attendanceResults.map((item) =>
          item.contractNo === updated.contractNo && item.workingMonth === updated.workingMonth
            ? updated
            : item);
        this.attendanceMessage = '勤怠時間を訂正しました。';
      },
      error: () => {
        this.attendanceMessage = '勤怠時間の訂正に失敗しました。';
      }
    });
  }

  attendanceMonthLabel(value: string): string {
    const compact = value.replace(/[^0-9]/g, '');
    return compact.length === 6 ? `${compact.slice(0, 4)}年${compact.slice(4, 6)}月` : value;
  }

  loadOffBookExpenses(): void {
    this.offBookExpenseMessage = '';
    let params = new HttpParams();
    const search = this.offBookExpenseSearchForm;
    if (search.receivedDateFrom) {
      params = params.set('receivedDateFrom', search.receivedDateFrom);
    }
    if (search.receivedDateTo) {
      params = params.set('receivedDateTo', search.receivedDateTo);
    }
    if (search.handlerEmployeeNo) {
      params = params.set('handlerEmployeeNo', search.handlerEmployeeNo);
    }
    this.http.get<OffBookExpense[]>(this.offBookExpensesApiUrl, { params }).subscribe({
      next: (expenses) => {
        this.offBookExpenses = expenses;
        this.offBookExpenseMessage = `${expenses.length}件見つかりました。`;
      },
      error: () => {
        this.offBookExpenses = [];
        this.offBookExpenseMessage = '勘定外費用の検索に失敗しました。';
      }
    });
  }

  clearOffBookExpenseSearch(): void {
    this.offBookExpenseSearchForm = {
      receivedDateFrom: '',
      receivedDateTo: '',
      handlerEmployeeNo: ''
    };
    this.loadOffBookExpenses();
  }

  openOffBookExpenseCreate(): void {
    this.offBookExpenseMessage = '';
    this.offBookExpenseCreateMode = true;
    this.editingOffBookExpense = {
      expenseNo: 0,
      receivedDate: this.currentDateInput(),
      handlerEmployeeNo: '',
      amount: 0,
      currency: '日本円',
      reason: ''
    };
  }

  openOffBookExpenseEdit(expense: OffBookExpense): void {
    this.offBookExpenseMessage = '';
    this.offBookExpenseCreateMode = false;
    this.editingOffBookExpense = {
      expenseNo: expense.expenseNo,
      receivedDate: expense.receivedDate,
      handlerEmployeeNo: expense.handlerEmployeeNo,
      amount: expense.amount,
      currency: expense.currency,
      reason: expense.reason
    };
  }

  closeOffBookExpenseEdit(): void {
    this.editingOffBookExpense = null;
    this.offBookExpenseCreateMode = false;
  }

  saveOffBookExpense(): void {
    if (!this.editingOffBookExpense) {
      return;
    }
    const form = this.editingOffBookExpense;
    const requestBody = {
      receivedDate: form.receivedDate,
      handlerEmployeeNo: form.handlerEmployeeNo,
      amount: form.amount,
      currency: form.currency,
      reason: form.reason
    };
    const creating = this.offBookExpenseCreateMode;
    const request = this.offBookExpenseCreateMode
      ? this.http.post<OffBookExpense>(this.offBookExpensesApiUrl, requestBody)
      : this.http.put<OffBookExpense>(`${this.offBookExpensesApiUrl}/${form.expenseNo}`, requestBody);
    request.subscribe({
      next: () => {
        this.closeOffBookExpenseEdit();
        this.loadOffBookExpenses();
        this.offBookExpenseMessage = creating
          ? '勘定外費用を登録しました。'
          : '勘定外費用を訂正しました。';
      },
      error: () => {
        this.offBookExpenseMessage = creating
          ? '勘定外費用の登録に失敗しました。'
          : '勘定外費用の訂正に失敗しました。';
      }
    });
  }

  loadCustomerOptions(): void {
    if (this.customerOptions.length > 0) {
      return;
    }
    this.http.get<CustomerOption[]>(this.customersApiUrl).subscribe({
      next: (options) => {
        this.customerOptions = options;
      },
      error: () => {
        this.contractMessage = '\u5f97\u610f\u5148\u4e00\u89a7\u306e\u53d6\u5f97\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002';
      }
    });
  }

  loadCustomerList(): void {
    this.customerMessage = '';
    this.http.get<CustomerOption[]>(this.customersApiUrl).subscribe({
      next: (customers) => {
        this.customerOptions = customers;
        this.customerMessage = `${customers.length}\u4ef6\u8868\u793a\u3057\u307e\u3057\u305f\u3002`;
      },
      error: () => {
        this.customerMessage = '\u53d6\u5f15\u5148\u4e00\u89a7\u306e\u53d6\u5f97\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002';
      }
    });
  }

  openCustomerCreate(): void {
    this.customerMessage = '';
    this.customerCreateMode = true;
    this.editingCustomer = { companyId: '', companyName: '' };
  }

  openCustomerEdit(customer: CustomerOption): void {
    this.customerMessage = '';
    this.customerCreateMode = false;
    this.editingCustomer = { ...customer };
  }

  closeCustomerEdit(): void {
    this.editingCustomer = null;
    this.customerCreateMode = false;
  }

  saveCustomerEdit(): void {
    if (!this.editingCustomer) {
      return;
    }

    const request = this.editingCustomer;
    const creating = this.customerCreateMode;
    const saveRequest = creating
      ? this.http.post<CustomerOption>(this.customersApiUrl, request)
      : this.http.put<CustomerOption>(`${this.customersApiUrl}/${request.companyId}`, request);
    saveRequest.subscribe({
      next: (saved) => {
        this.customerOptions = creating
          ? [...this.customerOptions, saved].sort((left, right) => left.companyId.localeCompare(right.companyId))
          : this.customerOptions.map((customer) => customer.companyId === saved.companyId ? saved : customer);
        this.editingCustomer = null;
        this.customerCreateMode = false;
        this.customerMessage = creating
          ? '\u53d6\u5f15\u5148\u60c5\u5831\u3092\u65b0\u898f\u767b\u9332\u3057\u307e\u3057\u305f\u3002'
          : '\u53d6\u5f15\u5148\u60c5\u5831\u3092\u8a02\u6b63\u3057\u307e\u3057\u305f\u3002';
      },
      error: () => {
        this.customerMessage = creating
          ? '\u53d6\u5f15\u5148\u306e\u65b0\u898f\u767b\u9332\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002'
          : '\u53d6\u5f15\u5148\u306e\u8a02\u6b63\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002';
      }
    });
  }

  searchContracts(page = 0): void {
    this.contractMessage = '';
    let params = new HttpParams()
      .set('page', String(page))
      .set('size', String(this.contractSize));

    Object.entries(this.contractSearchForm).forEach(([key, value]) => {
      if (value.trim()) {
        params = params.set(key, value.trim());
      }
    });

    this.http.get<ContractPageResponse>(this.contractsApiUrl, { params }).subscribe({
      next: (response) => {
        this.contractResults = response.items;
        this.contractTotalElements = response.totalElements;
        this.contractPage = response.page;
        this.contractSize = response.size;
        this.contractTotalPages = response.totalPages;
        this.contractMessage = `${response.totalElements}\u4ef6\u898b\u3064\u304b\u308a\u307e\u3057\u305f\u3002`;
      },
      error: () => {
        this.contractMessage = '\u5951\u7d04\u4e00\u89a7\u306e\u691c\u7d22\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002';
      }
    });
  }

  clearContractSearch(): void {
    this.contractSearchForm = {
      engineerName: '',
      operationStatus: '',
      contractStartDateFrom: '',
      contractEndDateTo: '',
      effectiveMonthFrom: '',
      effectiveMonthTo: '',
      customerId: '',
      engineerCompanyId: '',
      projectAffiliation: '',
      engineerProposer: ''
    };
    this.contractResults = [];
    this.contractTotalElements = 0;
    this.contractPage = 0;
    this.contractTotalPages = 0;
    this.contractMessage = '';
  }

  openContractEdit(row: ContractListItem): void {
    this.contractMessage = '';
    this.contractModalMode = 'edit';
    this.editingContract = { ...row };
  }

  openContractCreate(): void {
    this.contractMessage = '';
    this.contractModalMode = 'create';
    this.editingContract = {
      contractNo: 0,
      engineerType: '',
      engineerName: '',
      contractAmount: null,
      customerName: '',
      engineerUnitPrice: null,
      engineerCompanyName: '',
      operationStatus: '',
      contractStartDate: null,
      contractEndDate: null,
      projectAffiliation: '',
      engineerProposer: '',
      settlementLowerHour: null,
      settlementUpperHour: null
    };
  }

  closeContractEdit(): void {
    this.editingContract = null;
    this.contractModalMode = null;
  }

  deleteContract(row: ContractListItem): void {
    if (!window.confirm(`契約番号 ${row.contractNo} を削除します。よろしいですか。`)) {
      return;
    }

    this.http.delete<void>(`${this.contractsApiUrl}/${row.contractNo}`).subscribe({
      next: () => {
        this.contractResults = this.contractResults.filter((item) => item.contractNo !== row.contractNo);
        this.contractTotalElements = Math.max(0, this.contractTotalElements - 1);
        this.contractMessage = '契約情報を削除しました。';
      },
      error: () => {
        this.contractMessage = '契約情報の削除に失敗しました。';
      }
    });
  }

  saveContractEdit(): void {
    if (!this.editingContract) {
      return;
    }
    const payload: ContractListItem = {
      ...this.editingContract,
      contractStartDate: this.editingContract.contractStartDate || null,
      contractEndDate: this.editingContract.contractEndDate || null,
      projectAffiliation: this.editingContract.projectAffiliation || '',
      engineerProposer: this.editingContract.engineerProposer || '',
      settlementLowerHour: this.editingContract.settlementLowerHour,
      settlementUpperHour: this.editingContract.settlementUpperHour
    };
    const mode = this.contractModalMode;
    const request = mode === 'create'
      ? this.http.post<ContractListItem>(this.contractsApiUrl, payload)
      : this.http.put<ContractListItem>(`${this.contractsApiUrl}/${this.editingContract.contractNo}`, payload);
    request.subscribe({
      next: (updated) => {
        if (mode === 'create') {
          this.contractResults = [updated, ...this.contractResults];
          this.contractTotalElements += 1;
        } else {
          this.contractResults = this.contractResults.map((row) =>
            row.contractNo === updated.contractNo ? updated : row
          );
        }
        this.editingContract = null;
        this.contractModalMode = null;
        this.contractMessage = mode === 'create' ? '契約情報を登録しました。' : '契約情報を訂正しました。';
      },
      error: () => {
        this.contractMessage = mode === 'create' ? '契約情報の登録に失敗しました。' : '契約情報の訂正に失敗しました。';
      }
    });
  }

  isContractCreateMode(): boolean {
    return this.contractModalMode === 'create';
  }

  contractStaffName(employeeNo: string): string {
    const employee = this.employeeMasterByNo.get(employeeNo);
    return employee?.employeeName ? `${employeeNo} ${employee.employeeName}` : employeeNo;
  }

  showContractOrderButton(row: ContractListItem): boolean {
    const engineerCompanyName = row.engineerCompanyName?.trim() ?? '';
    return engineerCompanyName !== ''
      && engineerCompanyName !== '0000'
      && engineerCompanyName !== '株式会社CMCソフト';
  }

  createOrderDocument(row: ContractListItem): void {
    this.openDocumentCreationMonth({ type: 'order', bulk: false, row });
  }

  createOrderDocumentsBulk(): void {
    this.openDocumentCreationMonth({ type: 'order', bulk: true });
  }

  createInvoiceDocument(row: ContractListItem): void {
    this.openDocumentCreationMonth({ type: 'invoice', bulk: false, row });
  }

  createInvoiceDocumentsBulk(): void {
    this.openDocumentCreationMonth({ type: 'invoice', bulk: true });
  }

  openDocumentCreationMonth(action: DocumentCreationAction): void {
    this.contractMessage = '';
    this.documentCreationAction = action;
    this.documentCreationMonth = this.currentMonthInput();
  }

  closeDocumentCreationMonth(): void {
    this.documentCreationAction = null;
  }

  confirmDocumentCreation(): void {
    if (!this.documentCreationAction || !this.documentCreationMonth) {
      return;
    }
    const action = this.documentCreationAction;
    const targetMonth = this.documentCreationMonth;
    this.documentCreationAction = null;
    if (action.type === 'order' && action.bulk) {
      this.executeOrderDocumentsBulk(targetMonth);
    } else if (action.type === 'order') {
      this.executeOrderDocument(action.row, targetMonth);
    } else if (action.bulk) {
      this.executeInvoiceDocumentsBulk(targetMonth);
    } else {
      this.executeInvoiceDocument(action.row, targetMonth);
    }
  }

  executeOrderDocument(row: ContractListItem, targetMonth: string): void {
    this.contractMessage = '';
    this.http.post<OrderDocumentResponse>(`${this.contractsApiUrl}/${row.contractNo}/order-document`, { targetMonth }).subscribe({
      next: (response) => {
        this.orderDocumentDialog = {
          title: '注文書作成完了',
          message: '注文書のExcelファイルを作成しました。',
          fileName: response.fileName,
          filePath: response.filePath
        };
      },
      error: () => {
        this.contractMessage = '注文書の出力に失敗しました。取引先番号と注文書テンプレートを確認してください。';
      }
    });
  }

  executeOrderDocumentsBulk(targetMonth: string): void {
    this.contractMessage = '';
    this.http.post<OrderDocumentBulkResponse>(`${this.contractsApiUrl}/order-documents/bulk`, { targetMonth }).subscribe({
      next: (response) => {
        this.orderDocumentDialog = {
          title: '注文書一括作成完了',
          message: `${response.createdCount}件の注文書を作成しました。`,
          fileName: response.fileNames.join('\n'),
          filePath: response.outputDirectory
        };
      },
      error: () => {
        this.contractMessage = '注文書の一括作成に失敗しました。対象契約と注文書テンプレートを確認してください。';
      }
    });
  }

  closeOrderDocumentDialog(): void {
    this.orderDocumentDialog = null;
  }

  executeInvoiceDocument(row: ContractListItem, targetMonth: string): void {
    this.contractMessage = '';
    this.http.post<InvoiceDocumentResponse>(`${this.contractsApiUrl}/${row.contractNo}/invoice-document`, { targetMonth }).subscribe({
      next: (response) => {
        this.orderDocumentDialog = {
          title: '請求書作成完了',
          message: `請求書を作成しました。明細${response.detailCount}件`,
          fileName: response.fileName,
          filePath: response.filePath
        };
      },
      error: () => {
        this.contractMessage = '請求書の出力に失敗しました。対象契約と請求書テンプレートを確認してください。';
      }
    });
  }

  executeInvoiceDocumentsBulk(targetMonth: string): void {
    this.contractMessage = '';
    this.http.post<InvoiceDocumentBulkResponse>(`${this.contractsApiUrl}/invoice-documents/bulk`, { targetMonth }).subscribe({
      next: (response) => {
        this.orderDocumentDialog = {
          title: '請求書一括作成完了',
          message: `${response.createdCount}件の請求書を作成しました。`,
          fileName: response.fileNames.join('\n'),
          filePath: response.outputDirectory
        };
      },
      error: () => {
        this.contractMessage = '請求書の一括作成に失敗しました。対象契約と請求書テンプレートを確認してください。';
      }
    });
  }

  goContractPage(page: number): void {
    if (page < 0 || (this.contractTotalPages > 0 && page >= this.contractTotalPages)) {
      return;
    }
    this.searchContracts(page);
  }

  contractPageLabel(): string {
    if (this.contractTotalPages === 0) {
      return '0 / 0';
    }
    return `${this.contractPage + 1} / ${this.contractTotalPages}`;
  }

  searchSales(): void {
    this.salesMessage = '';
    this.selectedSalesDetail = null;

    let params = new HttpParams();
    Object.entries(this.salesSearchForm).forEach(([key, value]) => {
      if (value.trim()) {
        params = params.set(key, value.trim());
      }
    });

    this.http.get<SalesSummaryItem[]>(this.salesApiUrl, { params }).subscribe({
      next: (results) => {
        this.salesResults = results;
        this.salesMessage = `${results.length}\u4ef6\u898b\u3064\u304b\u308a\u307e\u3057\u305f\u3002`;
      },
      error: () => {
        this.salesMessage = '\u58f2\u4e0a\u4e00\u89a7\u306e\u691c\u7d22\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002';
      }
    });
  }

  openSalesDetail(row: SalesSummaryItem): void {
    const params = new HttpParams()
      .set('billingMonth', row.billingMonth)
      .set('billingDestinationCompanyId', row.billingDestinationCompanyId);

    this.http.get<SalesDetailResponse>(`${this.salesApiUrl}/details`, { params }).subscribe({
      next: (detail) => {
        this.selectedSalesDetail = detail;
      },
      error: () => {
        this.salesMessage = '\u58f2\u4e0a\u660e\u7d30\u306e\u53d6\u5f97\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002';
      }
    });
  }

  closeSalesDetail(): void {
    this.selectedSalesDetail = null;
  }

  salesDetailTotal(detail: SalesDetailResponse): number {
    return detail.items.reduce((total, row) => total + this.amount(row.billingAmount), 0);
  }

  searchSalesAnalysis(): void {
    this.salesAnalysisMessage = '';
    if (this.salesAnalysisMode === 'customer') {
      this.searchSalesMonthlyAnalysis();
      return;
    }

    let params = new HttpParams();
    Object.entries(this.salesAnalysisSearchForm).forEach(([key, value]) => {
      if (key !== 'billingDestinationCompanyId' && value.trim()) {
        params = params.set(key, value.trim());
      }
    });

    this.http.get<SalesAnalysisItem[]>(`${this.salesApiUrl}/analysis`, { params }).subscribe({
      next: (results) => {
        this.salesAnalysisResults = results;
        this.salesMonthlyAnalysisResults = [];
        this.salesAnalysisMessage = `${results.length}\u793e\u898b\u3064\u304b\u308a\u307e\u3057\u305f\u3002`;
      },
      error: () => {
        this.salesAnalysisMessage = '\u58f2\u4e0a\u96c6\u8a08\u89e3\u6790\u306e\u691c\u7d22\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002';
      }
    });
  }

  searchSalesMonthlyAnalysis(): void {
    let params = new HttpParams();
    Object.entries(this.salesAnalysisSearchForm).forEach(([key, value]) => {
      if (value.trim()) {
        params = params.set(key, value.trim());
      }
    });

    this.http.get<SalesMonthlyAnalysisItem[]>(`${this.salesApiUrl}/analysis/monthly`, { params }).subscribe({
      next: (results) => {
        this.salesMonthlyAnalysisResults = results;
        this.salesAnalysisResults = [];
        this.salesAnalysisMessage = `${results.length}\u30f6\u6708\u898b\u3064\u304b\u308a\u307e\u3057\u305f\u3002`;
      },
      error: () => {
        this.salesAnalysisMessage = '\u58f2\u4e0a\u96c6\u8a08\u89e3\u6790\u306e\u691c\u7d22\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002';
      }
    });
  }

  changeSalesAnalysisMode(): void {
    this.salesAnalysisMessage = '';
    this.salesAnalysisResults = [];
    this.salesMonthlyAnalysisResults = [];
  }

  salesAnalysisTotalAmount(): number {
    return this.salesAnalysisResults.reduce((total, row) => total + this.amount(row.salesAmount), 0);
  }

  salesAnalysisTotalPersonCount(): number {
    return this.salesAnalysisResults.reduce((total, row) => total + this.amount(row.workingPersonCount), 0);
  }

  salesAnalysisPercent(row: SalesAnalysisItem): number {
    const total = this.salesAnalysisTotalAmount();
    if (total <= 0) {
      return 0;
    }
    return Math.round((this.amount(row.salesAmount) / total) * 1000) / 10;
  }

  salesAnalysisPieBackground(): string {
    const total = this.salesAnalysisTotalAmount();
    if (total <= 0) {
      return '#f4efe4';
    }

    const colors = this.salesAnalysisColors();
    let start = 0;
    const segments = this.salesAnalysisResults.map((row, index) => {
      const share = (this.amount(row.salesAmount) / total) * 100;
      const end = start + share;
      const segment = `${colors[index % colors.length]} ${start}% ${end}%`;
      start = end;
      return segment;
    });
    return `conic-gradient(${segments.join(', ')})`;
  }

  salesAnalysisColor(index: number): string {
    const colors = this.salesAnalysisColors();
    return colors[index % colors.length];
  }

  salesMonthlyTotalAmount(): number {
    return this.salesMonthlyAnalysisResults.reduce((total, row) => total + this.amount(row.salesAmount), 0);
  }

  salesMonthlyTotalPersonCount(): number {
    return this.salesMonthlyAnalysisResults.reduce((total, row) => total + this.amount(row.workingPersonCount), 0);
  }

  salesMonthlyMaxPersonCount(): number {
    return Math.max(1, ...this.salesMonthlyAnalysisResults.map((row) => this.amount(row.workingPersonCount)));
  }

  salesMonthlyMaxAmount(): number {
    return Math.max(1, ...this.salesMonthlyAnalysisResults.map((row) => this.amount(row.salesAmount)));
  }

  salesMonthlyBarHeight(row: SalesMonthlyAnalysisItem): string {
    return `${(this.amount(row.workingPersonCount) / this.salesMonthlyMaxPersonCount()) * 100}%`;
  }

  salesMonthlyLinePoints(): string {
    const rows = this.salesMonthlyAnalysisResults;
    if (rows.length === 0) {
      return '';
    }
    const maxAmount = this.salesMonthlyMaxAmount();
    const step = rows.length === 1 ? 0 : 100 / (rows.length - 1);
    return rows.map((row, index) => {
      const x = rows.length === 1 ? 50 : index * step;
      const y = 100 - (this.amount(row.salesAmount) / maxAmount) * 100;
      return `${x},${y}`;
    }).join(' ');
  }

  salesMonthLabel(month: string): string {
    const compact = this.normalizeMonth(month);
    if (compact.length < 6) {
      return month;
    }
    return `${Number(compact.slice(4, 6))}\u6708`;
  }

  searchProfitAnalysis(): void {
    this.profitAnalysisMessage = '';

    let params = new HttpParams();
    Object.entries(this.profitAnalysisSearchForm).forEach(([key, value]) => {
      if (value.trim()) {
        params = params.set(key, value.trim());
      }
    });

    this.http.get<ProfitAnalysisItem[]>(`${this.salesApiUrl}/profit-analysis`, { params }).subscribe({
      next: (results) => {
        this.profitAnalysisResults = results;
        this.profitAnalysisMessage = `${results.length}\u30f6\u6708\u898b\u3064\u304b\u308a\u307e\u3057\u305f\u3002`;
      },
      error: () => {
        this.profitAnalysisMessage = '\u5229\u76ca\u89e3\u6790\u306e\u691c\u7d22\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002';
      }
    });
  }

  loadFiscalProfitAnalysis(): void {
    this.fiscalProfitMessage = '';
    this.http.get<FiscalProfitAnalysisResponse>(`${this.salesApiUrl}/fiscal-profit`).subscribe({
      next: (response) => {
        this.fiscalProfitStartMonth = response.fiscalStartMonth;
        this.fiscalProfitEndMonth = response.fiscalEndMonth;
        this.fiscalProfitMonths = response.months;
        this.fiscalProfitRows = response.rows;
        this.fiscalProfitCompanyTotal = this.amount(response.companyProfitTotal);
        this.projectAffiliationProfitRows = response.projectAffiliationProfits ?? [];
        this.projectAffiliationProfitTotal = this.projectAffiliationProfitRows.reduce(
          (sum, row) => sum + this.amount(row.profitAmount),
          0
        );
        this.fiscalProfitMessage = `${response.months.length}か月の利益を集計しました。`;
      },
      error: () => {
        this.fiscalProfitStartMonth = '';
        this.fiscalProfitEndMonth = '';
        this.fiscalProfitMonths = [];
        this.fiscalProfitRows = [];
        this.fiscalProfitCompanyTotal = 0;
        this.projectAffiliationProfitRows = [];
        this.projectAffiliationProfitTotal = 0;
        this.fiscalProfitMessage = '決算期利益の取得に失敗しました。';
      }
    });
  }

  searchAllowanceAnalysis(): void {
    this.allowanceAnalysisMessage = '';
    this.http.get<AllowanceAnalysisResponse>(`${this.salesApiUrl}/allowance-analysis`).subscribe({
      next: (response) => {
        this.allowanceAnalysisMonths = response.months;
        this.allowanceAnalysisResults = response.rows;
        this.recruitmentAllowanceAmounts.clear();
        Object.entries(response.recruitmentAmounts ?? {}).forEach(([employeeNo, amounts]) => {
          Object.entries(amounts).forEach(([month, amount]) => {
            this.recruitmentAllowanceAmounts.set(`${employeeNo}\u0000${month}`, this.amount(amount));
          });
        });
        this.rebuildAllowanceDisplayRows();
        this.allowanceAnalysisMessage = `${response.rows.length}名の担当者を集計しました。`;
      },
      error: () => {
        this.allowanceAnalysisMessage = '手当管理の集計に失敗しました。';
      }
    });
  }

  saveAllowanceRecruitment(): void {
    this.allowanceAnalysisMessage = '';
    const rows = this.allowanceAnalysisResults.map((row) => ({
      employeeNo: row.employeeNo,
      amounts: this.allowanceAnalysisMonths.map((month) => ({
        paymentMonth: month,
        amount: this.recruitmentAllowanceAmounts.get(`${row.employeeNo}\u0000${month}`) ?? 0
      }))
    }));
    this.http.put(`${this.salesApiUrl}/allowance-analysis/recruitment`, { rows }).subscribe({
      next: () => {
        this.allowanceAnalysisMessage = '募集手当を登録しました。';
      },
      error: () => {
        this.allowanceAnalysisMessage = '募集手当の登録に失敗しました。';
      }
    });
  }

  rebuildAllowanceDisplayRows(): void {
    this.allowanceAnalysisDisplayResults = this.allowanceAnalysisResults.flatMap((row, rowIndex) => {
      const recruitmentMonthlyAmounts = row.monthlyAmounts.map((amount, index) => ({
        month: amount.month,
        amount: this.recruitmentAllowanceAmount(row.employeeNo, index)
      }));
      const salesTotal = row.monthlyAmounts.reduce((total, amount) => total + this.amount(amount.amount), 0);
      const recruitmentTotal = recruitmentMonthlyAmounts.reduce((total, amount) => total + this.amount(amount.amount), 0);
      const employeeTotal = salesTotal + recruitmentTotal;
      return [
        { ...row, isRecruitment: false, isEmployeeGroupStart: rowIndex > 0, employeeTotal },
        {
          ...row,
          allowanceName: '募集手当',
          monthlyAmounts: recruitmentMonthlyAmounts,
          subtotal: recruitmentTotal,
          total: employeeTotal,
          isRecruitment: true,
          isEmployeeGroupStart: false,
          employeeTotal
        }
      ];
    });
  }

  setRecruitmentAllowanceAmount(row: AllowanceDisplayRow, index: number, value: string | number): void {
    const parsed = typeof value === 'number' ? value : Number(String(value).replace(/,/g, '').trim());
    const amount = Number.isFinite(parsed) ? parsed : 0;
    const month = this.allowanceAnalysisMonths[index];
    if (!month) {
      return;
    }
    this.recruitmentAllowanceAmounts.set(`${row.employeeNo}\u0000${month}`, amount);
    row.monthlyAmounts[index].amount = amount;
    const total = row.monthlyAmounts.reduce((sum, item) => sum + this.amount(item.amount), 0);
    row.subtotal = total;
    const salesRow = this.allowanceAnalysisDisplayResults.find(
      (item) => item.employeeNo === row.employeeNo && !item.isRecruitment
    );
    const employeeTotal = this.amount(salesRow?.subtotal) + total;
    this.allowanceAnalysisDisplayResults
      .filter((item) => item.employeeNo === row.employeeNo)
      .forEach((item) => item.employeeTotal = employeeTotal);
    row.total = employeeTotal;
  }

  formatAllowanceAmount(value: number | null): string {
    return new Intl.NumberFormat('ja-JP', { maximumFractionDigits: 0 }).format(this.amount(value));
  }

  private recruitmentAllowanceAmounts = new Map<string, number>();

  private recruitmentAllowanceAmount(employeeNo: string, monthIndex: number): number {
    const month = this.allowanceAnalysisMonths[monthIndex];
    return month === undefined ? 0 : this.recruitmentAllowanceAmounts.get(`${employeeNo}\u0000${month}`) ?? 0;
  }

  openAllowanceAnalysisDetails(row: AllowanceAnalysisRow, index: number): void {
    const month = this.allowanceAnalysisMonths[index];
    if (!month) {
      return;
    }
    this.http.get<AllowanceAnalysisDetail[]>(`${this.salesApiUrl}/allowance-analysis/details`, {
      params: new HttpParams()
        .set('employeeNo', row.employeeNo)
        .set('month', month)
    }).subscribe({
      next: (details) => {
        this.allowanceAnalysisDetails = details;
        this.allowanceAnalysisDetailTitle = `${month.replace('-', '/')} ${row.employeeName || row.employeeNo} 営業手当明細`;
      },
      error: () => {
        this.allowanceAnalysisMessage = '手当計算明細の取得に失敗しました。';
      }
    });
  }

  closeAllowanceAnalysisDetails(): void {
    this.allowanceAnalysisDetails = [];
    this.allowanceAnalysisDetailTitle = '';
  }

  loadShareholderSalaries(): void {
    this.shareholderSalaryMessage = '';
    this.http.get<ShareholderSalaryResponse>(this.shareholderSalariesApiUrl, {
      params: new HttpParams().set('employeeNo', this.shareholderSalaryEmployeeNo)
    }).subscribe({
      next: (response) => {
        this.shareholderSalaryEmployeeNo = response.employeeNo;
        this.shareholderSalaryEmployeeName = response.employeeName;
        this.shareholderSalaryFiscalStartMonth = response.fiscalStartMonth;
        this.shareholderSalaryFiscalEndMonth = response.fiscalEndMonth;
        this.shareholderSalaryRows = response.rows;
        this.shareholderSalaryTotal = this.amount(response.totalOverUnder);
      },
      error: () => {
        this.shareholderSalaryRows = [];
        this.shareholderSalaryTotal = 0;
        this.shareholderSalaryMessage = '株主給与の取得に失敗しました。';
      }
    });
  }

  changeShareholderSalaryEmployee(): void {
    this.loadShareholderSalaries();
  }

  formatShareholderAmount(value: number | null): string {
    return new Intl.NumberFormat('ja-JP', { maximumFractionDigits: 2 }).format(this.amount(value));
  }

  setShareholderAmount(
    row: ShareholderSalaryRow,
    field: 'contractAmount' | 'overtimeAmount' | 'baseSalary' | 'advanceExpense2' | 'companyTotalAmount',
    value: string
  ): void {
    const normalized = value.replace(/,/g, '').trim();
    const parsed = normalized === '' || normalized === '-' ? 0 : Number(normalized);
    row[field] = Number.isFinite(parsed) ? parsed : 0;
    if (field === 'companyTotalAmount') {
      row.companyTotalManual = true;
    }
    this.recalculateShareholderSalaryRow(row);
  }

  recalculateShareholderSalaryRow(row: ShareholderSalaryRow): void {
    row.overUnder = this.amount(row.contractAmount)
      + this.amount(row.overtimeAmount)
      - this.amount(row.baseSalary)
      - this.amount(row.advanceExpense2)
      - this.amount(row.companyTotalAmount);
    this.shareholderSalaryTotal = this.shareholderSalaryRows
      .reduce((total, item) => total + this.amount(item.overUnder), 0);
  }

  saveShareholderSalaries(): void {
    this.shareholderSalaryMessage = '';
    this.http.put<ShareholderSalaryResponse>(this.shareholderSalariesApiUrl, {
      employeeNo: this.shareholderSalaryEmployeeNo,
      rows: this.shareholderSalaryRows.map((row) => ({
        paymentMonth: row.paymentMonth,
        contractAmount: this.amount(row.contractAmount),
        overtimeAmount: this.amount(row.overtimeAmount),
        baseSalary: this.amount(row.baseSalary),
        advanceExpense2: this.amount(row.advanceExpense2),
        companyTotalAmount: this.amount(row.companyTotalAmount),
        companyTotalManual: row.companyTotalManual
      }))
    }).subscribe({
      next: (response) => {
        this.shareholderSalaryEmployeeName = response.employeeName;
        this.shareholderSalaryRows = response.rows;
        this.shareholderSalaryTotal = this.amount(response.totalOverUnder);
        this.shareholderSalaryMessage = '株主給与を登録しました。';
      },
      error: () => {
        this.shareholderSalaryMessage = '株主給与の登録に失敗しました。';
      }
    });
  }

  profitAnalysisTotalAmount(): number {
    return this.profitAnalysisResults.reduce((total, row) => total + this.amount(row.profitAmount), 0);
  }

  profitAnalysisMaxPersonCount(): number {
    return Math.max(1, ...this.profitAnalysisResults.map((row) => this.amount(row.workingPersonCount)));
  }

  profitAnalysisMaxAmount(): number {
    return Math.max(1, ...this.profitAnalysisResults.map((row) => Math.abs(this.amount(row.profitAmount))));
  }

  profitAnalysisBarHeight(row: ProfitAnalysisItem): string {
    return `${(this.amount(row.workingPersonCount) / this.profitAnalysisMaxPersonCount()) * 100}%`;
  }

  profitAnalysisLinePoints(): string {
    const rows = this.profitAnalysisResults;
    if (rows.length === 0) {
      return '';
    }
    const maxAmount = this.profitAnalysisMaxAmount();
    const step = rows.length === 1 ? 0 : 100 / (rows.length - 1);
    return rows.map((row, index) => {
      const x = rows.length === 1 ? 50 : index * step;
      const y = 100 - (this.amount(row.profitAmount) / maxAmount) * 100;
      return `${x},${Math.max(0, Math.min(100, y))}`;
    }).join(' ');
  }

  isSalaryConditionCurrentMonth(): boolean {
    return this.normalizeMonth(this.salaryConditionPaymentMonth) === this.currentMonthCompact();
  }

  isSalaryConditionEditable(): boolean {
    const paymentMonth = this.normalizeMonth(this.salaryConditionPaymentMonth);
    return [this.currentMonthCompact(), this.nextMonthCompact()].includes(paymentMonth);
  }

  canCreateSalaryDetails(): boolean {
    return this.salaryConditionRows.length > 0 && !this.salaryConditionsDirty && !this.salaryDetailsRegistered;
  }

  markSalaryConditionsDirty(): void {
    this.salaryConditionsDirty = true;
    this.salaryConditionsSaved = false;
    this.salaryDetailsCreated = false;
  }

  isEmployeeTypeOne(row: { employeeNo?: string | null; employeeType?: string | null }): boolean {
    const employeeNo = String(row.employeeNo ?? '').trim();
    const employeeType = row.employeeType?.trim() || this.employeeTypeByNo.get(employeeNo)?.trim();
    return employeeType === '1';
  }

  isEmploymentInsuranceDisabled(row: { employeeNo?: string | null; employeeType?: string | null }): boolean {
    return this.isEmployeeTypeOne(row) || this.isEmploymentInsuranceExemptEmployee(row);
  }

  salaryConditionIdentityDisabled(row: SalaryConditionRow): boolean {
    return !this.isSalaryConditionEditable() || !row.editableIdentity;
  }

  loadEmployees(): void {
    this.employeeMessage = '';
    this.http.get<EmployeeMaster[]>(this.employeesApiUrl).subscribe({
      next: (employees) => {
        this.employees = employees;
        this.updateEmployeeMasterMap(employees);
        this.employeeMessage = `${employees.length}\u4ef6\u8868\u793a\u3057\u307e\u3057\u305f\u3002`;
      },
      error: () => {
        this.employeeMessage = '\u793e\u54e1\u4e00\u89a7\u306e\u53d6\u5f97\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002';
      }
    });
  }

  loadEmployeeTypes(): void {
    this.http.get<EmployeeMaster[]>(this.employeesApiUrl).subscribe({
      next: (employees) => {
        this.updateEmployeeMasterMap(employees);
      }
    });
  }

  openEmployeeEdit(employee: EmployeeMaster): void {
    this.employeeMessage = '';
    this.employeeCreateMode = false;
    const legacyNameParts = (employee.employeeName ?? '').replace(/　/g, ' ').trim().split(/\s+/);
    this.editingEmployee = {
      ...employee,
      lastName: employee.lastName || legacyNameParts[0] || '',
      firstName: employee.firstName || legacyNameParts.slice(1).join(' ') || ''
    };
  }

  openEmployeeCreate(): void {
    this.employeeMessage = '';
    this.employeeCreateMode = true;
    this.editingEmployee = {
      employeeNo: this.nextEmployeeNumberFromLoadedList(),
      employeeName: '',
      lastName: '',
      firstName: '',
      employeeType: '0',
      baseSalary: 0,
      dependentCount: 0,
      deductionCount: 0,
      consumptionTaxRate: 0,
      deletionFlag: '0'
    };
    this.http.get<string>(`${this.employeesApiUrl}/next-number`).subscribe({
      next: (employeeNo) => {
        if (this.employeeCreateMode && this.editingEmployee) {
          this.editingEmployee.employeeNo = employeeNo;
        }
      },
      error: () => {
        this.employeeMessage = '\u6b21\u306e\u793e\u54e1\u756a\u53f7\u306e\u53d6\u5f97\u306b\u5931\u6557\u3057\u305f\u305f\u3081\u3001\u4e00\u89a7\u306e\u793e\u54e1\u756a\u53f7\u304b\u3089\u4eee\u756a\u53f7\u3092\u8868\u793a\u3057\u307e\u3057\u305f\u3002';
      }
    });
  }

  employeeDisplayName(employee: EmployeeMaster): string {
    const splitName = `${employee.lastName ?? ''} ${employee.firstName ?? ''}`.trim();
    return splitName || employee.employeeName || '';
  }

  get regularEmployees(): EmployeeMaster[] {
    return this.employees.filter((employee) => employee.employeeType === '0');
  }

  get employeeListRows(): EmployeeMaster[] {
    return this.includeBpEmployees
      ? this.employees
      : this.employees.filter((employee) => employee.employeeType !== '2');
  }

  private nextEmployeeNumberFromLoadedList(): string {
    const maxEmployeeNo = this.employees
      .map((employee) => Number.parseInt(employee.employeeNo, 10))
      .filter((employeeNo) => Number.isFinite(employeeNo))
      .reduce((max, employeeNo) => Math.max(max, employeeNo), 0);
    return String(maxEmployeeNo + 1).padStart(4, '0');
  }

  closeEmployeeEdit(): void {
    this.editingEmployee = null;
    this.employeeCreateMode = false;
  }

  saveEmployeeEdit(): void {
    if (!this.editingEmployee) {
      return;
    }

    if (this.employeeCreateMode) {
      this.http.post<EmployeeMaster>(this.employeesApiUrl, this.editingEmployee).subscribe({
        next: (saved) => {
          this.employees = [...this.employees, saved].sort((left, right) =>
            left.employeeNo.localeCompare(right.employeeNo));
          this.updateEmployeeMasterMap(this.employees);
          this.editingEmployee = null;
          this.employeeCreateMode = false;
          this.employeeMessage = '\u793e\u54e1\u60c5\u5831\u3092\u65b0\u898f\u767b\u9332\u3057\u307e\u3057\u305f\u3002';
        },
        error: () => {
          this.employeeMessage = '\u793e\u54e1\u60c5\u5831\u306e\u65b0\u898f\u767b\u9332\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002';
        }
      });
      return;
    }

    this.http.put<EmployeeMaster>(
      `${this.employeesApiUrl}/${this.editingEmployee.employeeNo}`,
      this.editingEmployee
    ).subscribe({
      next: (saved) => {
        this.employees = this.employees.map((employee) =>
          employee.employeeNo === saved.employeeNo ? saved : employee
        );
        this.editingEmployee = null;
        this.employeeMessage = '\u793e\u54e1\u60c5\u5831\u3092\u8a02\u6b63\u3057\u307e\u3057\u305f\u3002';
      },
      error: () => {
        this.employeeMessage = '\u793e\u54e1\u60c5\u5831\u306e\u8a02\u6b63\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002';
      }
    });
  }

  loadSalaryConditions(): void {
    this.salaryConditionMessage = '';
    this.salaryDetailRows = [];
    this.salaryDetailsCreated = false;
    this.salaryDetailsRegistered = false;
    let params = new HttpParams();
    if (this.salaryConditionPaymentMonth.trim()) {
      params = params.set('paymentMonth', this.salaryConditionPaymentMonth.trim());
    }

    this.http.get<SalaryConditionResponse>(`${this.salaryCalculationApiUrl}/conditions`, { params }).subscribe({
      next: (response) => {
        this.applySalaryConditionResponse(response);
        if (!response.hasData) {
          const copy = window.confirm('\u5148\u6708\u304b\u3089\u6708\u7d66\u660e\u7d30\u8a08\u7b97\u6761\u4ef6\u3092\u30b3\u30d4\u30fc\u3057\u3066\u3082\u3044\u3044\u3067\u3059\u304b');
          if (copy) {
            this.copyPreviousSalaryConditions();
          } else {
            this.salaryConditionMessage = `${response.paymentMonth}\u306e\u8a08\u7b97\u6761\u4ef6\u306f\u307e\u3060\u3042\u308a\u307e\u305b\u3093\u3002`;
          }
        }
      },
      error: () => {
        this.salaryConditionMessage = '\u6708\u7d66\u660e\u7d30\u8a08\u7b97\u6761\u4ef6\u306e\u53d6\u5f97\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002';
      }
    });
  }

  copyPreviousSalaryConditions(): void {
    this.http.get<SalaryConditionResponse>(`${this.salaryCalculationApiUrl}/conditions/copy-previous`, {
      params: new HttpParams().set('paymentMonth', this.salaryConditionPaymentMonth)
    }).subscribe({
      next: (response) => {
        this.applySalaryConditionResponse(response);
        this.salaryConditionsSaved = false;
        this.salaryConditionsDirty = true;
        this.salaryConditionMessage = `${response.previousPaymentMonth}\u306e\u8a08\u7b97\u6761\u4ef6\u3092\u8868\u793a\u3057\u307e\u3057\u305f\u3002\u767b\u9332\u3059\u308b\u3068${response.paymentMonth}\u306e\u30c7\u30fc\u30bf\u306b\u306a\u308a\u307e\u3059\u3002`;
      },
      error: () => {
        this.salaryConditionMessage = '\u5148\u6708\u30c7\u30fc\u30bf\u306e\u30b3\u30d4\u30fc\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002';
      }
    });
  }

  addSalaryConditionRow(): void {
    this.salaryConditionRows = [
      ...this.salaryConditionRows,
      {
        employeeNo: '',
        employeeName: '',
        employeeType: '1',
        paymentMonth: this.salaryConditionPaymentMonth,
        baseSalary: 0,
        consumptionTaxRate: 0,
        commutingAllowance: 0,
        managementAllowance: 0,
        transportationAdjustment: 0,
        overtimePay: 0,
        advanceExpense: 0,
        advanceExpense2: 0,
        otherAmount: 0,
        otherItemName: '',
        editableIdentity: true
      }
    ];
  }

  saveSalaryConditions(): void {
    if (!window.confirm('\u305d\u306e\u307e\u307e\u3067\u767b\u9332\u3057\u3066\u3082\u3044\u3044\u3067\u3059\u304b')) {
      return;
    }

    const rows = this.salaryConditionRows
      .filter((row) => row.employeeNo.trim())
      .map((row) => ({
        employeeNo: row.employeeNo.trim(),
        employeeName: row.employeeName.trim(),
        employeeType: row.employeeType,
        paymentMonth: this.salaryConditionPaymentMonth,
        baseSalary: row.baseSalary,
        consumptionTaxRate: row.consumptionTaxRate,
        commutingAllowance: row.commutingAllowance,
        managementAllowance: row.managementAllowance,
        transportationAdjustment: row.transportationAdjustment,
        overtimePay: row.overtimePay,
        advanceExpense: row.advanceExpense,
        advanceExpense2: row.advanceExpense2,
        otherAmount: row.otherAmount,
        otherItemName: row.otherItemName
      }));

    this.http.put<SalaryConditionResponse>(`${this.salaryCalculationApiUrl}/conditions`, {
      paymentMonth: this.salaryConditionPaymentMonth,
      rows
    }).subscribe({
      next: (response) => {
        this.applySalaryConditionResponse(response);
        this.salaryConditionsSaved = true;
        this.salaryConditionsDirty = false;
        this.salaryConditionMessage = '\u6708\u7d66\u660e\u7d30\u8a08\u7b97\u6761\u4ef6\u3092\u767b\u9332\u3057\u307e\u3057\u305f\u3002';
      },
      error: () => {
        this.salaryConditionMessage = '\u6708\u7d66\u660e\u7d30\u8a08\u7b97\u6761\u4ef6\u306e\u767b\u9332\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002';
      }
    });
  }

  createSalaryDetails(): void {
    this.http.get<EmployeeMaster[]>(this.employeesApiUrl).subscribe({
      next: (employees) => {
        this.updateEmployeeMasterMap(employees);
        this.fetchSalaryDetailDraft();
      },
      error: () => {
        this.fetchSalaryDetailDraft();
      }
    });
  }

  fetchSalaryDetailDraft(): void {
    this.http.get<SalaryDetailDraftRow[]>(`${this.salaryCalculationApiUrl}/salary-details/draft`, {
      params: new HttpParams().set('paymentMonth', this.salaryConditionPaymentMonth)
    }).subscribe({
      next: (rows) => {
        this.salaryDetailRows = rows.map((row) => this.normalizeSalaryDetailRow(row));
        this.salaryDetailsCreated = true;
        this.salaryDetailsRegistered = false;
        this.activeView = 'salaryDetailPreview';
        this.salaryConditionMessage = '\u7d66\u6599\u660e\u7d30\u3092\u4f5c\u6210\u3057\u307e\u3057\u305f\u3002\u5fc5\u8981\u306b\u5fdc\u3058\u3066\u4fee\u6b63\u3057\u3066\u304b\u3089\u767b\u9332\u3057\u3066\u304f\u3060\u3055\u3044\u3002';
      },
      error: () => {
        this.salaryConditionMessage = '\u7d66\u6599\u660e\u7d30\u306e\u4f5c\u6210\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002';
      }
    });
  }

  backToSalaryConditions(): void {
    this.activeView = 'salaryConditions';
  }

  confirmAndSaveSalaryDetails(): void {
    if (!window.confirm('\u305d\u306e\u307e\u307e\u3067\u767b\u9332\u3057\u3066\u3082\u3044\u3044\u3067\u3059\u304b')) {
      return;
    }
    this.saveSalaryDetails();
  }

  saveSalaryDetails(): void {
    this.http.post<SalaryDetailDraftRow[]>(`${this.salaryCalculationApiUrl}/salary-details`, {
      paymentMonth: this.salaryConditionPaymentMonth,
      rows: this.salaryDetailRows.map((row) => this.normalizeSalaryDetailRow(row))
    }).subscribe({
      next: () => {
        this.salaryDetailsRegistered = true;
        this.salaryConditionMessage = '\u7d66\u6599\u660e\u7d30\u3092\u767b\u9332\u3057\u307e\u3057\u305f\u3002';
      },
      error: () => {
        this.salaryConditionMessage = '\u7d66\u6599\u660e\u7d30\u306e\u767b\u9332\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002';
      }
    });
  }

  openSalaryDetailEdit(row: SalaryDetailDraftRow, index: number): void {
    this.editingSalaryDetail = { ...row };
    this.editingSalaryDetailIndex = index;
  }

  closeSalaryDetailEdit(): void {
    this.editingSalaryDetail = null;
    this.editingSalaryDetailIndex = null;
  }

  applySalaryDetailEdit(): void {
    if (this.editingSalaryDetail === null || this.editingSalaryDetailIndex === null) {
      return;
    }
    const updated = { ...this.editingSalaryDetail };
    if (!updated.employeeType?.trim()) {
      updated.employeeType = this.employeeTypeByNo.get(updated.employeeNo.trim()) ?? '';
    }
    if (this.isEmploymentInsuranceExemptEmployee(updated)) {
      updated.employmentInsuranceAmount = 0;
    }
    this.salaryDetailRows = this.salaryDetailRows.map((row, index) =>
      index === this.editingSalaryDetailIndex ? updated : row
    );
    this.closeSalaryDetailEdit();
  }

  normalizeSalaryDetailRow(row: SalaryDetailDraftRow): SalaryDetailDraftRow {
    const normalized = { ...row };
    if (!normalized.employeeType?.trim()) {
      normalized.employeeType = this.employeeTypeByNo.get(normalized.employeeNo.trim()) ?? '';
    }
    this.recalculateSalaryDetail(normalized);
    return normalized;
  }

  recalculateSalaryDetail(row: SalaryDetailDraftRow): void {
    if (this.isEmployeeTypeOne(row)) {
      const condition = this.findSalaryCondition(row.employeeNo);
      const employee = this.employeeMasterByNo.get(row.employeeNo.trim());
      const baseSalary = this.amount(row.baseSalary ?? employee?.baseSalary ?? condition?.baseSalary);
      const overtimePay = this.amount(condition?.overtimePay);
      const consumptionTaxRate = this.amount(employee?.consumptionTaxRate ?? condition?.consumptionTaxRate);
      const remuneration = Math.round((baseSalary + overtimePay) * (1 + consumptionTaxRate));
      const supportTotal = Math.round(
        remuneration
        + this.amount(condition?.transportationAdjustment)
        + this.amount(condition?.commutingAllowance)
      );
      row.remunerationAmount = remuneration;
      row.supportTotalAmount = supportTotal;
      row.employmentInsuranceAmount = 0;
      row.pensionInsuranceAmount = 0;
      row.healthInsuranceAmount = 0;
      row.residentTaxAmount = 0;
      row.childSupportAmount = 0;
      row.taxableIncome = 0;
      row.incomeTax = 0;
      row.deductionTotalAmount = 0;
      row.transferPaymentAmount = supportTotal + this.amount(row.otherAmount);
      return;
    }

    if (this.isEmploymentInsuranceExemptEmployee(row)) {
      row.employmentInsuranceAmount = 0;
    }
    const condition = this.findSalaryCondition(row.employeeNo);
    row.taxableIncome = this.amount(row.remunerationAmount)
      - this.amount(condition?.commutingAllowance)
      - this.amount(row.employmentInsuranceAmount)
      - this.amount(row.pensionInsuranceAmount)
      - this.amount(row.healthInsuranceAmount);
    const deductionTotal = this.amount(row.employmentInsuranceAmount)
      + this.amount(row.pensionInsuranceAmount)
      + this.amount(row.healthInsuranceAmount)
      + this.amount(row.residentTaxAmount)
      + this.amount(row.childSupportAmount)
      + this.amount(row.incomeTax);
    row.deductionTotalAmount = Math.trunc(deductionTotal);
    row.transferPaymentAmount = this.amount(row.supportTotalAmount) - row.deductionTotalAmount + this.amount(row.otherAmount);
  }

  searchPayrollSlips(): void {
    this.payrollMessage = '';
    this.selectedPayrollSlip = null;

    let params = new HttpParams();
    Object.entries(this.payrollSearchForm).forEach(([key, value]) => {
      if (value.trim()) {
        params = params.set(key, value.trim());
      }
    });

    this.http.get<PayrollSlipListItem[]>(this.payrollApiUrl, { params }).subscribe({
      next: (results) => {
        this.payrollResults = results;
        this.payrollMessage = `${results.length}\u4ef6\u898b\u3064\u304b\u308a\u307e\u3057\u305f\u3002`;
      },
      error: () => {
        this.payrollMessage = '\u793e\u54e1\u7d66\u4e0e\u660e\u7d30\u306e\u691c\u7d22\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002';
      }
    });
  }

  clearPayrollSearch(): void {
    this.payrollSearchForm = {
      paymentMonth: '',
      employeeNo: '',
      employeeName: ''
    };
    this.payrollResults = [];
    this.payrollMessage = '';
  }

  openPayrollDetail(row: PayrollSlipListItem): void {
    this.payrollMessage = '';

    this.http.get<PayrollSlipDetail>(`${this.payrollApiUrl}/${row.employeeNo}/${row.paymentMonth}`).subscribe({
      next: (detail) => {
        this.selectedPayrollSlip = detail;
        this.activeView = 'payrollDetail';
      },
      error: () => {
        this.payrollMessage = '\u500b\u5225\u793e\u54e1\u660e\u7d30\u306e\u53d6\u5f97\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002';
      }
    });
  }

  backToPayrollSearch(): void {
    this.activeView = 'payrollSearch';
    this.selectedPayrollSlip = null;
  }

  printPayrollSlip(detail: PayrollSlipDetail): void {
    this.http.post<PayrollPdfResponse>(
      `${this.payrollApiUrl}/${detail.employeeNo}/${detail.paymentMonth}/pdf`,
      {}
    ).subscribe({
      next: (response) => {
        window.alert(`PDFを出力しました。\n${response.filePath}`);
      },
      error: () => {
        window.alert('\u7d66\u4e0e\u660e\u7d30PDF\u306e\u51fa\u529b\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002');
      }
    });
  }

  printPayrollSlipsBulk(): void {
    if (this.payrollResults.length === 0 || this.payrollBulkPrinting) {
      return;
    }
    if (!window.confirm(`${this.payrollResults.length}件の給与明細PDFを出力します。よろしいですか。`)) {
      return;
    }

    this.payrollBulkPrinting = true;
    this.payrollMessage = '給与明細PDFを一括出力しています。';
    const requests = this.payrollResults.map((row) =>
      this.http.post<PayrollPdfResponse>(
        `${this.payrollApiUrl}/${row.employeeNo}/${row.paymentMonth}/pdf`,
        {}
      ).pipe(
        map((response) => ({ row, response, success: true })),
        catchError(() => of({ row, response: null, success: false }))
      )
    );

    forkJoin(requests).subscribe({
      next: (results) => {
        this.payrollBulkPrinting = false;
        const successCount = results.filter((result) => result.success).length;
        const failedRows = results
          .filter((result) => !result.success)
          .map((result) => `${result.row.employeeNo}/${result.row.paymentMonth}`);
        this.payrollMessage = failedRows.length === 0
          ? `${successCount}件の給与明細PDFを出力しました。`
          : `${successCount}件の給与明細PDFを出力しました。失敗: ${failedRows.join(', ')}`;
        window.alert(this.payrollMessage);
      },
      error: () => {
        this.payrollBulkPrinting = false;
        this.payrollMessage = '給与明細PDFの一括出力に失敗しました。';
        window.alert(this.payrollMessage);
      }
    });
  }

  register(): void {
    this.registerErrors = this.validate(this.registerForm, true);
    this.registerMessage = '';

    if (this.registerErrors.length > 0) {
      return;
    }

    this.http.post<BillingRequest>(this.billingApiUrl, this.normalizePayload(this.registerForm)).subscribe({
      next: (saved) => {
        this.registerMessage = `\u767b\u9332\u3057\u307e\u3057\u305f\u3002No: ${saved.no}`;
        this.registerForm = this.createEmptyForm();
      },
      error: () => {
        this.registerMessage = '\u767b\u9332\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002\u5165\u529b\u5185\u5bb9\u3068\u30d0\u30c3\u30af\u30a8\u30f3\u30c9\u3092\u78ba\u8a8d\u3057\u3066\u304f\u3060\u3055\u3044\u3002';
      }
    });
  }

  search(): void {
    this.searchMessage = '';
    const params = this.buildSearchParams();

    this.http.get<BillingRequest[]>(this.billingApiUrl, { params }).subscribe({
      next: (results) => {
        this.searchResults = results;
        this.searchMessage = `${results.length}\u4ef6\u898b\u3064\u304b\u308a\u307e\u3057\u305f\u3002`;
      },
      error: () => {
        this.searchMessage = '\u691c\u7d22\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002\u30d0\u30c3\u30af\u30a8\u30f3\u30c9\u3092\u78ba\u8a8d\u3057\u3066\u304f\u3060\u3055\u3044\u3002';
      }
    });
  }

  clearSearch(): void {
    this.searchForm = this.createEmptyForm();
    this.searchResults = [];
    this.searchMessage = '';
  }

  formatRate(rate: number | null): string {
    if (rate === null || rate === undefined) {
      return '';
    }
    return `${(rate * 100).toFixed(2)}%`;
  }

  pensionGradeLabel(row: SocialInsurancePremiumAmount): string {
    return row.pensionGrade ? `${row.grade}(${row.pensionGrade})` : `${row.grade}`;
  }

  remunerationRange(row: SocialInsurancePremiumAmount): string {
    const from = row.monthlyRemunerationFrom;
    const to = row.monthlyRemunerationTo;
    if (from === null && to === null) {
      return '';
    }
    if (from === null) {
      return `\uff5e ${to?.toLocaleString()}`;
    }
    if (to === null) {
      return `${from.toLocaleString()} \uff5e`;
    }
    return `${from.toLocaleString()} \uff5e ${to.toLocaleString()}`;
  }

  combinedTotal(row: SocialInsurancePremiumAmount): number | null {
    return this.addNullable(row.healthInsuranceTotalAmount, row.careInsuranceTotalAmount);
  }

  combinedEmployee(row: SocialInsurancePremiumAmount): number | null {
    return this.addNullable(row.healthInsuranceEmployeeAmount, row.careInsuranceEmployeeAmount);
  }

  amount(value: number | null | undefined): number {
    return value ?? 0;
  }

  private isEmploymentInsuranceExemptEmployee(row: { employeeNo?: string | null }): boolean {
    return row.employeeNo?.trim() === '0010';
  }

  payrollSupportTotal(detail: PayrollSlipDetail): number {
    return this.amount(detail.baseSalary)
      + this.amount(detail.commutingAllowance)
      + this.amount(detail.managementAllowance)
      + this.amount(detail.transportationAdjustment)
      + this.amount(detail.overtimePay)
      + this.amount(detail.advanceExpense)
      + this.amount(detail.advanceExpense2);
  }

  payrollDeductionTotal(detail: PayrollSlipDetail): number {
    return Math.trunc(this.amount(detail.employmentInsuranceAmount)
      + this.amount(detail.pensionInsuranceAmount)
      + this.amount(detail.healthInsuranceAmount)
      + this.amount(detail.incomeTax)
      + this.amount(detail.residentTaxAmount)
      + this.amount(detail.childSupportAmount));
  }

  otherItemLabel(detail: PayrollSlipDetail): string {
    return detail.otherItemName?.trim() || '\u305d\u306e\u4ed6\u91d1\u984d';
  }

  payrollSlipTitle(detail: PayrollSlipDetail): string {
    return `${this.toJapaneseEraMonth(detail.paymentMonth)}\u660e\u7d30\u66f8`;
  }

  payrollTransferDate(detail: PayrollSlipDetail): string {
    const compact = detail.paymentMonth.replace(/[^0-9]/g, '');
    if (compact.length < 6) {
      return '';
    }
    return `${compact.slice(0, 4)}/${compact.slice(4, 6)}/25`;
  }

  payrollPdfFileName(detail: PayrollSlipDetail): string {
    const normalizedName = detail.employeeName.replace(/\s/g, '');
    return `${this.toJapaneseEraMonth(detail.paymentMonth)}\u7d66\u4e0e\u660e\u7d30\u66f8_${detail.employeeNo}_${normalizedName}`;
  }

  private loadEmploymentRates(): void {
    this.http.get<EmploymentInsuranceRate[]>(`${this.insuranceApiUrl}/employment-rates`).subscribe({
      next: (rates) => {
        this.employmentRates = rates;
      },
      error: () => {
        this.masterMessage = '\u96c7\u7528\u4fdd\u967a\u6599\u7387\u306e\u53d6\u5f97\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002';
      }
    });
  }

  private loadSocialPremiums(): void {
    this.http.get<SocialInsurancePremiumAmount[]>(`${this.insuranceApiUrl}/social-premiums`).subscribe({
      next: (premiums) => {
        this.socialPremiums = premiums;
      },
      error: () => {
        this.masterMessage = '\u4fdd\u967a\u6599\u984d\u8868\u306e\u53d6\u5f97\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002';
      }
    });
  }

  private currentMonthInput(): string {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  }

  private currentDateInput(): string {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
  }

  private createMonthOptions(): string[] {
    const base = new Date();
    base.setDate(1);
    return Array.from({ length: 25 }, (_, index) => {
      const date = new Date(base.getFullYear(), base.getMonth() + index - 12, 1);
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
    });
  }

  documentCreationMonthLabel(value: string): string {
    const compact = value.replace(/[^0-9]/g, '');
    return compact.length === 6
      ? `${compact.slice(0, 4)}年${compact.slice(4, 6)}月`
      : value;
  }

  private currentMonthCompact(): string {
    return this.currentMonthInput().replace('-', '');
  }

  private nextMonthCompact(): string {
    const now = new Date();
    const next = new Date(now.getFullYear(), now.getMonth() + 1, 1);
    return `${next.getFullYear()}${String(next.getMonth() + 1).padStart(2, '0')}`;
  }

  private normalizeMonth(value: string): string {
    return value.replace(/[^0-9]/g, '').slice(0, 6);
  }

  private salesAnalysisColors(): string[] {
    return [
      '#4472c4',
      '#ed7d31',
      '#a5a5a5',
      '#ffc000',
      '#5b9bd5',
      '#70ad47',
      '#264478',
      '#9e480e',
      '#636363',
      '#997300',
      '#255e91',
      '#43682b'
    ];
  }

  private createEmptyForm(): BillingRequest {
    return {
      requestSource: '',
      requestDestination: '',
      requestMonth: '',
      engineer: '',
      amount: null,
      remarks: ''
    };
  }

  private validate(form: BillingRequest, requireAll: boolean): string[] {
    const errors: string[] = [];

    if (requireAll && !form.requestSource.trim()) {
      errors.push('\u8acb\u6c42\u5143\u3092\u5165\u529b\u3057\u3066\u304f\u3060\u3055\u3044\u3002');
    }
    if (requireAll && !form.requestDestination.trim()) {
      errors.push('\u8acb\u6c42\u5148\u3092\u5165\u529b\u3057\u3066\u304f\u3060\u3055\u3044\u3002');
    }
    if (requireAll && !form.requestMonth) {
      errors.push('\u8acb\u6c42\u5e74\u6708\u3092\u9078\u629e\u3057\u3066\u304f\u3060\u3055\u3044\u3002');
    }
    if (requireAll && !form.engineer.trim()) {
      errors.push('\u8acb\u6c42\u6280\u8853\u8005\u3092\u5165\u529b\u3057\u3066\u304f\u3060\u3055\u3044\u3002');
    }
    if (form.amount === null || form.amount === undefined) {
      if (requireAll) {
        errors.push('\u8acb\u6c42\u91d1\u984d\u3092\u5165\u529b\u3057\u3066\u304f\u3060\u3055\u3044\u3002');
      }
    } else if (form.amount <= 0) {
      errors.push('\u8acb\u6c42\u91d1\u984d\u306f1\u4ee5\u4e0a\u3067\u5165\u529b\u3057\u3066\u304f\u3060\u3055\u3044\u3002');
    }

    return errors;
  }

  private normalizePayload(form: BillingRequest): BillingRequest {
    return {
      requestSource: form.requestSource.trim(),
      requestDestination: form.requestDestination.trim(),
      requestMonth: form.requestMonth,
      engineer: form.engineer.trim(),
      amount: form.amount,
      remarks: form.remarks.trim()
    };
  }

  private buildSearchParams(): HttpParams {
    let params = new HttpParams();
    const values = this.normalizePayload(this.searchForm);

    Object.entries(values).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    });

    return params;
  }

  private addNullable(first: number | null, second: number | null): number | null {
    if (first === null || first === undefined) {
      return null;
    }
    return first + (second ?? 0);
  }

  private toJapaneseEraMonth(paymentMonth: string): string {
    const compact = paymentMonth.replace(/[^0-9]/g, '');
    const year = Number(compact.slice(0, 4));
    const month = Number(compact.slice(4, 6));
    if (!year || !month) {
      return paymentMonth;
    }
    const reiwaYear = year - 2018;
    return `\u4ee4\u548c${String(reiwaYear).padStart(2, '0')}\u5e74${String(month).padStart(2, '0')}\u6708`;
  }

  private applySalaryConditionResponse(response: SalaryConditionResponse): void {
    this.salaryConditionPaymentMonth = response.paymentMonth;
    this.salaryConditionRows = response.rows.map((row) => ({ ...row, editableIdentity: false }));
    this.updateEmployeeTypeMap(this.salaryConditionRows);
    this.salaryConditionsSaved = response.hasData;
    this.salaryConditionsDirty = false;
  }

  private updateEmployeeTypeMap(rows: Array<{ employeeNo: string; employeeType: string }>): void {
    rows.forEach((row) => {
      if (row.employeeNo?.trim()) {
        this.employeeTypeByNo.set(row.employeeNo.trim(), row.employeeType?.trim() ?? '');
      }
    });
  }

  private updateEmployeeMasterMap(employees: EmployeeMaster[]): void {
    employees.forEach((employee) => {
      if (employee.employeeNo?.trim()) {
        this.employeeMasterByNo.set(employee.employeeNo.trim(), employee);
        this.employeeTypeByNo.set(employee.employeeNo.trim(), employee.employeeType?.trim() ?? '');
      }
    });
  }

  private findSalaryCondition(employeeNo: string): SalaryConditionRow | undefined {
    return this.salaryConditionRows.find((row) => row.employeeNo.trim() === employeeNo.trim());
  }
}
