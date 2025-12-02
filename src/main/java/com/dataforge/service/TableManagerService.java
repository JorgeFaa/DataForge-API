package com.dataforge.service;

import com.dataforge.dto.ColumnDefinition;
import com.dataforge.dto.CreateTableRequest;
import com.dataforge.dto.ForeignKeyDefinition;
import com.dataforge.dto.ModifyColumnRequest;
import com.dataforge.dto.TableRelationship;
import com.dataforge.dto.TableSchemaResponse;
import com.dataforge.exception.ResourceNotFoundException; // Import ResourceNotFoundException
import com.dataforge.exception.InvalidInputException; // Import InvalidInputException
import com.dataforge.model.DatabaseInstance;
import com.dataforge.repository.DatabaseInstanceRepository;
import com.dataforge.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TableManagerService {

    @Autowired
    private DatabaseInstanceRepository instanceRepository;
    @Autowired
    private EncryptionUtil encryptionUtil;

    public void createTable(Long dbId, CreateTableRequest request) {
        DatabaseInstance instance = findInstance(dbId);
        String url = buildJdbcUrl(instance);
        String decryptedPassword = encryptionUtil.decrypt(instance.getDbPassword());

        String createTableSql = buildCreateTableSql(request);

        try (Connection conn = DriverManager.getConnection(url, instance.getDbUser(), decryptedPassword);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
        } catch (SQLException e) {
            throw new InvalidInputException("Failed to create table: " + e.getMessage()); // Use InvalidInputException
        }
    }

    public List<String> listTables(Long dbId) {
        DatabaseInstance instance = findInstance(dbId);
        String url = buildJdbcUrl(instance);
        String decryptedPassword = encryptionUtil.decrypt(instance.getDbPassword());
        List<String> tableNames = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, instance.getDbUser(), decryptedPassword)) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, "public", "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tableNames.add(rs.getString("TABLE_NAME"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list tables: " + e.getMessage(), e); // Keep RuntimeException for unexpected DB errors
        }
        return tableNames;
    }

    public TableSchemaResponse getTableSchema(Long dbId, String tableName) {
        DatabaseInstance instance = findInstance(dbId);
        String url = buildJdbcUrl(instance);
        String decryptedPassword = encryptionUtil.decrypt(instance.getDbPassword());
        List<ColumnDefinition> columns = new ArrayList<>();
        Set<String> primaryKeyColumns = new HashSet<>();

        try (Connection conn = DriverManager.getConnection(url, instance.getDbUser(), decryptedPassword)) {
            DatabaseMetaData metaData = conn.getMetaData();

            try (ResultSet pkRs = metaData.getPrimaryKeys(null, "public", tableName)) {
                while (pkRs.next()) {
                    primaryKeyColumns.add(pkRs.getString("COLUMN_NAME"));
                }
            }

            try (ResultSet rs = metaData.getColumns(null, "public", tableName, "%")) {
                if (!rs.isBeforeFirst()) { // Check if ResultSet is empty
                    throw new ResourceNotFoundException("Table '" + tableName + "' not found in database ID " + dbId);
                }
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    String dataType = rs.getString("TYPE_NAME");
                    int size = rs.getInt("COLUMN_SIZE");
                    if (dataType.equalsIgnoreCase("varchar") || dataType.equalsIgnoreCase("char")) {
                        dataType += "(" + size + ")";
                    }
                    boolean isNullable = "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
                    boolean isPrimaryKey = primaryKeyColumns.contains(columnName);
                    
                    columns.add(new ColumnDefinition(columnName, dataType, isPrimaryKey, !isNullable, false));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get table schema: " + e.getMessage(), e);
        }
        return new TableSchemaResponse(tableName, columns);
    }

    public void deleteTable(Long dbId, String tableName) {
        DatabaseInstance instance = findInstance(dbId);
        String url = buildJdbcUrl(instance);
        String decryptedPassword = encryptionUtil.decrypt(instance.getDbPassword());
        String dropTableSql = "DROP TABLE IF EXISTS " + tableName + " CASCADE";

        try (Connection conn = DriverManager.getConnection(url, instance.getDbUser(), decryptedPassword);
             Statement stmt = conn.createStatement()) {
            stmt.execute(dropTableSql);
        } catch (SQLException e) {
            throw new InvalidInputException("Failed to delete table '" + tableName + "': " + e.getMessage()); // Use InvalidInputException
        }
    }

    public void addColumn(Long dbId, String tableName, ColumnDefinition columnDefinition) {
        DatabaseInstance instance = findInstance(dbId);
        String url = buildJdbcUrl(instance);
        String decryptedPassword = encryptionUtil.decrypt(instance.getDbPassword());

        String addColumnSql = String.format("ALTER TABLE %s ADD COLUMN %s", tableName, buildColumnDefinitionSql(columnDefinition));
        System.out.println("Executing SQL: " + addColumnSql);

        try (Connection conn = DriverManager.getConnection(url, instance.getDbUser(), decryptedPassword);
             Statement stmt = conn.createStatement()) {
            stmt.execute(addColumnSql);
        } catch (SQLException e) {
            throw new InvalidInputException("Failed to add column '" + columnDefinition.name() + "' to table '" + tableName + "': " + e.getMessage()); // Use InvalidInputException
        }
    }

    public void modifyColumn(Long dbId, String tableName, String oldColumnName, ModifyColumnRequest request) {
        DatabaseInstance instance = findInstance(dbId);
        String url = buildJdbcUrl(instance);
        String decryptedPassword = encryptionUtil.decrypt(instance.getDbPassword());

        try (Connection conn = DriverManager.getConnection(url, instance.getDbUser(), decryptedPassword);
             Statement stmt = conn.createStatement()) {

            String currentColumnName = oldColumnName;

            if (request.newColumnName() != null && !request.newColumnName().isBlank() && !request.newColumnName().equals(oldColumnName)) {
                String renameSql = String.format("ALTER TABLE %s RENAME COLUMN %s TO %s", tableName, currentColumnName, request.newColumnName());
                System.out.println("Executing SQL: " + renameSql);
                stmt.execute(renameSql);
                currentColumnName = request.newColumnName();
            }

            if (request.newDataType() != null && !request.newDataType().isBlank()) {
                String alterTypeSql = String.format("ALTER TABLE %s ALTER COLUMN %s TYPE %s", tableName, currentColumnName, request.newDataType());
                System.out.println("Executing SQL: " + alterTypeSql);
                stmt.execute(alterTypeSql);
            }

            if (request.isNullable() != null) {
                String alterNullabilitySql;
                if (request.isNullable()) {
                    alterNullabilitySql = String.format("ALTER TABLE %s ALTER COLUMN %s DROP NOT NULL", tableName, currentColumnName);
                } else {
                    alterNullabilitySql = String.format("ALTER TABLE %s ALTER COLUMN %s SET NOT NULL", tableName, currentColumnName);
                }
                System.out.println("Executing SQL: " + alterNullabilitySql);
                stmt.execute(alterNullabilitySql);
            }

        } catch (SQLException e) {
            throw new InvalidInputException("Failed to modify column '" + oldColumnName + "' in table '" + tableName + "': " + e.getMessage()); // Use InvalidInputException
        }
    }

    public void deleteColumn(Long dbId, String tableName, String columnName) {
        DatabaseInstance instance = findInstance(dbId);
        String url = buildJdbcUrl(instance);
        String decryptedPassword = encryptionUtil.decrypt(instance.getDbPassword());

        String dropColumnSql = String.format("ALTER TABLE %s DROP COLUMN %s", tableName, columnName);
        System.out.println("Executing SQL: " + dropColumnSql);

        try (Connection conn = DriverManager.getConnection(url, instance.getDbUser(), decryptedPassword);
             Statement stmt = conn.createStatement()) {
            stmt.execute(dropColumnSql);
        } catch (SQLException e) {
            throw new InvalidInputException("Failed to delete column '" + columnName + "' from table '" + tableName + "': " + e.getMessage()); // Use InvalidInputException
        }
    }

    public void dropForeignKey(Long dbId, String tableName, String constraintName) {
        DatabaseInstance instance = findInstance(dbId);
        String url = buildJdbcUrl(instance);
        String decryptedPassword = encryptionUtil.decrypt(instance.getDbPassword());

        String dropFkSql = String.format("ALTER TABLE %s DROP CONSTRAINT %s", tableName, constraintName);
        System.out.println("Executing SQL: " + dropFkSql);

        try (Connection conn = DriverManager.getConnection(url, instance.getDbUser(), decryptedPassword);
             Statement stmt = conn.createStatement()) {
            stmt.execute(dropFkSql);
        } catch (SQLException e) {
            throw new InvalidInputException("Failed to drop foreign key constraint '" + constraintName + "' from table '" + tableName + "': " + e.getMessage()); // Use InvalidInputException
        }
    }

    public List<TableRelationship> getTableRelationships(Long dbId, String tableName) {
        DatabaseInstance instance = findInstance(dbId);
        String url = buildJdbcUrl(instance);
        String decryptedPassword = encryptionUtil.decrypt(instance.getDbPassword());
        List<TableRelationship> relationships = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, instance.getDbUser(), decryptedPassword)) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getImportedKeys(null, "public", tableName)) {
                while (rs.next()) {
                    relationships.add(new TableRelationship(
                            rs.getString("FK_NAME"),
                            rs.getString("FKTABLE_NAME"),
                            rs.getString("FKCOLUMN_NAME"),
                            rs.getString("PKTABLE_NAME"),
                            rs.getString("PKCOLUMN_NAME")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get relationships for table '" + tableName + "': " + e.getMessage(), e); // Keep RuntimeException
        }
        return relationships;
    }

    private DatabaseInstance findInstance(Long dbId) {
        return instanceRepository.findById(dbId)
                .orElseThrow(() -> new ResourceNotFoundException("Database instance not found with id: " + dbId)); // Throw custom exception
    }

    private String buildJdbcUrl(DatabaseInstance instance) {
        return String.format("jdbc:postgresql://%s:%d/%s", instance.getHost(), instance.getPort(), instance.getDbName());
    }

    private String buildCreateTableSql(CreateTableRequest request) {
        StringBuilder sql = new StringBuilder("CREATE TABLE ")
                .append(request.tableName())
                .append(" (");

        List<String> definitions = new ArrayList<>();

        String columnsSql = request.columns().stream()
                .map(this::buildColumnDefinitionSql)
                .collect(Collectors.joining(", "));
        definitions.add(columnsSql);

        request.columns().stream()
                .filter(ColumnDefinition::isPrimaryKey)
                .map(ColumnDefinition::name)
                .findFirst()
                .ifPresent(pkColumn -> definitions.add("PRIMARY KEY (" + pkColumn + ")"));

        if (request.foreignKeys() != null) {
            request.foreignKeys().stream()
                    .filter(ForeignKeyDefinition::isValid)
                    .map(this::buildForeignKeyConstraintSql)
                    .forEach(definitions::add);
        }

        sql.append(String.join(", ", definitions));
        sql.append(");");
        
        System.out.println("Executing SQL: " + sql.toString());
        return sql.toString();
    }

    private String buildColumnDefinitionSql(ColumnDefinition column) {
        StringBuilder colSql = new StringBuilder();
        colSql.append(column.name()).append(" ").append(column.dataType());
        if (!column.isNullable()) {
            colSql.append(" NOT NULL");
        }
        if (column.isUnique()) {
            colSql.append(" UNIQUE");
        }
        return colSql.toString();
    }

    private String buildForeignKeyConstraintSql(ForeignKeyDefinition fk) {
        return String.format("CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)",
                fk.constraintName(),
                fk.localColumn(),
                fk.foreignTable(),
                fk.foreignColumn());
    }
}
