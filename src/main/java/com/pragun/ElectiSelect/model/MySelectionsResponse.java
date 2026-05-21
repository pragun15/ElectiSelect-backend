package com.pragun.ElectiSelect.model;

public class MySelectionsResponse {
    private SelectionDetailDTO openElective;
    private SelectionDetailDTO departmentElective;

    public MySelectionsResponse() {
        this.openElective = new SelectionDetailDTO(false);
        this.departmentElective = new SelectionDetailDTO(false);
    }

    public SelectionDetailDTO getOpenElective() {
        return openElective;
    }

    public void setOpenElective(SelectionDetailDTO openElective) {
        this.openElective = openElective;
    }

    public SelectionDetailDTO getDepartmentElective() {
        return departmentElective;
    }

    public void setDepartmentElective(SelectionDetailDTO departmentElective) {
        this.departmentElective = departmentElective;
    }
}
