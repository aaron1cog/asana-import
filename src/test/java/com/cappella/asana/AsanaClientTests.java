package com.cappella.asana;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cappella.model.Problems;
import com.cappella.model.SubTask;
import com.cappella.model.TaskData;

class AsanaClientTests {

    final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Test
    void testInsertTasks() {
        Properties prop = getProperties();
        AsanaClient asana = new AsanaClient(prop.getProperty("asana.token"));
        ArrayList<TaskData> tasks = getInitialTasks();
        Problems problems = new Problems();
        // insert
        asana.insertOrUpdateGrantTasks(prop.getProperty("asana.workspace.name"),
                prop.getProperty("asana.project.name"), tasks, problems);
        // cleanup the tasks
        asana.deleteTasks(tasks);
        verifyTasks(tasks);
        Assertions.assertTrue(problems.getErrors().isEmpty());
        Assertions.assertTrue(problems.getWarnings().containsKey(Problems.WARNING_TASKDATA_MISSING_NAME));
    }

    @Test
    void testUpdateTasks() {
        Properties prop = getProperties();
        AsanaClient asana = new AsanaClient(prop.getProperty("asana.token"));
        ArrayList<TaskData> tasks = getInitialTasks();
        Problems problems = new Problems();
        // insert
        asana.insertOrUpdateGrantTasks(prop.getProperty("asana.workspace.name"),
                prop.getProperty("asana.project.name"), tasks, problems);
        // update
        modifyTasks(tasks);
        asana.insertOrUpdateGrantTasks(prop.getProperty("asana.workspace.name"),
                prop.getProperty("asana.project.name"), tasks, problems);
        // cleanup the tasks
        asana.deleteTasks(tasks);
        verifyTasks(tasks);
        Assertions.assertTrue(problems.getErrors().isEmpty());
        Assertions.assertTrue(problems.getWarnings().containsKey(Problems.WARNING_TASKDATA_MISSING_NAME));
    }

    @Test
    void testBadToken() {
        Properties prop = getProperties();
        AsanaClient asana = new AsanaClient("flubber");
        ArrayList<TaskData> tasks = getInitialTasks();
        Problems problems = new Problems();
        // insert
        asana.insertOrUpdateGrantTasks(prop.getProperty("asana.workspace.name"),
                prop.getProperty("asana.project.name"), tasks, problems);
        Assertions.assertTrue(problems.getErrors().containsKey(Problems.ERROR_FROM_ASANA));
    }

    @Test
    void testBadWorkspace() {
        Properties prop = getProperties();
        AsanaClient asana = new AsanaClient(prop.getProperty("asana.token"));
        ArrayList<TaskData> tasks = getInitialTasks();
        Problems problems = new Problems();
        // insert
        asana.insertOrUpdateGrantTasks(prop.getProperty("flubber"),
                prop.getProperty("asana.project.name"), tasks, problems);
        Assertions.assertTrue(problems.getErrors().containsKey(Problems.ERROR_NO_WORKSPACE));
    }

    @Test
    void testBadProject() {
        Properties prop = getProperties();
        AsanaClient asana = new AsanaClient(prop.getProperty("asana.token"));
        ArrayList<TaskData> tasks = getInitialTasks();
        Problems problems = new Problems();
        // insert
        asana.insertOrUpdateGrantTasks(prop.getProperty("asana.workspace.name"),
                prop.getProperty("flubber"), tasks, problems);
        Assertions.assertTrue(problems.getErrors().containsKey(Problems.ERROR_PROJECT_NOT_IN_WORKSPACE));
    }

    /**
     * currently unable to get the property file vai spring methods
     * 
     * @Configuration
     *                @TestPropertySource("classpath:private/asana.properties")
     * 
     * @Autowired
     *            public Environment env;
     * 
     *            @Value("${asana.token}")
     *            String asanaToken;
     * 
     *            so using the 'old fashioned' classload, inputstream method
     * 
     * @return
     */
    Properties getProperties() {
        ClassLoader classloader = this.getClass().getClassLoader();
        InputStream is = classloader.getResourceAsStream("private/asana.properties");
        Assertions.assertTrue((is != null), "unable to load private/asana.properties");
        Properties prop = new Properties();
        try {
            prop.load(is);
        } catch (IOException e) {
            Assertions.fail("IOException trying load properties from private/asana.properties ", e);
        }
        return prop;
    }

    /**
     * @return ArrayList<TaskData>
     */
    ArrayList<TaskData> getInitialTasks() {
        ArrayList<TaskData> tasks = new ArrayList<TaskData>();
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
    void modifyTasks(ArrayList<TaskData> tasks) {
        for (TaskData temp : tasks) {
            if (temp.getDueDate() != null) {
                temp.setDueDate(temp.getDueDate().plusDays(4L));
            }
            temp.setSection("Declined");
            // TODO subtasks
        }
    }

    /**
     * 
     * @param tasks
     */
    void verifyTasks(ArrayList<TaskData> tasks) {
        // To verify that the information was added to Asana
        // we need to look at the data which was returned from Asana
        // name, due date, section, subtasks, and description
        // unfortunately I haven't figured out how to get due_on or description back
        // from Asana
        // only verifying name and section
        for (TaskData task : tasks) {
            // the only time the names won't match is if the original task has a null name
            // so
            // verify that the task.getName() is not null
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
