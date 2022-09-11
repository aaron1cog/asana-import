package com.cappella.csv;

import java.beans.Transient;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.csv.CSVRecord;
import com.cappella.model.Problems;
import com.cappella.model.TaskData;
import com.cappella.model.SubTask;

class CsvClientTests {

        final Logger LOGGER = LoggerFactory.getLogger(getClass());

        @Test
        void testNoHeadersCsvFile() {
                // the simplest way to malform the header is to add a trailing comma
                ClassLoader classloader = this.getClass().getClassLoader();
                InputStream is = classloader.getResourceAsStream("noHeaders.csv");
                Problems problems = new Problems();
                CsvClient csv = new CsvClient();
                List<TaskData> grantTasks = csv.parseGrantCsvToTasks(is, problems);
                Assertions.assertTrue(problems.getErrors().containsKey(Problems.ERROR_PARSING_CSV_FILE));
        }

        @Test
        void testMalformedHeadersCsvFile() {
                // the simplest way to malform the header is to add a trailing comma
                ClassLoader classloader = this.getClass().getClassLoader();
                InputStream is = classloader.getResourceAsStream("malformedHeader.csv");
                Problems problems = new Problems();
                CsvClient csv = new CsvClient();
                List<TaskData> grantTasks = csv.parseGrantCsvToTasks(is, problems);
                Assertions.assertTrue(problems.getErrors().containsKey(Problems.ERROR_PARSING_CSV_FILE));
        }

        @Test
        void testMalformedDataCsvFile() {
        }

        @Test
        void testMissingNameHeaderCsvFile() {
                ClassLoader classloader = this.getClass().getClassLoader();
                InputStream is = classloader.getResourceAsStream("missingHeaderTaskName.csv");
                Problems problems = new Problems();
                CsvClient csv = new CsvClient();
                List<TaskData> grantTasks = csv.parseGrantCsvToTasks(is, problems);
                Assertions.assertTrue(problems.getErrors().containsKey(Problems.ERROR_MISSING_GRANT_TASK_NAME_HEADER));

        }

        @Test
        void testMalformedTaskDueDateCsvFile() {
                ClassLoader classloader = this.getClass().getClassLoader();
                InputStream is = classloader.getResourceAsStream("malformedTaskDueDate.csv");
                Problems problems = new Problems();
                CsvClient csv = new CsvClient();
                List<TaskData> grantTasks = csv.parseGrantCsvToTasks(is, problems);
                Assertions.assertTrue(problems.getErrors().containsKey(Problems.ERROR_PARSING_TASK_DUE_DATE));
        }

        @Test
        void testMalformedSubTaskDueDateCsvFile() {
                ClassLoader classloader = this.getClass().getClassLoader();
                InputStream is = classloader.getResourceAsStream("malformedSubTaskDueDate.csv");
                Problems problems = new Problems();
                CsvClient csv = new CsvClient();
                List<TaskData> grantTasks = csv.parseGrantCsvToTasks(is, problems);
                Assertions.assertTrue(problems.getErrors().containsKey(Problems.ERROR_PARSING_SUB_TASK_DUE_DATE));
        }

        @Test
        void testSpecialCharactersCsvFile() {
        }

        @Test
        void testOnlyNameCsvFile() {
        }

        @Test
        void testSimpleCsvFile() {
                ClassLoader classloader = this.getClass().getClassLoader();
                InputStream is = classloader.getResourceAsStream("simple.csv");
                Problems problems = new Problems();
                CsvClient csv = new CsvClient();
                List<TaskData> grantTasks = csv.parseGrantCsvToTasks(is, problems);
                Assertions.assertTrue(problems.getErrors().isEmpty());
                // should also assert that it successfully parsed the grant tasks expected
        }

        @Test
        void testProductionCsvFile() {
        }

        @Test
        void testParseGrantDueDateWrongFormat() {
                Problems problems = new Problems();
                CsvClient csv = new CsvClient();
                String dateValue = "1/1/2001";
                LocalDate dueDate = csv.parseGrantDueDate(dateValue, problems);
                Assertions.assertNull(dueDate);
                Assertions.assertTrue(problems.getErrors().containsKey(Problems.ERROR_PARSING_TASK_DUE_DATE));
        }

        @Test
        void testParseGrantDueDate() {
                Problems problems = new Problems();
                CsvClient csv = new CsvClient();
                String dateValue = "Jan 1, 2001";
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("LLL d, yyyy", Locale.ENGLISH);
                LocalDate date = LocalDate.parse(dateValue, formatter);
                LocalDate dueDate = csv.parseGrantDueDate(dateValue, problems);
                Assertions.assertTrue(date.equals(dueDate));
                Assertions.assertTrue(problems.getErrors().isEmpty());
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
                Problems problems = new Problems();
                CsvClient csv = new CsvClient();
                csv.parseSubTaskDueDate(subTask, fullDate, problems);
                String expectedSubTaskName = name.concat(" ").concat(datePrefix);
                Assertions.assertTrue(expectedSubTaskName.equals(subTask.getName()));
                Assertions.assertNull(subTask.getDueDate());
                Assertions.assertTrue(problems.getErrors().containsKey(Problems.ERROR_PARSING_SUB_TASK_DUE_DATE));
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
                Problems problems = new Problems();
                CsvClient csv = new CsvClient();
                csv.parseSubTaskDueDate(subTask, fullDate, problems);
                String expectedSubTaskName = name.concat(" ").concat(datePrefix);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH);
                LocalDate dueDate = LocalDate.parse(dateString, formatter);
                Assertions.assertTrue(expectedSubTaskName.equals(subTask.getName()));
                Assertions.assertTrue(dueDate.equals(subTask.getDueDate()));
                Assertions.assertTrue(problems.getErrors().isEmpty());
        }

}
