package com.cappella.csv;

import java.beans.Transient;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.csv.CSVRecord;
import com.cappella.model.Problems;
import com.cappella.model.TaskData;
import com.cappella.model.SubTask;

class CsvClientTests {

        private final Logger LOGGER = LoggerFactory.getLogger(getClass());

        private final Problems problems;
        private final CsvClient csv;

        public CsvClientTests(){
                this.problems = new Problems();
                this.csv = new CsvClient();
        }

        @AfterEach
        private void afterTest(){
                this.problems.clear();
        }

        @Test
        void testNoHeadersCsvFile() {
                List<TaskData> grantTasks = parseCsvFile("noHeaders.csv");
                Assertions.assertTrue(this.problems.getErrors().containsKey(Problems.ERROR_PARSING_CSV_FILE));
                Assertions.assertNull(grantTasks);
        }

        @Test
        void testMalformedHeadersCsvFile() {
                // the simplest way to malform the header is to add a trailing comma
                List<TaskData> grantTasks = parseCsvFile("malformedHeader.csv");
                Assertions.assertTrue(this.problems.getErrors().containsKey(Problems.ERROR_PARSING_CSV_FILE));
                Assertions.assertNull(grantTasks);
        }

        @Test
        void testMissingNameHeaderCsvFile() {
                List<TaskData> grantTasks = parseCsvFile("missingHeaderTaskName.csv");
                Assertions.assertTrue(this.problems.getErrors().containsKey(Problems.ERROR_MISSING_GRANT_TASK_NAME_HEADER));
                Assertions.assertNull(grantTasks);
        }

        @Test
        void testMalformedTaskDueDateCsvFile() {
                List<TaskData> grantTasks = parseCsvFile("malformedTaskDueDate.csv");
                Assertions.assertTrue(this.problems.getErrors().containsKey(Problems.ERROR_PARSING_TASK_DUE_DATE));
        }

        @Test
        void testMalformedSubTaskDueDateCsvFile() {
                List<TaskData> grantTasks = parseCsvFile("malformedSubTaskDueDate.csv");
                Assertions.assertTrue(this.problems.getErrors().containsKey(Problems.ERROR_PARSING_SUB_TASK_DUE_DATE));
        }

        @Test
        void testNoTasksCsvFile() {
                // if there is a valid header line (containing the header for task name)
                // but no subsequent rows it will still return an empty list
                List<TaskData> grantTasks = parseCsvFile("noTaskRows.csv");
                Assertions.assertTrue(this.problems.getErrors().isEmpty());
                Assertions.assertTrue(grantTasks.isEmpty());
        }

        @Test
        void testNoTaskNamesCsvFile() {
                // simplest way to have rows with no task names is empty rows
                List<TaskData> grantTasks = parseCsvFile("noTaskNames.csv");
                Assertions.assertTrue(this.problems.getErrors().isEmpty());
                Assertions.assertTrue(grantTasks.isEmpty());
        }

        @Test
        void testSimpleCsvFile() {
                List<TaskData> grantTasks = parseCsvFile("simple.csv");
                Assertions.assertTrue(problems.getErrors().isEmpty());
                // should also assert that it successfully parsed the grant tasks expected
        }

        private List<TaskData> parseCsvFile(String filename){
                InputStream is = this.getClass().getClassLoader().getResourceAsStream(filename);
                List<TaskData> grantTasks = this.csv.parseGrantCsvToTasks(is, this.problems);
                return grantTasks;
        }

        @Test
        void testParseGrantDueDateWrongFormat() {
                String dateValue = "1/1/2001";
                LocalDate dueDate = this.csv.parseGrantDueDate(dateValue, problems);
                Assertions.assertNull(dueDate);
                Assertions.assertTrue(this.problems.getErrors().containsKey(Problems.ERROR_PARSING_TASK_DUE_DATE));
        }

        @Test
        void testParseGrantDueDate() {
                String dateValue = "Jan 1, 2001";
                LocalDate date = LocalDate.parse(dateValue, CsvClient.TASK_DATE_FORMATTER);
                LocalDate dueDate = this.csv.parseGrantDueDate(dateValue, problems);
                Assertions.assertEquals(date, dueDate);
                Assertions.assertTrue(this.problems.getErrors().isEmpty());
        }

        @Test
        void testParseSubTaskDueDateWrongFormat() {
                String name = "SubTask Name";
                String dateString = "123456";
                String datePrefix = "Milestone";
                String dateSeparator = "-";
                String fullDate = datePrefix.concat(dateSeparator).concat(dateString);
                SubTask subTask = new SubTask();
                subTask.setName(name);
                this.csv.parseSubTaskDueDate(subTask, fullDate, problems);
                String expectedSubTaskName = name.concat(" ").concat(datePrefix);
                Assertions.assertTrue(expectedSubTaskName.equals(subTask.getName()));
                Assertions.assertNull(subTask.getDueDate());
                Assertions.assertTrue(this.problems.getErrors().containsKey(Problems.ERROR_PARSING_SUB_TASK_DUE_DATE));
        }

        @Test
        void testParseSubTaskDueDate() {
                String name = "SubTask Name";
                String dateString = "01/21/2001";
                String datePrefix = "Milestone";
                String dateSeparator = "-";
                String fullDate = datePrefix.concat(dateSeparator).concat(dateString);
                SubTask subTask = new SubTask();
                subTask.setName(name);
                this.csv.parseSubTaskDueDate(subTask, fullDate, problems);
                String expectedSubTaskName = name.concat(" ").concat(datePrefix);
                LocalDate dueDate = LocalDate.parse(dateString, CsvClient.SUB_TASK_DATE_FORMATTER);
                Assertions.assertTrue(expectedSubTaskName.equals(subTask.getName()));
                Assertions.assertTrue(dueDate.equals(subTask.getDueDate()));
                Assertions.assertTrue(this.problems.getErrors().isEmpty());
        }

}
