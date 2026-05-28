package com.smartfactory.visioninspection.models;

public class User {
    private final String employeeId;
    private final String name;
    private final String password;
    private final String department;
    private final String role;
    private final String phoneNumber;

    public User(String employeeId,
                String name,
                String password,
                String department,
                String role,
                String phoneNumber) {
        this.employeeId = employeeId;
        this.name = name;
        this.password = password;
        this.department = department;
        this.role = role;
        this.phoneNumber = phoneNumber;
    }

    public String getEmployeeId() { return employeeId; }
    public String getName() { return name; }
    public String getPassword() { return password; }
    public String getDepartment() { return department; }
    public String getRole() { return role; }
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
            sb.append(roleValue);
        }
        if (!phone.isEmpty()) {
            if (sb.length() > 0) sb.append("  ");
            sb.append(phone);
        }

        return sb.length() == 0 ? "-" : sb.toString();
    }
}
