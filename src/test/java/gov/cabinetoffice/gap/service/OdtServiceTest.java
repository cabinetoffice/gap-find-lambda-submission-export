package gov.cabinetoffice.gap.service;

import gov.cabinetoffice.gap.enums.ResponseTypeEnum;
import gov.cabinetoffice.gap.enums.SubmissionSectionStatus;
import gov.cabinetoffice.gap.model.Submission;
import gov.cabinetoffice.gap.model.SubmissionQuestion;
import gov.cabinetoffice.gap.model.SubmissionSection;
import org.junit.jupiter.api.Test;
import org.odftoolkit.odfdom.doc.OdfDocument;
import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static gov.cabinetoffice.gap.testdata.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;


class OdtServiceTest {

    @Test
    void compareTestGenerateSingleOdtForSchemeVersion1() throws Exception {
        OdtService.generateSingleOdt(V1_SUBMISSION, "testFileName");
        final String generatedContent;
        try (OdfDocument generatedDoc = OdfDocument.loadDocument("/tmp/testFileName.odt")) {
            generatedContent = docToString(generatedDoc.getContentDom());
        }

        assertThat(generatedContent).contains("Eligibility");
        assertThat(generatedContent).contains("V1_Company name");
        assertThat(generatedContent).contains("V1_9-10 St Andrew Square");
        assertThat(generatedContent).contains("V1_Edinburgh");
        assertThat(generatedContent).contains("V1_EH2 2AF");
        assertThat(generatedContent).contains("V1_Limited company");
        assertThat(generatedContent).contains("V1_CHN");
        assertThat(generatedContent).contains("V1_12738494");
        assertThat(generatedContent).contains("V1_Scotland", "V1_North East England");
        assertThat(generatedContent).contains("My Custom Section");
        assertThat(generatedContent).doesNotContain("V2_Company name");
        assertThat(generatedContent).doesNotContain("V2_9-10 St Andrew Square");
        assertThat(generatedContent).doesNotContain("V2_Edinburgh");
        assertThat(generatedContent).doesNotContain("V2_EH2 2AF");
        assertThat(generatedContent).doesNotContain("V2_Limited company");
        assertThat(generatedContent).doesNotContain("V2_CHN");
        assertThat(generatedContent).doesNotContain("V2_12738494");
        assertThat(generatedContent).doesNotContain("V2_Scotland", "V2_North East England");
    }

    @Test
    void compareTestGenerateSingleOdtForLimitedCompanyWithCCAndCHForSchemeVersion2() throws Exception {
        OdtService.generateSingleOdt(V2_SUBMISSION_LIMITED_COMPANY_WITH_CC_AND_CH, "testFileName2");
        final String generatedContent;
        try (OdfDocument generatedDoc = OdfDocument.loadDocument("/tmp/testFileName2.odt")) {
            generatedContent = docToString(generatedDoc.getContentDom());
        }

        assertThat(generatedContent).contains("Test Org Name v2");
        assertThat(generatedContent).contains("Eligibility");
        assertThat(generatedContent).contains("V2_Company name");
        assertThat(generatedContent).contains("V2_9-10 St Andrew Square");
        assertThat(generatedContent).contains("V2_Edinburgh");
        assertThat(generatedContent).contains("V2_EH2 2AF");
        assertThat(generatedContent).contains("V2_Limited company");
        assertThat(generatedContent).contains("Companies House number if the organisation has one (if blank, number has not been entered)");
        assertThat(generatedContent).contains("V2_CHN");
        assertThat(generatedContent).contains("Charities Commission number if the organisation has one (if blank, number has not been entered)");
        assertThat(generatedContent).contains("V2_12738494");
        assertThat(generatedContent).contains("V2_Scotland", "V2_North East England");
        assertThat(generatedContent).doesNotContain("V1_9-10 St Andrew Square");
        assertThat(generatedContent).doesNotContain("V1_Edinburgh");
        assertThat(generatedContent).doesNotContain("V1_EH2 2AF");
        assertThat(generatedContent).doesNotContain("V1_Limited company");
        assertThat(generatedContent).doesNotContain("V1_CHN");
        assertThat(generatedContent).doesNotContain("V1_12738494");
        assertThat(generatedContent).doesNotContain("V1_Scotland", "V1_North East England");
        assertThat(generatedContent).doesNotContain("My Custom Section");
    }

