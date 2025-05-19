package dao.Mail;

import dao.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CountChecker {
    public boolean checkAndUpdateCount() {
        String selectSql = "SELECT COUNT(*) AS remaining FROM Schedule1 WHERE numberOfOccurrences > 0";
        String updateSql = "UPDATE Schedule1 SET numberOfOccurrences = numberOfOccurrences - 1 WHERE numberOfOccurrences > 0";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql);
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next() && rs.getInt("remaining") > 0) {
                    updateStmt.executeUpdate();
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}