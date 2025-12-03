package com.dataforge.service;

import com.dataforge.dto.SqlQueryResponse;
import com.dataforge.exception.ResourceNotFoundException;
import com.dataforge.model.DatabaseInstance;
import com.dataforge.repository.DatabaseInstanceRepository;
import com.dataforge.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class QueryService {

    @Autowired
    private DatabaseInstanceRepository instanceRepository;
    @Autowired
    private EncryptionUtil encryptionUtil;

    public SqlQueryResponse executeQuery(Long dbId, String sql) {
        DatabaseInstance instance = instanceRepository.findById(dbId)
                .orElseThrow(() -> new ResourceNotFoundException("Database instance not found with id: " + dbId));

        String url = String.format("jdbc:postgresql://%s:%d/%s", instance.getHost(), instance.getPort(), instance.getDbName());
        String user = instance.getDbUser();
        String decryptedPassword = encryptionUtil.decrypt(instance.getDbPassword());

        try (Connection conn = DriverManager.getConnection(url, user, decryptedPassword);
             Statement stmt = conn.createStatement()) {

            // stmt.execute() returns 'true' if the result is a ResultSet (like a SELECT)
            // and 'false' if it's an update count or no result.
            boolean isResultSet = stmt.execute(sql);

            if (isResultSet) {
                // It was a SELECT query
                try (ResultSet rs = stmt.getResultSet()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    List<String> columns = new ArrayList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        columns.add(metaData.getColumnName(i));
                    }

                    List<Map<String, Object>> rows = new ArrayList<>();
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>(); // Use LinkedHashMap to preserve column order
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(metaData.getColumnName(i), rs.getObject(i));
                        }
                        rows.add(row);
                    }
                    return SqlQueryResponse.forSelect(columns, rows);
                }
            } else {
                // It was an UPDATE, INSERT, DELETE, or DDL statement
                int rowsAffected = stmt.getUpdateCount();
                return SqlQueryResponse.forUpdate(rowsAffected);
            }

        } catch (SQLException e) {
            // If there's a SQL error, return a structured error response
            return SqlQueryResponse.forError(e.getMessage());
        }
    }
}
