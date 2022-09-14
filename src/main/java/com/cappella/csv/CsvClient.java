package com.cappella.csv;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cappella.model.Problems;
import com.cappella.model.SubTask;
import com.cappella.model.TaskData;

@Service
/**
 * This class parses CSV files from Instrumentl to be loaded into Asana.
 * The CSV file must have a first record/row of headers.
 * The header "Opportunity name" (GRANT_HEADER_TASK_NAME) must exist.
 * "Opportunity name" becomes the name of the task in Asana.
 * "Funder Full proposal deadline" is the due date for the task if it exists.
 * "Status" is the section for the task if it exists.
 * "Next task description" is a subtask for the task if it exists.
 * "Next task deadline" is the due date for the subtask if it exists.
 * TODO - need to determine if what fields will become the description for the task
 */
public class CsvClient {

    private static final String TYPE = "text/csv";

    public static final String GRANT_HEADER_TASK_NAME = "Opportunity name"; // used in Problems
    private static final String GRANT_HEADER_DUE_DATE = "Funder Full proposal deadline";
    private static final String GRANT_HEADER_SECTION = "Status";
    private static final String GRANT_HEADER_SUBTASK_NAME = "Next task description";
    private static final String GRANT_HEADER_SUBTASK_DUE_DATE = "Next task deadline";

