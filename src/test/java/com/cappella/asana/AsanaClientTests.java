package com.cappella.asana;

import com.cappella.model.Problems;
import com.cappella.model.SubTask;
import com.cappella.model.TaskData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class AsanaClientTests {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Value("${asana.workspace.name}")
    private String workspaceName;
    @Value("${asana.project.name}")
    private String projectName;
    @Autowired
    private AsanaClient asana;
    
    private final Problems problems;

    public AsanaClientTests(){
        this.problems = new Problems();
    }

    @AfterEach
    private void afterTest(){
        // TODO - this isn't necessary. JUnit creates a new instance of this class for each test method it runs.
        this.problems.clear();
    }

    @Test
    void testInsertTasks() {
        List<TaskData> tasks = getInitialTasks();
        // insert
        this.asana.updateOrInsertGrantTasks(workspaceName, projectName, tasks, problems);
        verifyTasks(tasks);
        Assertions.assertTrue(problems.getErrors().isEmpty());
        Assertions.assertTrue(problems.getWarnings().containsKey(Problems.WARNING_TASKDATA_MISSING_NAME));
        // delete/cleanup the test tasks added to Asana 
        this.asana.deleteTasks(tasks);
    }

    @Test
    void testUpdateTasks() {
        List<TaskData> tasks = getInitialTasks();
        // insert
        this.asana.updateOrInsertGrantTasks(workspaceName, projectName, tasks, problems);
        // update
        modifyTasks(tasks);
        this.asana.updateOrInsertGrantTasks(workspaceName, projectName, tasks, problems);
        verifyTasks(tasks);
        Assertions.assertTrue(problems.getErrors().isEmpty());
        Assertions.assertTrue(problems.getWarnings().containsKey(Problems.WARNING_TASKDATA_MISSING_NAME));
        // delete/cleanup the test tasks added to Asana 
        this.asana.deleteTasks(tasks);
    }

    @Test
    void testBadToken() {
        // don't use this.asana because want to test an invalid token
        AsanaClient asanaClient = new AsanaClient("flubber");
        asanaClient.updateOrInsertGrantTasks(workspaceName, projectName, getInitialTasks(), problems);
        Assertions.assertTrue(problems.getErrors().containsKey(Problems.ERROR_FROM_ASANA));
    }

    @Test
    void testBadWorkspace() {
        this.asana.updateOrInsertGrantTasks("I LIKE MONKEYS", projectName, getInitialTasks(), problems);
        Assertions.assertTrue(problems.getErrors().containsKey(Problems.ERROR_NO_WORKSPACE));
    }

    @Test
    void testBadProject() {
        this.asana.updateOrInsertGrantTasks(workspaceName, "I LIKE MONKEYS", getInitialTasks(), problems);
        Assertions.assertTrue(problems.getErrors().containsKey(Problems.ERROR_PROJECT_NOT_IN_WORKSPACE));
    }

    /**
     * @return List<TaskData>
     */
    List<TaskData> getInitialTasks() {
        List<TaskData> tasks = new ArrayList<>();
        // all fields
        tasks.add(createTaskData("test task name 1", LocalDate.now(), "Submitted",
                "test subTask 1", LocalDate.now().plusDays(1)));
        // task date null
        tasks.add(createTaskData("test task name 2", null, "Declined",
                "test subTask 2", LocalDate.now().plusDays(1)));
        // task without section
        tasks.add(createTaskData("test task name 3", LocalDate.now(), null,
                "test subTask 3", LocalDate.now().plusDays(1)));
        // task without subtask
        tasks.add(createTaskData("test task name 4", LocalDate.now(), "Awarded",
                null, null));
        // task with subtask but no date
        tasks.add(createTaskData("test task name 5", LocalDate.now(), "Abandoned",
                "test subTask 5", null));
        // task without name - this is a negative test
        tasks.add(createTaskData(null, LocalDate.now(), "Abandoned",
                "test subTask 6", LocalDate.now()));
        // all fields adding this one after the negative test to ensure the negative is
        // skipped and all tasks after it are still processed
        tasks.add(createTaskData("test task name 7", LocalDate.now(), "Submitted",
                "test subTask 7", LocalDate.now().plusDays(1)));
        return tasks;
    }

    /**
     * 
     * @param taskName
     * @param taskDate
     * @param sectionName
     * @param subTaskName
     * @param subTaskDate
     * @return
     */
    TaskData createTaskData(String taskName, LocalDate taskDate, String sectionName,
            String subTaskName, LocalDate subTaskDate) {
        TaskData task = new TaskData();
        task.setName(taskName);
        task.setDueDate(taskDate);
        task.setSection(sectionName);
        if (sectionName != null) {
            SubTask subTask = new SubTask();
            subTask.setName(subTaskName);
            subTask.setDueDate(subTaskDate);
            task.addSubTask(subTask);
        }
        return task;
    }

    /**
     * @param tasks
     */
    void modifyTasks(List<TaskData> tasks) {
        for (TaskData temp : tasks) {
            if (temp.getDueDate() != null) {
                temp.setDueDate(temp.getDueDate().plusDays(4L));
            }
            temp.setSection("Declined");
            // TODO subtasks
        }
    }

    /**
     * To verify that the information was added to Asana
     * we need to look at the data which was returned from Asana
     * name, due date, section, subtasks, and description
     * unfortunately I haven't figured out how to get due_on or description back
     * from Asana only verifying name and section
     * 
     * @param tasks
     */
    void verifyTasks(List<TaskData> tasks) {
        for (TaskData task : tasks) {
            // the only time the names won't match is if the original task has a null name
            // so verify that the task.getName() is not null
            if (task.getName() != null) {
                Assertions.assertEquals(task.getName(), task.getAsanaData().name,
                        "Names do not match for " + task.getName() + " != " + task.getAsanaData().name);
                // this assertion should work if the section is null as well as populated
                Assertions.assertEquals(task.getSection(), task.getAsanaSection(),
                        "Section for " + task.getName() + " " + task.getSection() + " != " + task.getAsanaSection());
            }
        }
    }

}
