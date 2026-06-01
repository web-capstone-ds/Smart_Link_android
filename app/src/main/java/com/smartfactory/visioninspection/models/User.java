package com.smartfactory.visioninspection.models;

public class User {
    private final String employeeId;
    private final String name;
    private final String department;
    private final String role;
    private final String phoneNumber;

    public User(String employeeId,
                String name,
                String department,
                String role,
                String phoneNumber) {
        this.employeeId = employeeId;
        this.name = name;
        this.department = department;
        this.role = role;
        this.phoneNumber = phoneNumber;
    }

    // Legacy constructor (password is ignored; kept only for compatibility with old mock code).
    public User(String employeeId,
                String name,
                String passwordIgnored,
                String department,
                String role,
                String phoneNumber) {
        this(employeeId, name, department, role, phoneNumber);
    }

    public String getEmployeeId() { return employeeId; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public String getRole() { return toRoleLabel(role); }
    public String getRoleCode() { return role; }
    public String getPhoneNumber() { return phoneNumber; }

    public String getDisplayName() {
        return name + " (" + employeeId + ")";
    }

    public String getDepartmentPhoneLabel() {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return department;
        }
        return department + "  " + phoneNumber;
    }
    public String getDepartmentRolePhoneLabel() {
        String dep = department == null ? "" : department.trim();
        String roleValue = role == null ? "" : role.trim();
        String phone = phoneNumber == null ? "" : phoneNumber.trim();

        StringBuilder sb = new StringBuilder();
        if (!dep.isEmpty()) sb.append(dep);
        if (!roleValue.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(toRoleLabel(roleValue));
        }
        if (!phone.isEmpty()) {
            if (sb.length() > 0) sb.append("  ");
            sb.append(phone);
        }

        return sb.length() == 0 ? "-" : sb.toString();
    }

    private String toRoleLabel(String roleValue) {
        if (roleValue == null) return "-";
        String normalized = roleValue.trim().toUpperCase();
        switch (normalized) {
            case "ADMIN":
                return "관리자";
            case "OPERATOR":
                return "운영자";
            case "INSPECTOR":
                return "검사원";
            case "ENGINEER":
                return "엔지니어";
            default:
                return roleValue;
        }
    }
}
