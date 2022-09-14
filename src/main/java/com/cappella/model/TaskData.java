package com.cappella.model;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import com.asana.models.Task;

public class TaskData {

    private String name;
    private LocalDate dueDate;
    // need to add description
    private String section;
    private List<SubTask> subTasks;
    private Task asanaData;
    private String asanaSection;

    public TaskData() {
        subTasks = new ArrayList<>();
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

    public void setSection(String newSection) {
        section = newSection;
    }

    public String getSection() {
        return section;
    }

    public void setAsanaSection(String newSection) {
        asanaSection = newSection;
    }

    public String getAsanaSection() {
        return asanaSection;
    }

    public void addSubTask(SubTask subTask) {
        subTasks.add(subTask);
    }

    public List<SubTask> getSubTasks() {
        return subTasks;
    }

    public void setAsanaData(Task data) {
        asanaData = data;
    }

    public Task getAsanaData() {
        return asanaData;
    }

}
