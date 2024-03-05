package gov.cabinetoffice.gap.testData;

import gov.cabinetoffice.gap.enums.SubmissionSectionStatus;
import gov.cabinetoffice.gap.model.Submission;
import gov.cabinetoffice.gap.model.SubmissionQuestion;
import gov.cabinetoffice.gap.model.SubmissionSection;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

public class SubmissionTestData {

    public static final String SCHEME_ID = "1";

    public static final String SCHEME_NAME = "Test Scheme";

    public static final String LEGAL_NAME = "Test Org Name";

    public static final String GAP_ID = "GAP-LL-20220927-00001";

    public static final ZonedDateTime SUBMITTED_DATE = ZonedDateTime.now();

    public static final String EMAIL = "testEmailAddress";

    public static final Submission V1_SUBMISSION_WITHOUT_SECTIONS = new Submission(SCHEME_ID, SCHEME_NAME, LEGAL_NAME,
            GAP_ID, SUBMITTED_DATE, null, EMAIL, 1, true);

    public static final String SUBMISSION_SINGLE_EMPTY_SECTION_ARRAY_JSON_STRING = """
            [    {
                  "schemeId": "1",
                  "sectionId": "8dee2a3b-e19f-4d2b-8ca2-2581b5d1824d",
                  "sectionTitle": "Custom Section",
                  "sectionStatus": "NOT_STARTED",
                  "questions": []
                }
              ]""";

    public static final String SUBMISSION_SINGLE_SECTION_AS_OBJECT_JSON_STRING = """
            {
              "schemeId": "1",
              "sectionId": "8dee2a3b-e19f-4d2b-8ca2-2581b5d1824d",
              "sectionTitle": "Custom Section",
              "sectionStatus": "NOT_STARTED",
              "questions": []
            }
            """;

    public static final List<SubmissionSection> SINGLE_EMPTY_SECTION_OBJ = Collections
            .singletonList(new SubmissionSection("8dee2a3b-e19f-4d2b-8ca2-2581b5d1824d", "Custom Section",
                    SubmissionSectionStatus.NOT_STARTED, Collections.emptyList()));

    public static final List<SubmissionSection> ESSENTIAL_INFO_SECTION = Collections
            .singletonList(new SubmissionSection("ESSENTIAL", "essential", SubmissionSectionStatus.NOT_STARTED,
                    Collections.singletonList(SubmissionQuestion.builder().questionId("APPLICANT_ORG_NAME")
                            .response("test org name").build())));

    public static final Submission V1_SUBMISSION_WITH_ESSENTIAL_SECTION = new Submission(SCHEME_ID, SCHEME_NAME,
            LEGAL_NAME, GAP_ID, SUBMITTED_DATE, ESSENTIAL_INFO_SECTION, EMAIL, 1, true);

}
