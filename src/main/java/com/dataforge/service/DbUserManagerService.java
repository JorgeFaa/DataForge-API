package com.dataforge.service;

import com.dataforge.dto.DbUserInfo;
import com.dataforge.dto.ManagePermissionsRequest;
import com.dataforge.dto.UserPermissionInfo;
import com.dataforge.exception.ResourceNotFoundException;
import com.dataforge.exception.InvalidInputException;
import com.dataforge.model.DatabaseInstance;
import com.dataforge.repository.DatabaseInstanceRepository;
import com.dataforge.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DbUserManagerService {

    @Autowired
    private DatabaseInstanceRepository instanceRepository;
    @Autowired
    private EncryptionUtil encryptionUtil;

    public void createDbUser(Long dbId, String username, String password) {
        DatabaseInstance instance = instanceRepository.findById(dbId)
                .orElseThrow(() -> new ResourceNotFoundException("Database instance not found with id: " + dbId));

        String url = String.format("jdbc:postgresql://%s:%d/%s", instance.getHost(), instance.getPort(), instance.getDbName());
        String dbAdminUser = instance.getDbUser();
        String dbAdminPassword = encryptionUtil.decrypt(instance.getDbPassword());

        String createUserSql = String.format("CREATE USER %s WITH PASSWORD '%s';", username, password.replace("'", "''"));
        System.out.println("Executing SQL: " + createUserSql);

        try (Connection conn = DriverManager.getConnection(url, dbAdminUser, dbAdminPassword);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createUserSql);
        } catch (SQLException e) {
            throw new InvalidInputException("Failed to create database user '" + username + "': " + e.getMessage());
        }
    }

    public void deleteDbUser(Long dbId, String username) {
        DatabaseInstance instance = instanceRepository.findById(dbId)
                .orElseThrow(() -> new ResourceNotFoundException("Database instance not found with id: " + dbId));

        String url = String.format("jdbc:postgresql://%s:%d/%s", instance.getHost(), instance.getPort(), instance.getDbName());
        String dbAdminUser = instance.getDbUser();
        String dbAdminPassword = encryptionUtil.decrypt(instance.getDbPassword());

        String dropUserSql = String.format("DROP USER IF EXISTS %s;", username);
        System.out.println("Executing SQL: " + dropUserSql);

        try (Connection conn = DriverManager.getConnection(url, dbAdminUser, dbAdminPassword);
             Statement stmt = conn.createStatement()) {
            stmt.execute(dropUserSql);
        } catch (SQLException e) {
            throw new InvalidInputException("Failed to delete database user '" + username + "': " + e.getMessage());
        }
    }

    public void grantPermissions(Long dbId, String username, ManagePermissionsRequest request) {
        DatabaseInstance instance = instanceRepository.findById(dbId)
                .orElseThrow(() -> new ResourceNotFoundException("Database instance not found with id: " + dbId));

        String url = String.format("jdbc:postgresql://%s:%d/%s", instance.getHost(), instance.getPort(), instance.getDbName());
        String dbAdminUser = instance.getDbUser();
        String dbAdminPassword = encryptionUtil.decrypt(instance.getDbPassword());

        String privileges = request.privileges().stream().collect(Collectors.joining(", "));
        String grantSql = String.format("GRANT %s ON TABLE %s TO %s;", privileges, request.tableName(), username);
        System.out.println("Executing SQL: " + grantSql);

        try (Connection conn = DriverManager.getConnection(url, dbAdminUser, dbAdminPassword);
             Statement stmt = conn.createStatement()) {
            stmt.execute(grantSql);
        } catch (SQLException e) {
            throw new InvalidInputException("Failed to grant permissions to user '" + username + "' on table '" + request.tableName() + "': " + e.getMessage());
        }
    }

    public void revokePermissions(Long dbId, String username, ManagePermissionsRequest request) {
        DatabaseInstance instance = instanceRepository.findById(dbId)
                .orElseThrow(() -> new ResourceNotFoundException("Database instance not found with id: " + dbId));

        String url = String.format("jdbc:postgresql://%s:%d/%s", instance.getHost(), instance.getPort(), instance.getDbName());
        String dbAdminUser = instance.getDbUser();
        String dbAdminPassword = encryptionUtil.decrypt(instance.getDbPassword());

        String privileges = request.privileges().stream().collect(Collectors.joining(", "));
        String revokeSql = String.format("REVOKE %s ON TABLE %s FROM %s;", privileges, request.tableName(), username);
        System.out.println("Executing SQL: " + revokeSql);

        try (Connection conn = DriverManager.getConnection(url, dbAdminUser, dbAdminPassword);
             Statement stmt = conn.createStatement()) {
            stmt.execute(revokeSql);
        } catch (SQLException e) {
            throw new InvalidInputException("Failed to revoke permissions from user '" + username + "' on table '" + request.tableName() + "': " + e.getMessage());
        }
    }

    public List<DbUserInfo> listDbUsers(Long dbId) {
        DatabaseInstance instance = instanceRepository.findById(dbId)
                .orElseThrow(() -> new ResourceNotFoundException("Database instance not found with id: " + dbId));

        String url = String.format("jdbc:postgresql://%s:%d/%s", instance.getHost(), instance.getPort(), instance.getDbName());
        String dbAdminUser = instance.getDbUser();
        String dbAdminPassword = encryptionUtil.decrypt(instance.getDbPassword());
        List<DbUserInfo> users = new ArrayList<>();

        // Query pg_catalog.pg_user to get user information
        String listUsersSql = "SELECT usename, usesuper, usecreatedb FROM pg_catalog.pg_user ORDER BY usename;";
        System.out.println("Executing SQL: " + listUsersSql);

        try (Connection conn = DriverManager.getConnection(url, dbAdminUser, dbAdminPassword);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(listUsersSql)) {

            while (rs.next()) {
                users.add(new DbUserInfo(
                    rs.getString("usename"),
                    rs.getBoolean("usecreatedb"),
                    rs.getBoolean("usesuper")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list database users: " + e.getMessage(), e);
        }
        return users;
    }

    public List<UserPermissionInfo> listUserPermissions(Long dbId, String username) {
        DatabaseInstance instance = instanceRepository.findById(dbId)
                .orElseThrow(() -> new ResourceNotFoundException("Database instance not found with id: " + dbId));

        String url = String.format("jdbc:postgresql://%s:%d/%s", instance.getHost(), instance.getPort(), instance.getDbName());
        String dbAdminUser = instance.getDbUser();
        String dbAdminPassword = encryptionUtil.decrypt(instance.getDbPassword());
        List<UserPermissionInfo> permissions = new ArrayList<>();

        // Query information_schema.role_table_grants to get table privileges for a user
        // Note: This only shows privileges granted directly to the user, not via roles.
        String listPermissionsSql = String.format(
            "SELECT table_name, privilege_type FROM information_schema.role_table_grants WHERE grantee = '%s' AND table_schema = 'public';",
            username.replace("'", "''")
        );
        System.out.println("Executing SQL: " + listPermissionsSql);

        try (Connection conn = DriverManager.getConnection(url, dbAdminUser, dbAdminPassword);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(listPermissionsSql)) {

            // Group privileges by table name
            var privilegesByTable = new java.util.HashMap<String, List<String>>();
            while (rs.next()) {
                String tableName = rs.getString("table_name");
                String privilegeType = rs.getString("privilege_type");
                privilegesByTable.computeIfAbsent(tableName, k -> new ArrayList<>()).add(privilegeType);
            }

            privilegesByTable.forEach((tableName, privilegeList) -> 
                permissions.add(new UserPermissionInfo(tableName, privilegeList))
            );

        } catch (SQLException e) {
            throw new RuntimeException("Failed to list permissions for user '" + username + "': " + e.getMessage(), e);
        }
        return permissions;
    }
}
