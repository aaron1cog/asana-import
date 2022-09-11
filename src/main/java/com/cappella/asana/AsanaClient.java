package com.cappella.asana;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class AsanaClient {

    final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private String personalAccessToken;
    private Client client;

    // TODO
    // * add subtasks
    // * update subtasks
    // * review exception handling and if Problems should be passed in
    // * clean up logger messages

    /**
     * 
     * @param personalAccessToken
     * @param workspace
     */
    public AsanaClient(String accessToken) {
        personalAccessToken = accessToken;
    }

    /**
     * @param projectName
     * @param tasks
     * @param problems
     */
    public void insertOrUpdateGrantTasks(String workspaceName, String projectName, List<TaskData> tasks,
            Problems problems) {
        // TODO - ensure all parameters are not null
        client = Client.accessToken(personalAccessToken);
        Workspace workspace = getWorkspace(workspaceName, problems);
        if (workspace != null) {
            Project project = getProject(workspace, projectName, problems);
            if (project != null) {
                List<Task> projectTasks = getProjectTasks(project, problems);
                HashMap<String, Task> taskMap = null;
                if (projectTasks != null) {
                    taskMap = new HashMap<String, Task>();
                    for (Task temp : projectTasks) {
                        taskMap.put(temp.name, temp);
                    }
                }
                HashMap<String, Section> sectionMap = getSectionMap(project.gid, problems);
                for (TaskData taskData : tasks) {
                    // the only requirement for tasks is that they must have a name
                    if (taskData.getName() != null) {
                        if (taskMap != null && taskMap.containsKey(taskData.getName())) {
                            Task existingTask = taskMap.get(taskData.getName());
                            taskData.setAsanaData(existingTask);
                            updateTask(project, taskData, existingTask, sectionMap, problems);
                        } else {
                            insertTask(workspace, project, taskData, sectionMap, problems);
                        }
                    } else {
                        problems.addWarning(Problems.WARNING_TASKDATA_MISSING_NAME, null);

                    }
                }
            }
        }
    }

    /**
     * @param workspaceName
     * @param problems
     * @return Workspace
     */
    Workspace getWorkspace(String workspaceName, Problems problems) {
        Workspace workspace = null;
        try {
            List<Workspace> workspaces = client.workspaces.getWorkspaces()
                    .option("pretty", true)
                    .execute();
            for (Workspace temp : workspaces) {
                if (temp.name.equals(workspaceName)) {
                    workspace = temp;
                    break;
                }
            }
        } catch (Exception e) {
            problems.addError(Problems.ERROR_FROM_ASANA, e.toString());
        }
        if (workspace == null) {
            problems.addError(Problems.ERROR_NO_WORKSPACE, "Workspace = " + workspaceName);
        }
        return workspace;
    }

    /**
     * @param workspace
     * @param projectName
     * @param problems
     * @return Project
     */
    Project getProject(Workspace workspace, String projectName, Problems problems) {
        Project project = null;
        try {
            List<Project> projects = client.projects.getProjects(false, null, workspace.gid)
                    .option("pretty", true)
                    .execute();
            for (Project temp : projects) {
                if (temp.name.equals(projectName)) {
                    project = temp;
                    break;
                }
            }
        } catch (Exception e) {
            problems.addError(Problems.ERROR_FROM_ASANA, e.toString());
        }
        if (project == null) {
            problems.addError(Problems.ERROR_PROJECT_NOT_IN_WORKSPACE, "Workspace = " + workspace.name
                    + " Project = " + projectName);
        }
        return project;
    }

    /**
     * @param project
     * @param problems
     * @return List<Task>
     */
    List<Task> getProjectTasks(Project project, Problems problems) {
        List<Task> projectTasks = null;
        try {
            // get the existing tasks from Asana
            // result.nextPage.offset is offsset for pagination
            CollectionRequest<Task> request = client.tasks.getTasksForProject(project.gid, null)
                    .option("pretty", true);
            ResultBodyCollection<Task> result = request.executeRaw();
            projectTasks = result.data;
            if (result.nextPage != null) {
                // need to handle pagination if nextPage is not null
            }
        } catch (Exception e) {
            problems.addError(Problems.ERROR_FROM_ASANA, e.toString());
        }
        return projectTasks;
    }

    /**
     * @param sectionName
     * @param sectionMap
     * @param projectGid
     * @param problems
     * @return Section
     */
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
                            .data("name", sectionName)
                            .option("pretty", true)
                            .execute();
                    sectionMap.put(sectionName, newSection);
                    section = newSection;
                }
            }
        } catch (Exception e) {
            problems.addError(Problems.ERROR_FROM_ASANA, e.toString());
        }
        return section;
    }

    /**
     * @param projectGid
     * @param problems
     * @return HashMap<String, Section>
     */
    HashMap<String, Section> getSectionMap(String projectGid, Problems problems) {
        // Create map of sections since we don't want to create new ones if they already
        // exist.
        HashMap<String, Section> sectionMap = new HashMap<String, Section>();
        try {
            List<Section> sections = client.sections.getSectionsForProject(projectGid)
                    .option("pretty", true)
                    .execute();
            for (Section section : sections) {
                sectionMap.put(section.name, section);
            }
        } catch (Exception e) {
            problems.addError(Problems.ERROR_FROM_ASANA, e.toString());
        }
        return sectionMap;
    }

    /**
     * @param project
     * @param taskData
     * @param existingTask
     * @param sectionMap
     * @param problems
     */
    void updateTask(Project project, TaskData taskData, Task existingTask, Map<String, Section> sectionMap,
            Problems problems) {
        try {
            // TODO - add description
            if (taskData.getDueDate() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd",
                        Locale.ENGLISH);
                Task updateTask = client.tasks.updateTask(existingTask.gid)
                        .data("due_on", taskData.getDueDate().format(formatter))
                        .option("pretty", true)
                        .execute();
                taskData.setAsanaData(updateTask);
            }
            // adding a task to a section will remove it from the section it is already
            // in, thus add and update are the same action
            Section section = getOrCreateSectionGid(taskData.getSection(), sectionMap, project.gid, problems);
            if (section != null) {
                JsonElement addTaskToSectionResult = client.sections.addTaskForSection(section.gid)
                        .data("task", existingTask.gid)
                        .option("pretty", true)
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
        }
    }

    /**
     * @param workspace
     * @param project
     * @param taskData
     * @param sectionMap
     * @param problems
     */
    void insertTask(Workspace workspace, Project project, TaskData taskData, Map<String, Section> sectionMap,
            Problems problems) {
        try {
            // create the task
            // TODO - need to check that due_on isn't null
            // TODO - need to add description
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd",
                    Locale.ENGLISH);
            ItemRequest<Task> newTaskRequest = client.tasks.createTask()
                    .data("name", taskData.getName())
                    .data("workspace", workspace.gid)
                    .option("pretty", true);
            if (taskData.getDueDate() != null) {
                newTaskRequest = newTaskRequest.data("due_on", taskData.getDueDate().format(formatter));
            }
            ResultBody<Task> newTaskResult = newTaskRequest.executeRaw();
            Task newTask = newTaskResult.data;
            taskData.setAsanaData(newTask);
            Section section = getOrCreateSectionGid(taskData.getSection(), sectionMap, project.gid, problems);
            // set the project
            ItemRequest<JsonElement> requestAddProjectForTask = client.tasks.addProjectForTask(newTask.gid)
                    .data("project", project.gid)
                    .option("pretty", true);
            // determine if there is a section and if so add it as part of this call
            if (section != null) {
                // add section to the task
                requestAddProjectForTask = requestAddProjectForTask.data("section", section.gid);
                taskData.setAsanaSection(section.name);
            }
            JsonElement json = requestAddProjectForTask.execute();
            // subtasks are separate calls
        } catch (Exception e) {
            problems.addError(Problems.ERROR_FROM_ASANA, e.toString());
        }
    }

    /**
     * @param tasks
     */
    void deleteTasks(List<TaskData> tasks) {
        for (TaskData task : tasks) {
            // to account for negative tests we should only delete tasks that are sure to
            // exist in Asana which is indicated by task.getAsanaData() != null
            if (task.getAsanaData() != null) {
                try {
                    JsonElement result = client.tasks.deleteTask(task.getAsanaData().gid)
                            .option("pretty", true)
                            .execute();
                    LOGGER.debug("deleted task " + task.getName() + "  " + result.toString());
                } catch (Exception e) {
                    LOGGER.debug(e.toString());
                }
            }
        }
    }
}
