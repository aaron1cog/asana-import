package com.cappella.model;

import java.time.LocalDate;

public class SubTask {

    private String name;
    private LocalDate dueDate;

    public SubTask() {
    }

    public void setName(String newName) {
        name = newName;
    }

    public String getName() {
        return name;
    }

    public void setDueDate(LocalDate newDate) {
        dueDate = newDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

}
