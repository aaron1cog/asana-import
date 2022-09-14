package com.cappella.asana;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.asana.Client;
import com.asana.models.Project;
import com.asana.models.ResultBody;
import com.asana.models.ResultBodyCollection;
import com.asana.models.Section;
import com.asana.models.Task;
import com.asana.models.Workspace;
import com.asana.requests.CollectionRequest;
import com.asana.requests.ItemRequest;
import com.cappella.model.Problems;
import com.cappella.model.TaskData;
import com.google.gson.JsonElement;
 
@Service
/**
 * This class handles the communication with Asana.
 * It currently uses a Personal Access Token 
 * https://developers.asana.com/docs/personal-access-token
 * The workspace name and project name must be valid for the Asana workspace
 * or communication will fail.
 * 
 */
public class AsanaClient {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private static final String ASANA_PRETTY = "pretty";
    private static final String ASANA_NAME = "name";
    private static final String ASANA_DUE_ON = "due_on";
    private static final String ASANA_SECTION = "section";
    private static final String ASANA_TASK = "task";
    private static final String ASANA_WORKSPACE = "workspace";
    private static final String ASANA_PROJECT = "project";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd",
    Locale.ENGLISH);

    private final Client client;

    // TODO
    // * handle pagination
    // * add subtasks
    // * update subtasks
    // * once Description is added to TaskData need to update/insert it here

    /**
     * The personalAccessToken must be a valid token or any communication to 
     * Asana will fail.
     * @param personalAccessToken
     */
    public AsanaClient(String personalAccessToken) {
        client = Client.accessToken(personalAccessToken);
    }

    /**
     * WorkspaceName and ProjectName must be valid for the Asana workspace
     * or communication to Asana will fail.
     * 
     * Tasks must not be null and each TaskData object in it must have a name
     * at the very least or no data will be sent to Asana.
     * 
     * Problems is a list of all warnings and errors encountered during communication
     * with Asana.
     * 
     * @param workspaceName
     * @param projectName
     * @param tasks
     * @param problems
     */
    public void updateOrInsertGrantTasks(String workspaceName, String projectName, List<TaskData> tasks,
            Problems problems) {
        // TODO - ensure all parameters are not null
        Workspace workspace = getWorkspace(workspaceName, problems);
        if (workspace != null) {
            Project project = getProject(workspace, projectName, problems);
            if (project != null) {
                updateOrInsertTasks(workspace, project, tasks, problems);
            }
        }
    }

    Workspace getWorkspace(String workspaceName, Problems problems) {
        Workspace workspace = null;
        try {
            List<Workspace> workspaces = client.workspaces.getWorkspaces()
                    .option(ASANA_PRETTY, true)
                    .execute();
            for (Workspace temp : workspaces) {
                if (temp.name.equals(workspaceName)) {
                    workspace = temp;
                    return workspace;
                }
            }
        } catch (Exception e) {
            problems.addError(Problems.ERROR_FROM_ASANA, e.toString());
            LOGGER.debug("error getting workspace " + e.toString());
        }
        if (workspace == null) {
            problems.addError(Problems.ERROR_NO_WORKSPACE, "Workspace = " + workspaceName);
        }
        return workspace;
    }

    Project getProject(Workspace workspace, String projectName, Problems problems) {
        Project project = null;
        try {
            List<Project> projects = client.projects.getProjects(false, null, workspace.gid)
                    .option(ASANA_PRETTY, true)
                    .execute();
            for (Project temp : projects) {
                if (temp.name.equals(projectName)) {
                    project = temp;
                    break;
                }
            }
        } catch (Exception e) {
            problems.addError(Problems.ERROR_FROM_ASANA, e.toString());
            LOGGER.debug("error getting project " + e.toString());
        }
        if (project == null) {
            problems.addError(Problems.ERROR_PROJECT_NOT_IN_WORKSPACE, "Workspace = " + workspace.name
                    + " Project = " + projectName);
        }
        return project;
    }

    /**
     * The flow for updating or inserting is:
     * 1 - Get the list of tasks that exist in Asana. Asana only returns a list.
     * 2 - Put the list from Asana into a map so it is easy to see if the 
     *     tasks passed in are existing and so should be updated.
     * 3 - Iterate over the list of tasks passed in.
     *       a. Ensure the task has a name.  Without a name it is malformed and must be ignored.
     *       b. If it exists in the map from Asana then update it.
     *       c. Else insert it.
     * 
     * @param workspace
     * @param project
     * @param tasks
     * @param problems
     */
    void updateOrInsertTasks(Workspace workspace, Project project, List<TaskData> tasks, Problems problems){
        List<Task> projectTasks = getProjectTasks(project, problems);
        Map<String, Task> taskMap = null;
        if (projectTasks != null) {
            taskMap = new HashMap<>();
            for (Task temp : projectTasks) {
                taskMap.put(temp.name, temp);
            }
        }
        Map<String, Section> sectionMap = getSectionMap(project.gid, problems);
        for (TaskData taskData : tasks) {
            // the only requirement for tasks is that they must have a name
            if (taskData.getName() != null) {
                updateOrInsertTask(workspace, project, sectionMap, taskMap, taskData, problems);
            } else {
                problems.addWarning(Problems.WARNING_TASKDATA_MISSING_NAME, null);

            }
        }
    }

    List<Task> getProjectTasks(Project project, Problems problems) {
        List<Task> projectTasks = null;
        try {
            // get the existing tasks from Asana
            // result.nextPage.offset is offsset for pagination
            CollectionRequest<Task> request = client.tasks.getTasksForProject(project.gid, null)
                    .option(ASANA_PRETTY, true);
            ResultBodyCollection<Task> result = request.executeRaw();
            projectTasks = result.data;
            if (result.nextPage != null) {
                // TODO need to handle pagination if nextPage is not null
            }
        } catch (Exception e) {
            problems.addError(Problems.ERROR_FROM_ASANA, e.toString());
            LOGGER.debug("error getting project tasks " + e.toString());
        }
        return projectTasks;
    }

    Section getOrCreateSectionGid(String sectionName, Map<String, Section> sectionMap, String projectGid,
            Problems problems) {
        // in the case that the section associated with this task does not yet exist
        // it needs to be created on the project before associating it with the task
        Section section = null;
        try {
            if (sectionName != null && !sectionName.isBlank()) {
                Section existingSection = sectionMap.get(sectionName);
                if (existingSection != null) {
                    section = existingSection;
                } else {
                    Section newSection = client.sections.createSectionForProject(projectGid)
                            .data(ASANA_NAME, sectionName)
                            .option(ASANA_PRETTY, true)
                            .execute();
                    sectionMap.put(sectionName, newSection);
                    section = newSection;
                }
            }
        } catch (Exception e) {
            problems.addError(Problems.ERROR_FROM_ASANA, e.toString());
            LOGGER.debug("error setting section " + e.toString());
        }
        return section;
    }

    Map<String, Section> getSectionMap(String projectGid, Problems problems) {
        // Create map of sections since we don't want to create new ones if they already
        // exist.
        Map<String, Section> sectionMap = new HashMap<>();
        try {
            List<Section> sections = client.sections.getSectionsForProject(projectGid)
                    .option(ASANA_PRETTY, true)
                    .execute();
            for (Section section : sections) {
                sectionMap.put(section.name, section);
            }
        } catch (Exception e) {
            problems.addError(Problems.ERROR_FROM_ASANA, e.toString());
            LOGGER.debug("error getting sections for map " + e.toString());
        }
        return sectionMap;
    }

    void updateOrInsertTask(Workspace workspace, Project project, Map<String, Section> sectionMap, 
                            Map<String, Task> taskMap, TaskData taskData, Problems problems){
        if (taskMap != null && taskMap.containsKey(taskData.getName())) {
            Task existingTask = taskMap.get(taskData.getName());
            taskData.setAsanaData(existingTask);
            updateTask(project, taskData, existingTask, sectionMap, problems);
        } else {
            insertTask(workspace, project, taskData, sectionMap, problems);
        }
    }

    void updateTask(Project project, TaskData taskData, Task existingTask, Map<String, Section> sectionMap,
            Problems problems) {
        try {
            // TODO - add description
            if (taskData.getDueDate() != null) {
                Task updateTask = client.tasks.updateTask(existingTask.gid)
                        .data(ASANA_DUE_ON, taskData.getDueDate().format(DATE_FORMATTER))
                        .option(ASANA_PRETTY, true)
                        .execute();
                taskData.setAsanaData(updateTask);
            }
            // adding a task to a section will remove it from the section it is already
            // in, thus add and update are the same action
            Section section = getOrCreateSectionGid(taskData.getSection(), sectionMap, project.gid, problems);
            if (section != null) {
                JsonElement addTaskToSectionResult = client.sections.addTaskForSection(section.gid)
                        .data(ASANA_TASK, existingTask.gid)
                        .option(ASANA_PRETTY, true)
                        .execute();
                taskData.setAsanaSection(section.name);
            }
            // getting subtasks is a specific call
            // creating subtasks is a specific call
            // updating or deleting subtasks should use the task calls,
            // since subtasks are simply task objects with other tasks as the parent

            // get subtasks
            // iterate over them and see if the ones in the taskData are in Asana
            // if so update
            // else create
        } catch (Exception e) {
            problems.addError(Problems.ERROR_FROM_ASANA, e.toString());
            LOGGER.debug("error updating task " + e.toString());
        }
    }

    void insertTask(Workspace workspace, Project project, TaskData taskData, Map<String, Section> sectionMap,
            Problems problems) {
        try {
            // create the task
            // TODO - need to check that due_on isn't null
            // TODO - need to add description
            ItemRequest<Task> newTaskRequest = client.tasks.createTask()
                    .data(ASANA_NAME, taskData.getName())
                    .data(ASANA_WORKSPACE, workspace.gid)
                    .option(ASANA_PRETTY, true);
            if (taskData.getDueDate() != null) {
                newTaskRequest = newTaskRequest.data(ASANA_DUE_ON, taskData.getDueDate().format(DATE_FORMATTER));
            }
            ResultBody<Task> newTaskResult = newTaskRequest.executeRaw();
            Task newTask = newTaskResult.data;
            taskData.setAsanaData(newTask);
            Section section = getOrCreateSectionGid(taskData.getSection(), sectionMap, project.gid, problems);
            // set the project
            ItemRequest<JsonElement> requestAddProjectForTask = client.tasks.addProjectForTask(newTask.gid)
                    .data(ASANA_PROJECT, project.gid)
                    .option(ASANA_PRETTY, true);
            // determine if there is a section and if so add it as part of this call
            if (section != null) {
                // add section to the task
                requestAddProjectForTask = requestAddProjectForTask.data(ASANA_SECTION, section.gid);
                taskData.setAsanaSection(section.name);
            }
            JsonElement json = requestAddProjectForTask.execute();
            // subtasks are separate calls
        } catch (Exception e) {
            problems.addError(Problems.ERROR_FROM_ASANA, e.toString());
            LOGGER.debug("error inserting task " + e.toString());
        }
    }

    void deleteTasks(List<TaskData> tasks) {
        for (TaskData task : tasks) {
            // to account for negative tests we should only delete tasks that are sure to
            // exist in Asana which is indicated by task.getAsanaData() != null
            if (task.getAsanaData() != null) {
                try {
                    JsonElement result = client.tasks.deleteTask(task.getAsanaData().gid)
                            .option(ASANA_PRETTY, true)
                            .execute();
                    LOGGER.debug("deleted task " + task.getName() + "  " + result.toString());
                } catch (Exception e) {
                    LOGGER.debug(e.toString());
                    LOGGER.debug("error deleting task " + e.toString());
                }
            }
        }
    }
}
