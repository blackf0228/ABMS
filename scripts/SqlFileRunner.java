import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SqlFileRunner {

  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      throw new IllegalArgumentException("Usage: SqlFileRunner <jdbcUrl> <user> <password> <sqlFile>");
    }

    String jdbcUrl = args[0];
    String user = args[1];
    String password = args[2];
    Path sqlFile = Path.of(args[3]);

    Properties props = new Properties();
    props.setProperty("user", user);
    props.setProperty("password", password);

    String sql = Files.readString(sqlFile, StandardCharsets.UTF_8);
    try (Connection connection = DriverManager.getConnection(jdbcUrl, props)) {
      connection.setAutoCommit(false);
      try (Statement statement = connection.createStatement()) {
        for (String command : splitSql(sql)) {
          if (!command.isBlank()) {
            boolean hasResultSet = statement.execute(command);
            if (hasResultSet) {
              printResultSet(statement.getResultSet());
            }
          }
        }
      }
      connection.commit();
    }
  }

  private static void printResultSet(ResultSet rs) throws Exception {
    ResultSetMetaData metaData = rs.getMetaData();
    int columnCount = metaData.getColumnCount();
    while (rs.next()) {
      List<String> values = new ArrayList<>();
      for (int i = 1; i <= columnCount; i++) {
        values.add(metaData.getColumnLabel(i) + "=" + rs.getString(i));
      }
      System.out.println(String.join(", ", values));
    }
  }

  private static List<String> splitSql(String sql) {
    List<String> commands = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inString = false;

    for (int i = 0; i < sql.length(); i++) {
      char c = sql.charAt(i);
      current.append(c);
      if (c == '\'') {
        if (inString && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
          current.append(sql.charAt(i + 1));
          i++;
        } else {
          inString = !inString;
        }
      } else if (c == ';' && !inString) {
        commands.add(current.toString());
        current.setLength(0);
      }
    }

    if (!current.isEmpty()) {
      commands.add(current.toString());
    }
    return commands;
  }
}
