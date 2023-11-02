package gov.cabinetoffice.gap.service;

import com.fasterxml.jackson.databind.JsonMappingException;
import gov.cabinetoffice.gap.model.Submission;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static gov.cabinetoffice.gap.testData.SubmissionTestData.SINGLE_EMPTY_SECTION_OBJ;
import static gov.cabinetoffice.gap.testData.SubmissionTestData.SUBMISSION_SINGLE_EMPTY_SECTION_ARRAY_JSON_STRING;
import static gov.cabinetoffice.gap.testData.SubmissionTestData.SUBMISSION_SINGLE_SECTION_AS_OBJECT_JSON_STRING;
import static gov.cabinetoffice.gap.testData.SubmissionTestData.V1_SUBMISSION_WITHOUT_SECTIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SubmissionServiceTest {

    @Nested
    class addSubmissionSectionsJsonToSubmissionModel {

        @Test
        void successfullyParseAndAddSubmissionSections() throws Exception {

            Submission submissionToTest = V1_SUBMISSION_WITHOUT_SECTIONS;

            SubmissionService.addSubmissionSectionsJsonToSubmissionModel(submissionToTest, SUBMISSION_SINGLE_EMPTY_SECTION_ARRAY_JSON_STRING);

            assertEquals(submissionToTest.getSections(), SINGLE_EMPTY_SECTION_OBJ);
        }

        @Test
        void shouldFailsWhenSectionsNotProvidedAsAnArray() throws Exception {

            Submission submissionToTest = V1_SUBMISSION_WITHOUT_SECTIONS;

            assertThrows(JsonMappingException.class, () -> SubmissionService.addSubmissionSectionsJsonToSubmissionModel(submissionToTest, SUBMISSION_SINGLE_SECTION_AS_OBJECT_JSON_STRING));
        }
    }
}
