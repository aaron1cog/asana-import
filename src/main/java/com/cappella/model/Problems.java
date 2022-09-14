package com.cappella.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.cappella.csv.CsvClient;

public class Problems {

    public static final String ERROR_PARSING_CSV_FILE = "There was an error parsing the CSV file.";
    public static final String ERROR_MISSING_GRANT_TASK_NAME_HEADER = "The header " + CsvClient.GRANT_HEADER_TASK_NAME
            + " is missing from the CSV file.";
    public static final String ERROR_MISSING_GRANT_TASK_NAME = "The name of the task must not be null or empty. " +
            " Verify that the column  " + CsvClient.GRANT_HEADER_TASK_NAME +
            " is populated for each and every row.";
    public static final String ERROR_PARSING_TASK_DUE_DATE = "There was a problem parsing the date for the grant task.  The expected format is 3 letter month abbreviation day of month comma 4 digit year.  Example Jan 1, 2001.";
    public static final String ERROR_PARSING_SUB_TASK_DUE_DATE = "There was a problem parsing the date for the sub task.  The expected format is <type of date>-MM/dd/yyyy where <type of date> is like 'Milestone', 'Reporting', 'Draft'";
    public static final String ERROR_FROM_ASANA = "Exception from connecting to Asana";
    public static final String ERROR_PROJECT_NOT_IN_WORKSPACE = "The project does not exist in the workspace.";
    public static final String ERROR_NO_WORKSPACE = "The workspace does not exist.";
    public static final String WARNING_TASKDATA_MISSING_NAME = "Warning - task data missing a name so it was skipped";

    private final Map<String, String> errors;
    private final Map<String, String> warnings;

    public Problems() {
        errors = new HashMap<>();
        warnings = new HashMap<>();
    }

    public void addError(String key, String value) {
        errors.put(key, value);
    }

    public void addWarning(String key, String value) {
        warnings.put(key, value);
    }

    public Map<String, String> getErrors() {
        return Collections.unmodifiableMap(errors);
    }

    public Map<String, String> getWarnings() {
        return Collections.unmodifiableMap(warnings);
    }

    public void clear(){
        errors.clear();
        warnings.clear();
    }
}