    static final DateTimeFormatter TASK_DATE_FORMATTER = DateTimeFormatter.ofPattern("LLL d, yyyy", Locale.ENGLISH);
    static final DateTimeFormatter SUB_TASK_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH);

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    /**
     * @param file
     * @return boolean
     */
    public boolean hasCSVFormat(MultipartFile file) {
        return TYPE.equals(file.getContentType());
    }

    /**
     * CSV file must have a first record/row of headers.
     * The header "Opportunity name" (GRANT_HEADER_TASK_NAME) must exist.
     * List will be null if there are problems parsing the header.
     * List will empty if the header is present but no valid rows are parsed.
     * Any errors encountered will be contained in the Problems.
     * 
     * @param is
     * @param problems
     * @return List<TaskData>
     */
    public List<TaskData> parseGrantCsvToTasks(InputStream is, Problems problems) {
        List<TaskData> tasks = null;
        try {
            BufferedReader fileReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim());
            Iterable<CSVRecord> csvRecords = csvParser.getRecords();
            // ensure there is at least the header used for the task name
            if (!csvParser.getHeaderNames().contains(GRANT_HEADER_TASK_NAME)) {
                // log error that the task name header does not exist in the csv file
                problems.addError(Problems.ERROR_MISSING_GRANT_TASK_NAME_HEADER, null);
            } else {
                tasks = new ArrayList<>();
                for (CSVRecord csvRecord : csvRecords) {
                    TaskData task = parseTask(csvRecord, problems);
                    if (task != null) {
                        tasks.add(task);
                    }
                }
            }
        } catch (Exception e) {
            // handle all exceptions gracefully log error
            problems.addError(Problems.ERROR_PARSING_CSV_FILE, e.toString());
        }
        return tasks;
    }

    /**
     * Will return a null TaskData if the CSVRecord does not have a task.
     * Any errors encountered will be contained in the Problems.
     * 
     * @param csvRecord
     * @param problems
     * @return
     */
    TaskData parseTask(CSVRecord csvRecord, Problems problems) {
        TaskData task = null;
        String taskName = getCsvRecordString(csvRecord, GRANT_HEADER_TASK_NAME, problems);
        // if the task does not have a name it is not valid so log error and return a
        // null TaskData
        if (taskName != null && !taskName.isEmpty()) {
            task = new TaskData();
            task.setName(taskName);
            String dueDate = getCsvRecordString(csvRecord, GRANT_HEADER_DUE_DATE, problems);
            if (dueDate != null) {
                task.setDueDate(parseGrantDueDate(dueDate, problems));
            }
            task.setSection(getCsvRecordString(csvRecord, GRANT_HEADER_SECTION, problems));
            SubTask subTask = parseSubTask(csvRecord, problems);
            if (subTask != null) {
                task.addSubTask(subTask);
            }
            // TODO set description of the task
            // do we append some or all other columns to the description?
        } else {
            problems.addError(Problems.ERROR_MISSING_GRANT_TASK_NAME, null);
        }
        return task;
    }

    /**
     * Will return a null String if the CVSRecord does not have a value for the
     * String header passed in. Any errors encountered will be contained in the
     * Problems.
     * 
     * @param csvRecord
     * @param header
     * @param problems
     * @return String
     */
    String getCsvRecordString(CSVRecord csvRecord, String header, Problems problems) {
        String value = null;
        if (csvRecord.isMapped(header)) {
            try {
                value = csvRecord.get(header);
            } catch (IllegalArgumentException e) {
                // log that record is inconsistent
                problems.addError(Problems.ERROR_PARSING_CSV_FILE, e.toString());
            }
        }
        return value;
    }

    /**
     * Will return a null LocalDate if the String passed in does not contain a
     * valid date in the format 3 character month, 1 or 2 digit day of the month,
     * and 4 digit year ("LLL d, yyyy"). Any errors encountered parsing the String
     * will be
     * contained in the Problems.
     * 
     * @param value
     * @param problems
     * @return LocalDate
     */
    LocalDate parseGrantDueDate(String value, Problems problems) {
        LocalDate dueDate = null;
        if (!value.isEmpty()) {
            try { // if there are date parse errors catch them and keep processing
                dueDate = LocalDate.parse(value, TASK_DATE_FORMATTER);
            } catch (Exception e) {
                problems.addError(Problems.ERROR_PARSING_TASK_DUE_DATE, e.toString());
            }
        }
        return dueDate;
    }

    /**
     * Will return a null SubTask if the CSVRecord does not contain a subtask
     * or subtask name is null or an empty string.
     * 
     * @param task
     * @param csvRecord
     * @param problems
     */
    SubTask parseSubTask(CSVRecord csvRecord, Problems problems) {
        SubTask subTask = null;
        String subTaskName = getCsvRecordString(csvRecord, GRANT_HEADER_SUBTASK_NAME, problems);
        if (subTaskName != null && !subTaskName.isEmpty()) {
            subTask = new SubTask();
            subTask.setName(subTaskName);
            String subTaskDueDate = getCsvRecordString(csvRecord, GRANT_HEADER_SUBTASK_DUE_DATE, problems);
            if (subTaskDueDate != null) {
                parseSubTaskDueDate(subTask, subTaskDueDate, problems);
            }
        }
        return subTask;
    }

    /**
     * The pattern for this date is <type of date>-MM/dd/yyyy where <type of date>
     * is a String similar to 'Milestone', 'Reporting', 'Draft'.
     * This method will pull the string away from date and append string to the
     * sub task name and parse the date. Since both the subtask name and subtask
     * due date need to be manipulated this method takes the subtask object and
     * updates both parts of the subtask. The subTask passed in must not be null
     * and the name of the subtask must not be null.
     * 
     * @param subTask
     * @param value
     * @param problems
     */
    void parseSubTaskDueDate(SubTask subTask, String value, Problems problems) {
        LocalDate dueDate = null;
        if (!value.isEmpty()) {
            try { // if there are date parse errors catch them and keep processing
                String[] splitStrings = value.split("-");
                subTask.setName(subTask.getName().concat(" " + splitStrings[0]));
                dueDate = LocalDate.parse(splitStrings[1], SUB_TASK_DATE_FORMATTER);
                // set subtask due date
                subTask.setDueDate(dueDate);
            } catch (Exception e) {
                problems.addError(Problems.ERROR_PARSING_SUB_TASK_DUE_DATE, e.toString());
            }
        }
    }
}
