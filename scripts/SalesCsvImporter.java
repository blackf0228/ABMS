import java.io.BufferedReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SalesCsvImporter {
  private static final Pattern JAPANESE_DATE = Pattern.compile("^(\\d{4})年(\\d{1,2})月(\\d{1,2})日$");

  public static void main(String[] args) throws Exception {
    if (args.length != 5) {
      throw new IllegalArgumentException("Usage: SalesCsvImporter <root> <jdbcUrl> <user> <password> <ddlSql>");
    }

    Path root = Path.of(args[0]);
    String jdbcUrl = args[1];
    String user = args[2];
    String password = args[3];
    Path ddlSql = Path.of(args[4]);

    Path outputDir = root.resolve("output");
    Path customerCsv = outputDir.resolve("customer_master.csv");
    Path salesCsv = outputDir.resolve("sales_import.csv");
    Path contractsCsv = outputDir.resolve("contracts.csv");

    Properties props = new Properties();
    props.setProperty("user", user);
    props.setProperty("password", password);

    try (Connection connection = DriverManager.getConnection(jdbcUrl, props)) {
      connection.setAutoCommit(false);
      executeDdl(connection, ddlSql);
      truncateTables(connection);
      importCustomers(connection, customerCsv);
      importSales(connection, salesCsv);
      importContracts(connection, contractsCsv);
      connection.commit();

      System.out.println();
      System.out.println("Import result:");
      printCount(connection, "customer_masters");
      printCount(connection, "sales_imports");
      printCount(connection, "contracts");
    } catch (Exception e) {
      throw e;
    }
  }

  private static void executeDdl(Connection connection, Path ddlSql) throws Exception {
    String sql = Files.readString(ddlSql, StandardCharsets.UTF_8);
    for (String statementText : sql.split(";")) {
      String trimmed = statementText.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      try (Statement statement = connection.createStatement()) {
        statement.execute(trimmed);
      }
    }
  }

  private static void truncateTables(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate("TRUNCATE TABLE sales_imports");
      statement.executeUpdate("TRUNCATE TABLE contracts");
      statement.executeUpdate("TRUNCATE TABLE customer_masters");
    }
  }

  private static void importCustomers(Connection connection, Path csvPath) throws Exception {
    String sql = """
        INSERT INTO customer_masters (
          company_id,
          company_name
        ) VALUES (?, ?)
        """;
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      for (List<String> row : readCsvRows(csvPath)) {
        ps.setString(1, value(row, 0));
        ps.setString(2, value(row, 1));
        ps.addBatch();
      }
      ps.executeBatch();
    }
  }

  private static void importSales(Connection connection, Path csvPath) throws Exception {
    String sql = """
        INSERT INTO sales_imports (
          no,
          billing_source_company_id,
          billing_destination_company_id,
          billing_month,
          engineer_name,
          billing_amount
        ) VALUES (?, ?, ?, ?, ?, ?)
        """;
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      for (List<String> row : readCsvRows(csvPath)) {
        ps.setInt(1, Integer.parseInt(value(row, 0)));
        ps.setString(2, value(row, 1));
        ps.setString(3, value(row, 2));
        ps.setDate(4, Date.valueOf(parseDate(value(row, 3))));
        ps.setString(5, value(row, 4));
        ps.setBigDecimal(6, parseAmount(value(row, 5)));
        ps.addBatch();
      }
      ps.executeBatch();
    }
  }

  private static void importContracts(Connection connection, Path csvPath) throws Exception {
    String sql = """
        INSERT INTO contracts (
          contract_no,
          engineer_type,
          engineer_name,
          contract_amount,
          customer_name,
          contract_start_date,
          contract_end_date,
          engineer_unit_price,
          engineer_company_name,
          operation_status
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      for (List<String> row : readCsvRows(csvPath)) {
        ps.setInt(1, Integer.parseInt(value(row, 0)));
        ps.setString(2, nullIfBlank(value(row, 1)));
        ps.setString(3, nullIfBlank(value(row, 2)));
        ps.setBigDecimal(4, parseAmount(value(row, 3)));
        ps.setString(5, nullIfBlank(value(row, 4)));
        setDate(ps, 6, value(row, 5));
        setDate(ps, 7, value(row, 6));
        ps.setBigDecimal(8, parseAmount(value(row, 7)));
        ps.setString(9, nullIfBlank(value(row, 8)));
        ps.setString(10, nullIfBlank(value(row, 9)));
        ps.addBatch();
      }
      ps.executeBatch();
    }
  }

  private static void setDate(PreparedStatement ps, int parameterIndex, String text) throws SQLException {
    LocalDate date = parseDate(text);
    if (date == null) {
      ps.setDate(parameterIndex, null);
    } else {
      ps.setDate(parameterIndex, Date.valueOf(date));
    }
  }

  private static LocalDate parseDate(String text) {
    String value = nullIfBlank(text);
    if (value == null) {
      return null;
    }
    Matcher matcher = JAPANESE_DATE.matcher(value);
    if (matcher.matches()) {
      return LocalDate.of(
          Integer.parseInt(matcher.group(1)),
          Integer.parseInt(matcher.group(2)),
          Integer.parseInt(matcher.group(3)));
    }
    if (value.matches("^\\d+(\\.0)?$")) {
      long excelSerialDate = new BigDecimal(value).longValue();
      return LocalDate.of(1899, 12, 30).plusDays(excelSerialDate);
    }
    return LocalDate.parse(value);
  }

  private static BigDecimal parseAmount(String text) {
    String value = nullIfBlank(text);
    if (value == null) {
      return null;
    }
    String numeric = value.replaceAll("[^0-9.\\-]", "");
    if (numeric.isBlank()) {
      return null;
    }
    return new BigDecimal(numeric);
  }

  private static List<List<String>> readCsvRows(Path csvPath) throws Exception {
    List<List<String>> rows = new ArrayList<>();
    try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
      String line;
      boolean header = true;
      while ((line = reader.readLine()) != null) {
        List<String> parsed = parseCsvLine(line);
        if (!parsed.isEmpty() && parsed.get(0).startsWith("\uFEFF")) {
          parsed.set(0, parsed.get(0).substring(1));
        }
        if (header) {
          header = false;
          continue;
        }
        if (parsed.stream().allMatch(String::isBlank)) {
          continue;
        }
        rows.add(parsed);
      }
    }
    return rows;
  }

  private static List<String> parseCsvLine(String line) {
    List<String> result = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean quoted = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '"') {
        if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          current.append('"');
          i++;
        } else {
          quoted = !quoted;
        }
      } else if (c == ',' && !quoted) {
        result.add(current.toString());
        current.setLength(0);
      } else {
        current.append(c);
      }
    }
    result.add(current.toString());
    return result;
  }

  private static String value(List<String> row, int index) {
    if (index >= row.size()) {
      return "";
    }
    return row.get(index).trim();
  }

  private static String nullIfBlank(String text) {
    if (text == null || text.isBlank()) {
      return null;
    }
    return text.trim();
  }

  private static void printCount(Connection connection, String tableName) throws SQLException {
    try (Statement statement = connection.createStatement();
         ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
      rs.next();
      System.out.printf("%-20s %d%n", tableName, rs.getLong(1));
    }
  }
}
