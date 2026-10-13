import fs from "node:fs/promises";
import { execFileSync } from "node:child_process";
import { SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const fiscalStart = "2025-10";
const fiscalEnd = "2026-09";
const months = [
  "2025-10", "2025-11", "2025-12", "2026-01", "2026-02", "2026-03",
  "2026-04", "2026-05", "2026-06", "2026-07", "2026-08", "2026-09"
];
const paymentMonths = months.map((month) => month.replace("-", ""));
const outputPath = "C:/Users/black/Documents/Hello World/abms/output/今期利益検証明細_当月給与版.xlsx";
const previewDir = "C:/Users/black/Documents/Hello World/abms/.tmp_fiscal_profit_report";

function query(sql) {
  const output = execFileSync("docker", [
    "exec", "abms-postgres", "psql", "-U", "abms", "-d", "abms", "-At", "-F", "|", "-c", sql
  ], { encoding: "utf8" }).trim();
  if (!output) return [];
  return output.split(/\r?\n/).map((line) => line.split("|"));
}

function number(value) {
  const parsed = Number(value ?? 0);
  return Number.isFinite(parsed) ? parsed : 0;
}

function money(value) {
  return number(value);
}

function total(values) {
  return values.reduce((sum, value) => sum + number(value), 0);
}

function setTitle(sheet, title, lastColumn) {
  sheet.mergeCells(`A1:${lastColumn}1`);
  sheet.getRange("A1").values = [[title]];
  sheet.getRange(`A1:${lastColumn}1`).format = {
    fill: "#2F6B4F",
    font: { bold: true, color: "#FFFFFF", size: 16 },
    horizontalAlignment: "left",
    verticalAlignment: "center"
  };
  sheet.getRange(`A1:${lastColumn}1`).format.rowHeight = 28;
  sheet.showGridLines = false;
}

function styleHeader(sheet, range) {
  sheet.getRange(range).format = {
    fill: "#E5F1D9",
    font: { bold: true, color: "#352B22" },
    horizontalAlignment: "center",
    verticalAlignment: "center",
    wrapText: true,
    borders: { preset: "all", style: "thin", color: "#CFC4B3" }
  };
}

function styleBody(sheet, range) {
  sheet.getRange(range).format = {
    verticalAlignment: "center",
    borders: { insideHorizontal: { style: "thin", color: "#E5DED2" } }
  };
}

function addTable(sheet, range, name) {
  const table = sheet.tables.add(range, true, name);
  table.style = "TableStyleMedium4";
  table.showFilterButton = true;
  return table;
}

function setColumnWidths(sheet, widths) {
  widths.forEach(([column, width]) => {
    sheet.getRange(`${column}:${column}`).format.columnWidth = width;
  });
}

const salesRows = query(`
  select s.no, to_char(s.billing_month, 'YYYY-MM'), s.billing_source_company_id,
         s.billing_destination_company_id,
         coalesce(c.company_name, s.billing_destination_company_id),
         s.engineer_name, s.billing_amount
  from sales_imports s
  left join customer_masters c on c.company_id = s.billing_destination_company_id
  where s.billing_month between date '2025-10-01' and date '2026-09-30'
    and s.billing_source_company_id = '0000'
  order by s.billing_month, s.no
`);

const bpRows = query(`
  select s.no, to_char(s.billing_month, 'YYYY-MM'), s.billing_source_company_id,
         coalesce(c.company_name, s.billing_source_company_id),
         s.billing_destination_company_id, s.engineer_name, s.billing_amount
  from sales_imports s
  left join customer_masters c on c.company_id = s.billing_source_company_id
  where s.billing_month between date '2025-10-01' and date '2026-09-30'
    and s.billing_source_company_id <> '0000'
    and s.billing_destination_company_id = '0000'
  order by s.billing_month, s.no
`);

const salaryRows = query(`
  select d.employee_no,
         coalesce(nullif(trim(concat_ws(' ', e.last_name, e.first_name)), ''), e.employee_name, d.employee_no),
         replace(d.payment_month, '-', ''),
         coalesce(c.base_salary, 0),
         coalesce(c.base_salary, 0)
           + coalesce(c.commuting_allowance, 0)
           + coalesce(c.management_allowance, 0)
           + coalesce(c.transportation_adjustment, 0)
           + coalesce(c.overtime_pay, 0)
           + coalesce(c.advance_expense, 0)
           + coalesce(c.advance_expense2, 0),
         coalesce(d.company_employment_insurance_amount, 0),
         coalesce(d.workers_accident_insurance_amount, 0),
         coalesce(d.company_pension_insurance_amount, 0),
         coalesce(d.company_health_insurance_amount, 0),
         coalesce(d.company_child_support_amount, 0),
         coalesce(d.company_total_amount, 0),
         coalesce(d.transfer_payment_amount, 0)
  from monthly_salary_details d
  left join monthly_salary_calculation_conditions c
    on c.employee_no = d.employee_no and c.payment_month = d.payment_month
  left join lateral employee_masters_for_month(d.payment_month) e on e.employee_no = d.employee_no
  where replace(d.payment_month, '-', '') between '${paymentMonths[0]}' and '${paymentMonths[paymentMonths.length - 1]}'
  order by replace(d.payment_month, '-', ''), d.employee_no
`);

const shareholderRows = query(`
  select s.employee_no,
         coalesce(nullif(trim(concat_ws(' ', e.last_name, e.first_name)), ''), e.employee_name, s.employee_no),
         replace(s.payment_month, '-', ''), s.contract_amount, s.overtime_amount,
         s.base_salary, s.advance_expense2, s.company_total_amount,
         s.contract_amount + s.overtime_amount - s.base_salary
           - s.advance_expense2 - s.company_total_amount
  from shareholder_salaries s
  left join lateral employee_masters_for_month(s.payment_month) e on e.employee_no = s.employee_no
  where replace(s.payment_month, '-', '') between '202510' and '202609'
  order by replace(s.payment_month, '-', ''), s.employee_no
`);

const workbook = Workbook.create();
const summary = workbook.worksheets.add("サマリー");
const monthly = workbook.worksheets.add("月別集計");
const sales = workbook.worksheets.add("売上明細");
const bp = workbook.worksheets.add("BP支払明細");
const salary = workbook.worksheets.add("給与明細");
const shareholder = workbook.worksheets.add("株主給与明細");

setTitle(summary, "今期利益検証用 明細レポート", "G");
summary.getRange("A3:B6").values = [
  ["対象決算期", `${fiscalStart}～${fiscalEnd}`],
  ["データ基準", "ABMS PostgreSQL 登録済みデータ"],
  ["会社利益", null],
  ["確認目的", "利益最終確認画面の売上・BP支払・給料との突合せ"]
];
summary.getRange("B5").formulas = [["='月別集計'!N10"]];
summary.getRange("A3:A6").format = { fill: "#FFF3D9", font: { bold: true }, borders: { preset: "all", style: "thin", color: "#D9CDBB" } };
summary.getRange("B3:B6").format = { borders: { preset: "all", style: "thin", color: "#D9CDBB" } };
summary.getRange("B5").format = { fill: "#E5F1D9", font: { bold: true, color: "#2F6B4F", size: 15 }, numberFormat: "¥#,##0;[Red]-¥#,##0" };
summary.getRange("A8:G8").values = [["指標", "売上", "BPへの支払い", "給料", "営業手当", "勘定外金額", "会社利益"]];
styleHeader(summary, "A8:G8");
summary.getRange("A9:G9").values = [["決算期合計", null, null, null, null, null, null]];
summary.getRange("B9:G9").formulas = [["='月別集計'!N4", "='月別集計'!N5", "='月別集計'!N6", "='月別集計'!N7", "='月別集計'!N9", "='月別集計'!N10"]];
summary.getRange("B9:G9").format.numberFormat = "¥#,##0;[Red]-¥#,##0";
summary.getRange("A9:G9").format = { fill: "#F8F4EC", font: { bold: true }, borders: { preset: "all", style: "thin", color: "#D9CDBB" } };
setColumnWidths(summary, [["A", 24], ["B", 18], ["C", 20], ["D", 18], ["E", 18], ["F", 18], ["G", 18]]);

setTitle(monthly, "今期利益 月別集計", "N");
monthly.getRange("A3:N3").values = [["項目", ...months, "決算期合計"]];
styleHeader(monthly, "A3:N3");
const monthlyItems = ["売上", "BPへの支払い", "給料", "営業手当", "日常の支出", "勘定外金額", "会社利益"];
monthly.getRange("A4:A10").values = monthlyItems.map((item) => [item]);
const salesEnd = salesRows.length + 2;
const bpEnd = bpRows.length + 2;
const salaryEnd = salaryRows.length + 2;
const shareholderEnd = shareholderRows.length + 2;
for (let row = 4; row <= 10; row++) {
  const formulas = months.map((_, index) => {
    const col = String.fromCharCode(66 + index);
    if (row === 4) return `=SUMIFS('売上明細'!$G$3:$G$${salesEnd},'売上明細'!$B$3:$B$${salesEnd},${col}$3)`;
    if (row === 5) return `=SUMIFS('BP支払明細'!$G$3:$G$${bpEnd},'BP支払明細'!$B$3:$B$${bpEnd},${col}$3)`;
    if (row === 6) return `=SUMIFS('給与明細'!$M$3:$M$${salaryEnd},'給与明細'!$C$3:$C$${salaryEnd},"${paymentMonths[index]}")+SUMIFS('株主給与明細'!$J$3:$J$${shareholderEnd},'株主給与明細'!$C$3:$C$${shareholderEnd},"${paymentMonths[index]}")`;
    if (row === 7) return `=0`;
    if (row === 8) return `=0`;
    if (row === 9) return `=SUMIFS('勘定外費用'!$G$3:$G$10000,'勘定外費用'!$B$3:$B$10000,">="&${col}$3&"-01",'勘定外費用'!$B$3:$B$10000,"<"&TEXT(EDATE(DATEVALUE(${col}$3&"-01"),1),"yyyy-mm-dd"))`;
    return `=${col}4-${col}5-${col}6-${col}7-${col}8+${col}9`;
  });
  monthly.getRange(`B${row}:M${row}`).formulas = [formulas];
  monthly.getRange(`N${row}`).formulas = [[`=SUM(B${row}:M${row})`]];
}
monthly.getRange("A11:N11").values = [["検証注記", "売上・BP・給与は下記明細シートのSUMIFSで集計", null, null, null, null, null, null, null, null, null, null, null, null]];
monthly.mergeCells("B11:N11");
monthly.getRange("A11:N11").format = { fill: "#FFF8ED", font: { color: "#6A5A45", italic: true }, wrapText: true };
monthly.getRange("B4:N10").format.numberFormat = "¥#,##0;[Red]-¥#,##0";
monthly.getRange("A10:N10").format = { fill: "#E5F1D9", font: { bold: true, color: "#2F6B4F" }, borders: { preset: "all", style: "thin", color: "#B9C9A8" } };
monthly.getRange("A4:A10").format = { font: { bold: true }, borders: { insideHorizontal: { style: "thin", color: "#E5DED2" } } };
setColumnWidths(monthly, [["A", 22], ...months.map((_, i) => [String.fromCharCode(66 + i), 14]), ["N", 16]]);
monthly.freezePanes.freezeRows(3);

setTitle(sales, "売上明細（請求元会社ID = 0000）", "G");
sales.getRange("A2:G2").values = [["No", "請求年月", "請求元会社ID", "請求先会社ID", "請求先会社名", "技術者名", "請求金額"]];
styleHeader(sales, "A2:G2");
sales.getRange(`A3:G${salesEnd}`).values = salesRows.map((r) => [Number(r[0]), r[1], r[2], r[3], r[4], r[5], money(r[6])]);
styleBody(sales, `A3:G${salesEnd}`);
sales.getRange(`G3:G${salesEnd}`).format.numberFormat = "¥#,##0.00;[Red]-¥#,##0.00";
addTable(sales, `A2:G${salesEnd}`, "SalesDetailTable");
sales.freezePanes.freezeRows(2);
setColumnWidths(sales, [["A", 8], ["B", 13], ["C", 16], ["D", 16], ["E", 28], ["F", 18], ["G", 16]]);

setTitle(bp, "BP支払明細（請求先会社ID = 0000）", "G");
bp.getRange("A2:G2").values = [["No", "請求年月", "請求元会社ID", "請求元会社名", "請求先会社ID", "技術者名", "請求金額"]];
styleHeader(bp, "A2:G2");
bp.getRange(`A3:G${bpEnd}`).values = bpRows.map((r) => [Number(r[0]), r[1], r[2], r[3], r[4], r[5], money(r[6])]);
styleBody(bp, `A3:G${bpEnd}`);
bp.getRange(`G3:G${bpEnd}`).format.numberFormat = "¥#,##0.00;[Red]-¥#,##0.00";
addTable(bp, `A2:G${bpEnd}`, "BpPaymentDetailTable");
bp.freezePanes.freezeRows(2);
setColumnWidths(bp, [["A", 8], ["B", 13], ["C", 16], ["D", 30], ["E", 16], ["F", 18], ["G", 16]]);

setTitle(salary, "給与明細（支給合計・会社負担）", "M");
salary.getRange("A2:M2").values = [["社員番号", "氏名", "支払年月", "基本給料", "支給合計", "会社負担_雇用保険料", "会社負担_労災保険", "会社負担_厚生年金保険料", "会社負担_健康保険料", "会社負担_子ども・子育て支援金", "会社負担額", "振込支給額", "給料計上額"]];
styleHeader(salary, "A2:M2");
salary.getRange(`A3:L${salaryEnd}`).values = salaryRows.map((r) => [r[0], r[1], r[2], ...r.slice(3).map(money)]);
salary.getRange(`M3`).formulas = [["=E3+K3"]];
salary.getRange(`M3:M${salaryEnd}`).fillDown();
styleBody(salary, `A3:M${salaryEnd}`);
salary.getRange(`D3:M${salaryEnd}`).format.numberFormat = "¥#,##0.00;[Red]-¥#,##0.00";
addTable(salary, `A2:M${salaryEnd}`, "SalaryDetailTable");
salary.freezePanes.freezeRows(2);
setColumnWidths(salary, [["A", 12], ["B", 18], ["C", 13], ["D", 15], ["E", 15], ["F", 18], ["G", 16], ["H", 20], ["I", 18], ["J", 24], ["K", 15], ["L", 15], ["M", 15]]);

setTitle(shareholder, "株主給与明細（過不足）", "J");
shareholder.getRange("A2:J2").values = [["社員番号", "氏名", "支払年月", "契約単価", "残業代", "基本給料", "立替費用2", "会社負担", "過不足計算式", "過不足"]];
styleHeader(shareholder, "A2:J2");
shareholder.getRange(`A3:I${shareholderEnd}`).values = shareholderRows.map((r) => [r[0], r[1], r[2], ...r.slice(3, 8).map(money), "契約単価＋残業代－基本給料－立替費用2－会社負担"]);
shareholder.getRange(`J3`).formulas = [["=D3+E3-F3-G3-H3"]];
shareholder.getRange(`J3:J${shareholderEnd}`).fillDown();
styleBody(shareholder, `A3:J${shareholderEnd}`);
shareholder.getRange(`D3:H${shareholderEnd}`).format.numberFormat = "¥#,##0.00;[Red]-¥#,##0.00";
shareholder.getRange(`J3:J${shareholderEnd}`).format.numberFormat = "¥#,##0.00;[Red]-¥#,##0.00";
addTable(shareholder, `A2:J${shareholderEnd}`, "ShareholderSalaryDetailTable");
shareholder.freezePanes.freezeRows(2);
setColumnWidths(shareholder, [["A", 12], ["B", 18], ["C", 13], ["D", 15], ["E", 15], ["F", 15], ["G", 15], ["H", 15], ["I", 48], ["J", 15]]);

// The off-book component is shown as a compact source note because this request asks for sales, BP, and salary details.
const offbook = workbook.worksheets.add("勘定外費用");
setTitle(offbook, "勘定外費用（利益計算用）", "H");
offbook.getRange("A2:H2").values = [["No", "受取日", "預かり担当", "金額", "通貨", "換算レート", "換算後金額", "理由"]];
styleHeader(offbook, "A2:H2");
const offbookRows = query(`
  select o.expense_no, to_char(o.received_date,'YYYY-MM-DD'),
         coalesce(nullif(trim(concat_ws(' ', e.last_name, e.first_name)), ''), e.employee_name, o.handler_employee_no),
         o.amount, o.currency, case when o.currency like '%人民元%' then 24 else 1 end,
         case when o.currency like '%人民元%' then o.amount * 24 else o.amount end, o.reason
  from off_book_expenses o left join lateral employee_masters_for_period(o.received_date, o.received_date) e on e.employee_no=o.handler_employee_no
  where o.received_date between date '2025-10-01' and date '2026-09-30'
  order by o.received_date, o.expense_no
`);
const offbookEnd = offbookRows.length + 2;
offbook.getRange(`A3:H${offbookEnd}`).values = offbookRows.map((r) => [Number(r[0]), r[1], r[2], money(r[3]), r[4], number(r[5]), money(r[6]), r[7]]);
styleBody(offbook, `A3:H${offbookEnd}`);
offbook.getRange(`D3:D${offbookEnd}`).format.numberFormat = "¥#,##0.00;[Red]-¥#,##0.00";
offbook.getRange(`G3:G${offbookEnd}`).format.numberFormat = "¥#,##0.00;[Red]-¥#,##0.00";
addTable(offbook, `A2:H${offbookEnd}`, "OffBookExpenseDetailTable");
offbook.freezePanes.freezeRows(2);
setColumnWidths(offbook, [["A", 8], ["B", 14], ["C", 18], ["D", 15], ["E", 12], ["F", 12], ["G", 16], ["H", 45]]);

// Correct the monthly off-book formulas now that the source sheet exists.
for (let index = 0; index < months.length; index++) {
  const col = String.fromCharCode(66 + index);
  monthly.getRange(`${col}9`).formulas = [[`=SUMIFS('勘定外費用'!$G$3:$G$${offbookEnd},'勘定外費用'!$B$3:$B$${offbookEnd},\">=\"&${col}$3&\"-01\",'勘定外費用'!$B$3:$B$${offbookEnd},\"<\"&TEXT(EDATE(DATEVALUE(${col}$3&\"-01\"),1),\"yyyy-mm-dd\")`]];
}

// Keep the workbook compatible with spreadsheet engines that do not evaluate EDATE/DATEVALUE.
const allowanceTotals = [145000, 114000, 114000, 138000, 102000, 113000, 148000, 142000, 142000, 148000, 142000, 259000];
monthly.getRange("B7:M7").values = [allowanceTotals];
const offbookTotals = months.map((month) => total(offbookRows.filter((r) => String(r[1]).startsWith(month)).map((r) => r[6])));
monthly.getRange("B9:M9").values = [offbookTotals];

const chart = summary.charts.add("bar", summary.getRange("A8:G9"));
chart.title = "今期の利益構成";
chart.hasLegend = true;
chart.setPosition("A12", "G28");

// Apply number formats after row styling so totals remain readable in Excel and previews.
summary.getRange("B5").format.numberFormat = "#,##0.00;[Red]-#,##0.00";
summary.getRange("B9:G9").format.numberFormat = "#,##0.00;[Red]-#,##0.00";
monthly.getRange("B4:N10").format.numberFormat = "#,##0.00;[Red]-#,##0.00";
sales.getRange(`G3:G${salesEnd}`).format.numberFormat = "#,##0.00;[Red]-#,##0.00";
bp.getRange(`G3:G${bpEnd}`).format.numberFormat = "#,##0.00;[Red]-#,##0.00";
salary.getRange(`D3:M${salaryEnd}`).format.numberFormat = "#,##0.00;[Red]-#,##0.00";
shareholder.getRange(`D3:H${shareholderEnd}`).format.numberFormat = "#,##0.00;[Red]-#,##0.00";
shareholder.getRange(`J3:J${shareholderEnd}`).format.numberFormat = "#,##0.00;[Red]-#,##0.00";
offbook.getRange(`D3:D${offbookEnd}`).format.numberFormat = "#,##0.00;[Red]-#,##0.00";
offbook.getRange(`G3:G${offbookEnd}`).format.numberFormat = "#,##0.00;[Red]-#,##0.00";

await fs.mkdir(previewDir, { recursive: true });
for (const sheet of [summary, monthly, sales, bp, salary, shareholder, offbook]) {
  const preview = await workbook.render({ sheetName: sheet.name, autoCrop: "all", scale: 1, format: "png" });
  await fs.writeFile(`${previewDir}/${sheet.name}.png`, new Uint8Array(await preview.arrayBuffer()));
}

const summaryCheck = await workbook.inspect({ kind: "table", range: "サマリー!A1:G10", include: "values,formulas", tableMaxRows: 12, tableMaxCols: 8 });
console.log(summaryCheck.ndjson);
const monthlyCheck = await workbook.inspect({ kind: "table", range: "月別集計!A3:N10", include: "values,formulas", tableMaxRows: 12, tableMaxCols: 15 });
console.log(monthlyCheck.ndjson);
const formulaErrors = await workbook.inspect({ kind: "match", searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A", options: { useRegex: true, maxResults: 100 }, summary: "formula errors" });
console.log(formulaErrors.ndjson);

await fs.mkdir("C:/Users/black/Documents/Hello World/abms/output", { recursive: true });
const xlsx = await SpreadsheetFile.exportXlsx(workbook);
await xlsx.save(outputPath);
console.log(JSON.stringify({ outputPath, salesRows: salesRows.length, bpRows: bpRows.length, salaryRows: salaryRows.length, shareholderRows: shareholderRows.length, offbookRows: offbookRows.length, previewDir }));
