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
        subTasks = new ArrayList<SubTask>();
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

    /**
     * @param newSection
     */
    public void setSection(String newSection) {
        section = newSection;
    }

    /**
     * @return String
     */
    public String getSection() {
        return section;
    }

    public void setAsanaSection(String newSection) {
        asanaSection = newSection;
    }

    /**
     * @return String
     */
    public String getAsanaSection() {
        return asanaSection;
    }

    /**
     * @param subTask
     */
    public void addSubTask(SubTask subTask) {
        subTasks.add(subTask);
    }

    /**
     * @return List<SubTask>
     */
    public List<SubTask> getSubTasks() {
        return subTasks;
    }

    /**
     * @param data
     */
    public void setAsanaData(Task data) {
        asanaData = data;
    }

    /**
     * @return Task
     */
    public Task getAsanaData() {
        return asanaData;
    }

}