    @Test
    void compareTestGenerateSingleOdtForLimitedCompanyWithoutCCAndCHForSchemeVersion2() throws Exception {
        OdtService.generateSingleOdt(V2_SUBMISSION_LIMITED_COMPANY_WITHOUT_CC_AND_CH, "testFileName3");
        final String generatedContent;
        try (OdfDocument generatedDoc = OdfDocument.loadDocument("/tmp/testFileName3.odt")) {
            generatedContent = docToString(generatedDoc.getContentDom());
        }

        assertThat(generatedContent).contains("Test Org Name v2");
        assertThat(generatedContent).contains("Eligibility");
        assertThat(generatedContent).contains("V2_Company name");
        assertThat(generatedContent).contains("V2_9-10 St Andrew Square");
        assertThat(generatedContent).contains("V2_Edinburgh");
        assertThat(generatedContent).contains("V2_EH2 2AF");
        assertThat(generatedContent).contains("V2_Limited company");
        assertThat(generatedContent).contains("Companies House number if the organisation has one (if blank, number has not been entered)");
        assertThat(generatedContent).doesNotContain("V2_CHN");
        assertThat(generatedContent).contains("Charities Commission number if the organisation has one (if blank, number has not been entered)");
        assertThat(generatedContent).doesNotContain("V2_12738494");
        assertThat(generatedContent).contains("V2_Scotland", "V2_North East England");
        assertThat(generatedContent).doesNotContain("V1_9-10 St Andrew Square");
        assertThat(generatedContent).doesNotContain("V1_Edinburgh");
        assertThat(generatedContent).doesNotContain("V1_EH2 2AF");
        assertThat(generatedContent).doesNotContain("V1_Limited company");
        assertThat(generatedContent).doesNotContain("V1_CHN");
        assertThat(generatedContent).doesNotContain("V1_12738494");
        assertThat(generatedContent).doesNotContain("V1_Scotland", "V1_North East England");
        assertThat(generatedContent).doesNotContain("My Custom Section");
    }

    @Test
    void compareTestGenerateSingleOdtForNonLimitedCompanyForSchemeVersion2() throws Exception {
        OdtService.generateSingleOdt(V2_SUBMISSION_NON_LIMITED_COMPANY, "testFileName4");
        final String generatedContent;
        try (OdfDocument generatedDoc = OdfDocument.loadDocument("/tmp/testFileName4.odt")) {
            generatedContent = docToString(generatedDoc.getContentDom());
        }

        assertThat(generatedContent).contains("Test Non Limited Company v2");
        assertThat(generatedContent).contains("Eligibility");
        assertThat(generatedContent).contains("V2_Company name");
        assertThat(generatedContent).contains("V2_9-10 St Andrew Square");
        assertThat(generatedContent).contains("V2_Edinburgh");
        assertThat(generatedContent).contains("V2_EH2 2AF");
        assertThat(generatedContent).contains("Non-limited company");
        assertThat(generatedContent).doesNotContain("Companies House number if the organisation has one (if blank, number has not been entered)");
        assertThat(generatedContent).doesNotContain("V2_CHN");
        assertThat(generatedContent).doesNotContain("Charities Commission number if the organisation has one (if blank, number has not been entered)");
        assertThat(generatedContent).doesNotContain("V2_12738494");
        assertThat(generatedContent).contains("V2_Scotland", "V2_North East England");
        assertThat(generatedContent).doesNotContain("V1_9-10 St Andrew Square");
        assertThat(generatedContent).doesNotContain("V1_Edinburgh");
        assertThat(generatedContent).doesNotContain("V1_EH2 2AF");
        assertThat(generatedContent).doesNotContain("V1_Limited company");
        assertThat(generatedContent).doesNotContain("V1_CHN");
        assertThat(generatedContent).doesNotContain("V1_12738494");
        assertThat(generatedContent).doesNotContain("V1_Scotland", "V1_North East England");
        assertThat(generatedContent).doesNotContain("My Custom Section");
    }

    @Test
    void compareTestGenerateSingleOdtForIndividualForSchemeVersion2() throws Exception {
        OdtService.generateSingleOdt(V2_SUBMISSION_INDIVIDUAL, "testFileName5");
        final String generatedContent;
        try (OdfDocument generatedDoc = OdfDocument.loadDocument("/tmp/testFileName5.odt")) {
            generatedContent = docToString(generatedDoc.getContentDom());
        }

        assertThat(generatedContent).contains("Test Individual v2");
        assertThat(generatedContent).contains("Eligibility");
        assertThat(generatedContent).contains("Applicant name");
        assertThat(generatedContent).contains("V2_Company name");
        assertThat(generatedContent).contains("V2_9-10 St Andrew Square");
        assertThat(generatedContent).contains("V2_Edinburgh");
        assertThat(generatedContent).contains("V2_EH2 2AF");
        assertThat(generatedContent).contains("I am applying as an individual");
        assertThat(generatedContent).doesNotContain("Companies House number if the organisation has one (if blank, number has not been entered)");
        assertThat(generatedContent).doesNotContain("V2_CHN");
        assertThat(generatedContent).doesNotContain("Charities Commission number if the organisation has one (if blank, number has not been entered)");
        assertThat(generatedContent).doesNotContain("V2_12738494");
        assertThat(generatedContent).contains("V2_Scotland", "V2_North East England");
        assertThat(generatedContent).doesNotContain("V1_9-10 St Andrew Square");
        assertThat(generatedContent).doesNotContain("V1_Edinburgh");
        assertThat(generatedContent).doesNotContain("V1_EH2 2AF");
        assertThat(generatedContent).doesNotContain("V1_Limited company");
        assertThat(generatedContent).doesNotContain("V1_CHN");
        assertThat(generatedContent).doesNotContain("V1_12738494");
        assertThat(generatedContent).doesNotContain("V1_Scotland", "V1_North East England");
        assertThat(generatedContent).doesNotContain("My Custom Section");
    }


