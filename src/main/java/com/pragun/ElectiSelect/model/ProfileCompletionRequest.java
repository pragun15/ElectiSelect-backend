package com.pragun.ElectiSelect.model;

public class ProfileCompletionRequest {
    private String name;
    private String phone;
    private String department;
    private Integer semester; // Required for STUDENT, null for STAFF

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }
}
