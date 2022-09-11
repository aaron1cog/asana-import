package com.cappella.model;

import java.time.LocalDate;

public class SubTask {

    private String name;
    private LocalDate dueDate;

    public SubTask() {
    }

    /**
     * @param newName
     */
    public void setName(String newName) {
        name = newName;
    }

    /**
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * @param newDate
     */
    public void setDueDate(LocalDate newDate) {
        dueDate = newDate;
    }

    /**
     * @return LocalDate
     */
    public LocalDate getDueDate() {
        return dueDate;
    }

}
