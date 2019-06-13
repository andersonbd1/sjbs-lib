/*
 * Copyright (c) 2019, Neustar, Inc.
 * UNPUBLISHED PROPRIETARY SOURCE CODE
 *
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class InsertOldIssues {

  public static void main(String[] args) {
    boolean dryRun = true;
    //int limit = 100;
    int limit = 0;
    int totalInserts = 0;
    log("beluga");
    try {
      // The newInstance() call is a work around for some
      // broken Java implementations

      //Class.forName("com.mysql.cj.jdbc.Driver").newInstance();

      Connection conn =
          DriverManager.getConnection("jdbc:mysql://localhost/koha_sjbs2?serverTimezone=UTC" +
              "&user=root&password=root");

      ResultSet rs = conn.createStatement().executeQuery("select 1 from dual");
      rs.next();
      log(rs.getLong(1) + "");

      BufferedReader br = new BufferedReader(new FileReader("sql_dumps/insert_old_issues.sql"));
      String line = br.readLine();
      int count = 1;
      while (line != null) {
        log(line);
        if (line.startsWith("INSERT")) {
        } else {
          if (count++ > limit && limit > 0) {
            break;
          }
          line = line.replaceAll("\\(", "");
          line = line.replaceAll("\\).", "");
          String[] cols = line.split(",");
          int j = 0;
          // old schema
          // $ mysql -D koha_sjbs -u root -e "desc old_issues;"
          String borrowernumber = cols[j++]; //| int(11)     | YES  | MUL | NULL              |                             |
          String itemnumber = cols[j++]; //| int(11)     | YES  | MUL | NULL              |                             |
          String date_due = cols[j++]; //| datetime    | YES  |     | NULL              |                             |
          String branchcode = cols[j++]; //| varchar(10) | YES  | MUL | NULL              |                             |
          String issuingbranch = cols[j++]; //| varchar(18) | YES  | MUL | NULL              |                             |
          String returndate = cols[j++]; //| datetime    | YES  |     | NULL              |                             |
          String lastreneweddate = cols[j++]; //| datetime    | YES  |     | NULL              |                             |
          String returny = cols[j++]; // | varchar(4)  | YES  |     | NULL              |                             |
          String renewals = cols[j++]; //| tinyint(4)  | YES  |     | NULL              |                             |
          String auto_renew = cols[j++]; //| tinyint(1)  | YES  |     | 0                 |                             |
          String timestamp = cols[j++]; //| timestamp   | NO   |     | CURRENT_TIMESTAMP | on update CURRENT_TIMESTAMP |
          String issuedate = cols[j++]; //| datetime    | YES  |     | NULL              |                             |
          String onsite_checkout = cols[j++]; //| int(1)      | NO   |     | 0                 |
          //(1,2,'2010-07-06 00:00:00','1',NULL,'2010-07-28 00:00:00',NULL,NULL,NULL,0,'2010-07-29 00:02:22','2010-07-05 00:00:00',0),

          Statement stmt = conn.createStatement();

          rs = stmt.executeQuery("select 1 from old_issues "
              + "where borrowernumber = " + borrowernumber + " and itemnumber = " + itemnumber);
          if (true //!rs.next()
              && borrowernumber != null && !"NULL".equals(borrowernumber)
              && itemnumber != null && !"NULL".equals(itemnumber) ) {
            if (dryRun) {
              totalInserts++;
              log("borrowernumber: " + borrowernumber);
              log("itemnumber: " + itemnumber);
              log("");
            } else {
              // new schema
              // $ mysql -D koha_sjbs2 -u root -e "desc old_issues;"
              String insertCols =
                  "borrowernumber, "
                      //   | int(11)     | NO   | MUL | NULL              |                             |\n"
                      + "itemnumber, "
                      //  | int(11)     | NO   | UNI | NULL              |                             |\n"
                      + "date_due  , "
                      //  | datetime    | YES  |     | NULL              |                             |\n"
                      + "branchcode , "
                      //  | varchar(10) | YES  | MUL | NULL              |                             |\n"
                      + "returndate , "
                      //  | datetime    | YES  |     | NULL              |                             |\n"
                      + "lastreneweddate, "
                      //  | datetime    | YES  |     | NULL              |                             |\n"
                      + "renewals , "
                      //  | tinyint(4)  | YES  |     | NULL              |                             |\n"
                      + "auto_renew  , "
                      //  | tinyint(1)  | YES  |     | 0                 |                             |\n"
                      //+ "auto_renew_error, "  //  | varchar(32) | YES  |     | NULL              |                             |\n"
                      + "timestamp     , "
                      //  | timestamp   | NO   |     | CURRENT_TIMESTAMP | on update CURRENT_TIMESTAMP |\n"
                      + "issuedate      , "
                      //  | datetime    | YES  |     | NULL              |                             |\n"
                      + "onsite_checkout , "
                      //  | int(1)      | NO   |     | 0                 |                             |\n"
                      + "note             "     //  | longtext    | YES  |     | NULL              |                             |\n"
                  //+ "notedate       , "   //  | datetime    | YES  |     | NULL              |                             |\n"
                  //+ "noteseen         "   //  | int(1)      | YES  |     | NULL              |                             |"
                  ;
              String insertIntoIssues = "insert into issues (" + insertCols + ") values ("
                  + borrowernumber + ", "
                  + itemnumber + ", "
                  + date_due + ", "
                  + branchcode + ", "
                  + returndate + ", "
                  + lastreneweddate + ", "
                  + renewals + ", "
                  + auto_renew + ", "
                  + timestamp + ", "
                  + issuedate + ", "
                  + onsite_checkout + ", "
                  + "'manual insert by Ben'"
                  + ")";
              // https://dev.mysql.com/doc/connector-j/5.1/en/connector-j-usagenotes-last-insert-id.html
              stmt.executeUpdate(insertIntoIssues, Statement.RETURN_GENERATED_KEYS);
              int autoIncKeyFromApi = -1;
              rs = stmt.getGeneratedKeys();
              if (rs.next()) {
                autoIncKeyFromApi = rs.getInt(1);
                log("new id: " + autoIncKeyFromApi);
              } else {
                // throw an exception from here
              }
              if (autoIncKeyFromApi != -1) {
                stmt.executeUpdate("insert into old_issues select * from issues where issue_id = "
                    + autoIncKeyFromApi);
                stmt.executeUpdate("delete from issues where issue_id = " + autoIncKeyFromApi);
              }
            /*
            String sql = "insert into old_issues (borrowernumber, itemnumber) values (354, 680)";
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.executeUpdate();
            ResultSet rs2 = ps.getGeneratedKeys();

            if (rs2.next()) {
              log(rs2.getLong(1) + "");
            }
            */
            }
          }
        }
        line = br.readLine();
      }
      br.close();
      log("totalInserts: " + totalInserts);

    } catch (Exception ex) {
      ex.printStackTrace(System.err);
    }
  }

  protected static void log(String m) {
    System.out.println(m);
  }
}