    @Test
    void compareTestGenerateSingleOdtForLocalAuthoritiesForSchemeVersion2() throws Exception {
        final Submission submission = V2_SUBMISSION_LOCAL_AUTHORITY;
        OdtService.generateSingleOdt(submission, "testFileName5");
        final OdfDocument generatedDoc = OdfDocument.loadDocument("/tmp/testFileName5.odt");
        final String generatedContent = docToString(generatedDoc.getContentDom());

        assertThat(generatedContent).contains("Test Local authority");
        assertThat(generatedContent).contains("Eligibility");
        assertThat(generatedContent).contains("V2_Company name");
        assertThat(generatedContent).contains("V2_9-10 St Andrew Square");
        assertThat(generatedContent).contains("V2_Edinburgh");
        assertThat(generatedContent).contains("V2_EH2 2AF");
        assertThat(generatedContent).contains("Local authority");
        assertThat(generatedContent).doesNotContain("Companies House number if the organisation has one (if blank, number has not been entered)");
        assertThat(generatedContent).doesNotContain("V2_CHN");
        assertThat(generatedContent).doesNotContain("Charities Commission number if the organisation has one (if blank, number has not been entered)");
        assertThat(generatedContent).doesNotContain("V2_12738494");
        assertThat(generatedContent).contains("V2_Scotland", "V2_North East England");
        assertThat(generatedContent).doesNotContain("V1_9-10 St Andrew Square");
        assertThat(generatedContent).doesNotContain("V1_Edinburgh");
        assertThat(generatedContent).doesNotContain("V1_EH2 2AF");
        assertThat(generatedContent).doesNotContain("V1_Limited company");
        assertThat(generatedContent).doesNotContain("V1_CHN");
        assertThat(generatedContent).doesNotContain("V1_12738494");
        assertThat(generatedContent).doesNotContain("V1_Scotland", "V1_North East England");
        assertThat(generatedContent).doesNotContain("My Custom Section");
    }

    @Test
    void compareTestGenerateSingleOdtForOptionalCustomSectionQuestions() throws Exception {
        OdtService.generateSingleOdt(V2_SUBMISSION_WITH_CUSTOM_SECTION, "testFileName6");
        final String generatedContent;
        try (OdfDocument generatedDoc = OdfDocument.loadDocument("/tmp/testFileName6.odt")) {
            generatedContent = docToString(generatedDoc.getContentDom());
        }

        assertThat(generatedContent).contains("Custom section");
        assertThat(generatedContent).contains("Yes/No question");
        assertThat(generatedContent).contains("Date question");
        assertThat(generatedContent).contains("Multi select question");
        assertThat(generatedContent).contains("File upload question");
        assertThat(generatedContent).contains("Not provided");
    }

    @Test
    void generateSingleOdt_stripsUnsupportedCharactersFromSavedDocument() throws Exception {
        final SubmissionSection customSection = SubmissionSection.builder()
                .sectionId("26215de2-2211-48e4-9c82-835cd227daad")
                .sectionTitle("Project details \uD83D\uDE80")
                .sectionStatus(SubmissionSectionStatus.COMPLETED)
                .questions(List.of(SubmissionQuestion.builder()
                        .questionId("PROJECT_SUMMARY")
                        .fieldTitle("Describe your project")
                        .responseType(ResponseTypeEnum.LongAnswer)
                        .response("Our \uD83C\uDF08 project costs £1,000 – café \u2705")
                        .build()))
                .build();

        final Submission submission = Submission.builder()
                .legalName("Emoji Org \u2764\uFE0F")
                .schemeName("v2Scheme Name")
                .gapId("GAP-LL-20220927-00001")
                .submittedDate(ZonedDateTime.parse("2022-02-15T18:35:24.00Z"))
                .sections(List.of(
                        ELIGIBILITY_SECTION_SUBMISSION,
                        ORGANISATION_DETAILS_SECTION_SUBMISSION_NON_LIMITED,
                        FUNDING_DETAILS_SECTION_SUBMISSION,
                        customSection))
                .schemeVersion(2)
                .build();

        OdtService.generateSingleOdt(submission, "testFileName7");
        final String savedContent = readContentXml(Path.of("/tmp/testFileName7.odt"));

        assertThat(savedContent).doesNotContain("&#55356;", "&#55357;", "\uD83C\uDF08", "\uD83D\uDE80", "\u2705", "\uFE0F");
        assertThat(savedContent).contains("Project details ", "Our  project costs £1,000 – café ", "Emoji Org \u2764");
    }

    private String readContentXml(Path odt) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(odt))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals("content.xml")) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new IllegalStateException("content.xml not found in ODT");
    }

    private String docToString(Document document) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.getBuffer().toString();
    }
}