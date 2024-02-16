package gov.cabinetoffice.gap.service;

import org.junit.jupiter.api.Test;
import org.odftoolkit.odfdom.doc.OdfDocument;
import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

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

    private String docToString(Document document) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.getBuffer().toString();
    }
}