package com.dataforge.service;

import com.dataforge.exception.InvalidInputException;
import com.dataforge.exception.ResourceNotFoundException;
import com.dataforge.model.DatabaseInstance;
import com.dataforge.repository.DatabaseInstanceRepository;
import com.dataforge.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DynamicCrudService {

    @Autowired
    private DatabaseInstanceRepository instanceRepository;
    @Autowired
    private EncryptionUtil encryptionUtil;

    public void createRecord(Long dbId, String tableName, Map<String, Object> record) {
        DatabaseInstance instance = findInstance(dbId);
        String url = buildJdbcUrl(instance);
        String decryptedPassword = encryptionUtil.decrypt(instance.getDbPassword());

        String columns = String.join(", ", record.keySet());
        String placeholders = record.keySet().stream().map(k -> "?").collect(Collectors.joining(", "));
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, placeholders);

        try (Connection conn = DriverManager.getConnection(url, instance.getDbUser(), decryptedPassword);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int i = 1;
            for (Object value : record.values()) {
                pstmt.setObject(i++, value);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new InvalidInputException("Failed to create record in table '" + tableName + "': " + e.getMessage());
        }
    }

    public List<Map<String, Object>> readRecords(Long dbId, String tableName, int page, int limit, Map<String, String> filters, String orderByColumn, String orderDirection) {
        DatabaseInstance instance = findInstance(dbId);
        String url = buildJdbcUrl(instance);
        String decryptedPassword = encryptionUtil.decrypt(instance.getDbPassword());
        List<Map<String, Object>> records = new ArrayList<>();

        StringBuilder sqlBuilder = new StringBuilder(String.format("SELECT * FROM %s", tableName));
        List<Object> params = new ArrayList<>();

        if (filters != null && !filters.isEmpty()) {
            sqlBuilder.append(" WHERE ");
            String whereClause = filters.entrySet().stream()
                    .map(entry -> {
                        params.add(entry.getValue());
                        return String.format("%s = ?", entry.getKey());
                    })
                    .collect(Collectors.joining(" AND "));
            sqlBuilder.append(whereClause);
        }

        if (orderByColumn != null && !orderByColumn.isBlank()) {
            sqlBuilder.append(" ORDER BY ").append(orderByColumn);
            if (orderDirection != null && (orderDirection.equalsIgnoreCase("ASC") || orderDirection.equalsIgnoreCase("DESC"))) {
                sqlBuilder.append(" ").append(orderDirection.toUpperCase());
            } else {
                sqlBuilder.append(" ASC");
            }
        }

        sqlBuilder.append(String.format(" LIMIT %d OFFSET %d", limit, (page - 1) * limit));
        String sql = sqlBuilder.toString();

        try (Connection conn = DriverManager.getConnection(url, instance.getDbUser(), decryptedPassword);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> record = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        record.put(metaData.getColumnName(i), rs.getObject(i));
                    }
                    records.add(record);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read records from table '" + tableName + "': " + e.getMessage());
        }
        return records;
    }

    public int updateRecord(Long dbId, String tableName, Object recordId, Map<String, Object> updates) {
        DatabaseInstance instance = findInstance(dbId);
        String url = buildJdbcUrl(instance);
        String decryptedPassword = encryptionUtil.decrypt(instance.getDbPassword());

        StringBuilder sqlBuilder = new StringBuilder(String.format("UPDATE %s SET ", tableName));
        List<Object> params = new ArrayList<>();

        String setClause = updates.keySet().stream()
                .map(entry -> {
                    params.add(updates.get(entry));
                    return String.format("%s = ?", entry);
                })
                .collect(Collectors.joining(", "));
        sqlBuilder.append(setClause);

        sqlBuilder.append(" WHERE id = ?");
        params.add(parseId(recordId)); // Use the parsed ID

        String sql = sqlBuilder.toString();

        try (Connection conn = DriverManager.getConnection(url, instance.getDbUser(), decryptedPassword);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new InvalidInputException("Failed to update record in table '" + tableName + "': " + e.getMessage());
        }
    }

    public int deleteRecord(Long dbId, String tableName, Object recordId) {
        DatabaseInstance instance = findInstance(dbId);
        String url = buildJdbcUrl(instance);
        String decryptedPassword = encryptionUtil.decrypt(instance.getDbPassword());

        String sql = String.format("DELETE FROM %s WHERE id = ?", tableName);

        try (Connection conn = DriverManager.getConnection(url, instance.getDbUser(), decryptedPassword);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, parseId(recordId)); // Use the parsed ID
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new InvalidInputException("Failed to delete record from table '" + tableName + "': " + e.getMessage());
        }
    }

    private Object parseId(Object originalId) {
        if (originalId instanceof String) {
            try {
                // Try to parse it as a Long (which covers Integers)
                return Long.parseLong((String) originalId);
            } catch (NumberFormatException e) {
                // If it fails, it's probably a genuine string ID (like a UUID), so we return it as is.
                return originalId;
            }
        }
        // If it's already a number, return it as is.
        return originalId;
    }

    private DatabaseInstance findInstance(Long dbId) {
        return instanceRepository.findById(dbId)
                .orElseThrow(() -> new ResourceNotFoundException("Database instance not found with id: " + dbId));
    }

    private String buildJdbcUrl(DatabaseInstance instance) {
        return String.format("jdbc:postgresql://%s:%d/%s", instance.getHost(), instance.getPort(), instance.getDbName());
    }
}
