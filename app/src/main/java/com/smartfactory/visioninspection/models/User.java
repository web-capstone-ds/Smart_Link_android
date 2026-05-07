package com.smartfactory.visioninspection.models;

/**
 * 사용자 계정 모델 (mockUsers.ts 대응)
 */
public class User {
    private final String employeeId;  // 사번
    private final String name;        // 이름
    private final String password;    // 비밀번호 (실 환경: 해시 처리)
    private final String department;  // 부서
    private final String role;        // 역할

    public User(String employeeId, String name, String password,
                String department, String role) {
        this.employeeId = employeeId;
        this.name       = name;
        this.password   = password;
        this.department = department;
        this.role       = role;
    }

    public String getEmployeeId()  { return employeeId; }
    public String getName()        { return name; }
    public String getPassword()    { return password; }
    public String getDepartment()  { return department; }
    public String getRole()        { return role; }

    /** 표시용: "김민준 (EMP001)" */
    public String getDisplayName() {
        return name + " (" + employeeId + ")";
    }
}
