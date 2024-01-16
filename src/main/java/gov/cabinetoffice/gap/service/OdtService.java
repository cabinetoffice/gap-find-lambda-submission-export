package gov.cabinetoffice.gap.service;

import gov.cabinetoffice.gap.model.Submission;
import gov.cabinetoffice.gap.model.SubmissionQuestion;
import gov.cabinetoffice.gap.model.SubmissionSection;
import org.odftoolkit.odfdom.doc.OdfTextDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.dom.OdfContentDom;
import org.odftoolkit.odfdom.dom.element.office.OfficeTextElement;
import org.odftoolkit.odfdom.dom.element.table.TableTableElement;
import org.odftoolkit.odfdom.incubator.doc.text.OdfTextHeading;
import org.odftoolkit.odfdom.incubator.doc.text.OdfTextParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class OdtService {

    private static final Logger logger = LoggerFactory.getLogger(OdtService.class);
    private static final String ELIGIBILITY_SECTION_ID = "ELIGIBILITY";
    private static final String ESSENTIAL_SECTION_ID = "ESSENTIAL";
    private static final String ORGANISATION_DETAILS_SECTION_ID = "ORGANISATION_DETAILS";
    private static final String FUNDING_DETAILS_SECTION_ID = "FUNDING_DETAILS";

    private static final String APPLICANT_TYPE = "APPLICANT_TYPE";
    private static final String APPLICANT_ORG_NAME = "APPLICANT_ORG_NAME";
    private static final String APPLICANT_ORG_ADDRESS = "APPLICANT_ORG_ADDRESS";
    private static final String APPLICANT_ORG_CHARITY_NUMBER = "APPLICANT_ORG_CHARITY_NUMBER";
    private static final String APPLICANT_ORG_COMPANIES_HOUSE = "APPLICANT_ORG_COMPANIES_HOUSE";
    private static final String APPLICANT_AMOUNT = "APPLICANT_AMOUNT";
    private static final String BENEFITIARY_LOCATION = "BENEFITIARY_LOCATION";
    private static final String APPLICANT_ORG_TYPE_INDIVIDUAL = "I am applying as an individual";

    OdtService() {
    }

    public static void generateSingleOdt(final Submission submission, final String filename) throws Exception {
        try {
            Integer schemeVersion = submission.getSchemeVersion();
            OdfTextDocument odt = OdfTextDocument.newTextDocument();
            OdfContentDom contentDom = odt.getContentDom();
            OfficeTextElement documentText = odt.getContentRoot();
            String largeHeadingStyle = "Heading_20_2";
            String smallHeadingStyle = "Heading_20_10";
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("GMT"));
            final String fundingSectionName = schemeVersion == 1 ? ESSENTIAL_SECTION_ID : FUNDING_DETAILS_SECTION_ID;
            final String requiredCheckSectionName = schemeVersion == 1 ? ESSENTIAL_SECTION_ID : ORGANISATION_DETAILS_SECTION_ID;

            final SubmissionSection eligibilitySection = submission.getSectionById(ELIGIBILITY_SECTION_ID);
            final SubmissionSection requiredCheckSection = submission.getSectionById(requiredCheckSectionName);
            final String orgType = requiredCheckSection.getQuestionById(APPLICANT_TYPE).getResponse();
            final Boolean isIndividual = Objects.equals(orgType, APPLICANT_ORG_TYPE_INDIVIDUAL);

            OdfTextParagraph sectionBreak = new OdfTextParagraph(contentDom);
            sectionBreak.addContentWhitespace("\n\n");

            // Add top-level submission info
            OdfTextHeading mainHeading = new OdfTextHeading(contentDom);
            mainHeading.addStyledContentWhitespace(smallHeadingStyle, "Scheme applied for: " +
                    submission.getSchemeName() + "\n\n");
            final String nameHeadingPrefix = isIndividual ? "Applicant" : "Organisation";
            mainHeading.addStyledContentWhitespace(smallHeadingStyle, nameHeadingPrefix + " name: " +
                    submission.getLegalName() + "\n\n");
            mainHeading.addStyledContentWhitespace(smallHeadingStyle, "Gap ID: " +
                    submission.getGapId() + "\n\n");
            mainHeading.addStyledContentWhitespace(smallHeadingStyle, "Submitted date: " +
                    dateTimeFormatter.format(submission.getSubmittedDate()) + "\n\n");
            mainHeading.addStyledContentWhitespace(smallHeadingStyle, "Amount applied for: £" +
                    submission.getQuestionById(fundingSectionName, APPLICANT_AMOUNT).getResponse());
            documentText.appendChild(mainHeading);

            // ELIGIBILITY SECTION
            OdfTextHeading eligibilityHeading = new OdfTextHeading(contentDom);
            OdfTextParagraph eligibilityStatement = new OdfTextParagraph(contentDom);
            OdfTextParagraph eligibilityResponse = new OdfTextParagraph(contentDom);

            documentText.appendChild(sectionBreak.cloneElement());
            eligibilityHeading.addStyledContent(largeHeadingStyle, "Section 1 - " +
                    eligibilitySection.getSectionTitle());
            documentText.appendChild(eligibilityHeading);
            eligibilityStatement.addStyledContentWhitespace(smallHeadingStyle, "Eligibility statement: \n" +
                    eligibilitySection.getQuestionById(ELIGIBILITY_SECTION_ID).getDisplayText());
            eligibilityResponse.addContentWhitespace("Applicant selected: \n" +
                    eligibilitySection.getQuestionById(ELIGIBILITY_SECTION_ID).getResponse());
            documentText.appendChild(eligibilityStatement);
            documentText.appendChild(eligibilityResponse);

            // ESSENTIAL SECTION or ORGANISATION_DETAILS/FUNDING_DETAILS SECTION based on scheme version

            OdfTextHeading requiredCheckHeading = new OdfTextHeading(contentDom);
            OdfTextParagraph locationQuestion = new OdfTextParagraph(contentDom);
            OdfTextParagraph locationResponse = new OdfTextParagraph(contentDom);

            documentText.appendChild(sectionBreak.cloneElement());
            requiredCheckHeading.addStyledContent(largeHeadingStyle, "Section 2 - " +
                    "Required checks");
            documentText.appendChild(requiredCheckHeading);
            documentText.appendChild(new OdfTextParagraph(contentDom).addContentWhitespace(""));

            documentText.appendChild(generateEssentialTable(documentText, requiredCheckSection, submission.getEmail()));

            locationQuestion.addStyledContent(smallHeadingStyle, "Where this funding will be spent");

            locationResponse.addContentWhitespace(String.join(",\n",
                    submission.getQuestionById(fundingSectionName, BENEFITIARY_LOCATION).getMultiResponse()));

            documentText.appendChild(locationQuestion);
            documentText.appendChild(locationResponse);


            // CUSTOM SECTIONS
            AtomicInteger count = new AtomicInteger(3); // custom section starts from 3
            submission.getSections().forEach(section -> {
                // ignore eligibility and essential section
                if (!Objects.equals(section.getSectionId(), ELIGIBILITY_SECTION_ID) &&
                        !Objects.equals(section.getSectionId(), ESSENTIAL_SECTION_ID) &&
                        !Objects.equals(section.getSectionId(), ORGANISATION_DETAILS_SECTION_ID) &&
                        !Objects.equals(section.getSectionId(), FUNDING_DETAILS_SECTION_ID)) {

                    documentText.appendChild(sectionBreak.cloneElement());

                    // Add section title
                    OdfTextHeading sectionHeading = new OdfTextHeading(contentDom);
                    sectionHeading.addStyledContent(largeHeadingStyle, "Section " + count + " - " +
                            section.getSectionTitle());
                    documentText.appendChild(sectionHeading);

                    // Add the questions
                    section.getQuestions().forEach(question -> {
                        OdfTextParagraph questionParagraph = new OdfTextParagraph(contentDom);
                        OdfTextParagraph responseParagraph = new OdfTextParagraph(contentDom);
                        questionParagraph.addStyledContent(smallHeadingStyle, question.getFieldTitle());

                        switch (question.getResponseType()) {
                            case AddressInput:
                            case MultipleSelection:
                                if (question.getMultiResponse() != null) {
                                    responseParagraph.addContentWhitespace(String.join(",\n",
                                            question.getMultiResponse()) + "\n");
                                } else {
                                    responseParagraph.addContentWhitespace("Not provided");
                                }
                                break;
                            case SingleFileUpload:
                                if (question.getResponse() != null) {
                                    int index = question.getResponse().lastIndexOf(".");

                                    responseParagraph.addContentWhitespace("File name: " + question.getResponse().substring(0, index) + "\n");
                                    responseParagraph.addContentWhitespace("File extension: " + question.getResponse().substring(index + 1) + "\n");
                                } else {
                                    responseParagraph.addContentWhitespace("Not provided");
                                }
                                break;
                            case Date:
                                if (question.getMultiResponse() != null) {
                                    final String date = String.join("-", question.getMultiResponse());
                                    responseParagraph.addContentWhitespace((date.equals("--") ? "Not provided" : date) + "\n");
                                } else {
                                    responseParagraph.addContentWhitespace("Not provided");
                                }
                                break;
                            case YesNo:
                            case Dropdown:
                            case ShortAnswer:
                            case LongAnswer:
                            case Numeric:
                                if(question.getResponse() == null || question.getResponse().isEmpty()) {
                                    responseParagraph.addContentWhitespace("Not provided");
                                } else {
                                    responseParagraph.addContentWhitespace(question.getResponse() + "\n");
                                }
                                break;
                            default:
                                responseParagraph.addContentWhitespace(question.getResponse() + "\n");
                                break;
                        }

                        documentText.appendChild(questionParagraph);
                        documentText.appendChild(responseParagraph);
                    });

                    count.getAndIncrement();
                }
            });

            odt.save(String.format("/tmp/%s.odt", filename));
            odt.close();
        } catch (Exception e) {
            logger.error("Could not generate ODT for given submission", e);
            throw e;
        }
        logger.info("ODT file generated successfully");
    }

    /**
     * Wouldn't you know it, there's no good docs on generating a table by hand using ODFDOM.
     * There might be a better way to do it, but it sure isn't documented anywhere.
     */
    private static TableTableElement generateEssentialTable(final OfficeTextElement documentText,
                                                            final SubmissionSection section,
                                                            final String email) {
        OdfTable odfTable = OdfTable.newTable(documentText, 7, 2);

        final String orgType = section.getQuestionById(APPLICANT_TYPE).getResponse();
        final Boolean isIndividual = Objects.equals(orgType, APPLICANT_ORG_TYPE_INDIVIDUAL);

        final String orgNameHeading = isIndividual ? "Applicant name" : "Legal name of organisation";
        odfTable.getRowByIndex(0).getCellByIndex(0).setStringValue(orgNameHeading);
        odfTable.getRowByIndex(0).getCellByIndex(1).setStringValue(section.getQuestionById(APPLICANT_ORG_NAME).getResponse());

        odfTable.getRowByIndex(1).getCellByIndex(0).setStringValue("Type of organisation");
        odfTable.getRowByIndex(1).getCellByIndex(1).setStringValue(orgType);

        String[] applicantOrgAddress = section.getQuestionById(APPLICANT_ORG_ADDRESS).getMultiResponse();

        odfTable.getRowByIndex(2).getCellByIndex(0).setStringValue("The first line of address for the organisation");
        odfTable.getRowByIndex(2).getCellByIndex(1).setStringValue(applicantOrgAddress[0]);

        odfTable.getRowByIndex(3).getCellByIndex(0).setStringValue("The second line of address for the organisation");
        odfTable.getRowByIndex(3).getCellByIndex(1).setStringValue(applicantOrgAddress[1]);

        odfTable.getRowByIndex(4).getCellByIndex(0).setStringValue("The town of the address for the organisation");
        odfTable.getRowByIndex(4).getCellByIndex(1).setStringValue(applicantOrgAddress[2]);

        odfTable.getRowByIndex(5).getCellByIndex(0).setStringValue("The county of the address for the organisation");
        odfTable.getRowByIndex(5).getCellByIndex(1).setStringValue(applicantOrgAddress[3]);

        odfTable.getRowByIndex(6).getCellByIndex(0)
                .setStringValue("The postcode of the address for the organisation");
        odfTable.getRowByIndex(6).getCellByIndex(1)
                .setStringValue(applicantOrgAddress[4]);

        odfTable.getRowByIndex(7).getCellByIndex(0)
                .setStringValue("The email address for the lead applicant");
        odfTable.getRowByIndex(7).getCellByIndex(1)
                .setStringValue(email);

        Integer index = 8;
        final Boolean hasCharityCommissionNumber = section
                .mayGetQuestionById(APPLICANT_ORG_CHARITY_NUMBER)
                .isPresent();
        if (hasCharityCommissionNumber) {
            odfTable.getRowByIndex(index)
                    .getCellByIndex(0)
                    .setStringValue("Charities Commission number if the organisation has one (if blank, number has not been entered)");
            odfTable.getRowByIndex(index)
                    .getCellByIndex(1)
                    .setStringValue(section
                            .mayGetQuestionById(APPLICANT_ORG_CHARITY_NUMBER)
                            .map(SubmissionQuestion::getResponse)
                            .orElse("")
                    );
            index++;
        }

        final Boolean hasCompaniesHouseNumber = section
                .mayGetQuestionById(APPLICANT_ORG_COMPANIES_HOUSE)
                .isPresent();
        if (hasCompaniesHouseNumber) {
            odfTable.getRowByIndex(index)
                    .getCellByIndex(0)
                    .setStringValue("Companies House number if the organisation has one (if blank, number has not been entered)");
            odfTable.getRowByIndex(index)
                    .getCellByIndex(1)
                    .setStringValue(section
                            .mayGetQuestionById(APPLICANT_ORG_COMPANIES_HOUSE)
                            .map(SubmissionQuestion::getResponse)
                            .orElse("")
                    );
        }

        return odfTable.getOdfElement();
    }
}
