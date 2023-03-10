package gov.cabinetoffice.gap.testdata;

import gov.cabinetoffice.gap.enums.ResponseTypeEnum;
import gov.cabinetoffice.gap.enums.SubmissionSectionStatus;
import gov.cabinetoffice.gap.model.Submission;
import gov.cabinetoffice.gap.model.SubmissionQuestion;
import gov.cabinetoffice.gap.model.SubmissionQuestionValidation;
import gov.cabinetoffice.gap.model.SubmissionSection;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This is a temporary test class, not planned to be present for deploy
 * Once this is removed, we can opt to remove Lombok if we want to reduce zip/jar size
 */
public class TestData {
    public static final SubmissionQuestion ELIGIBILITY_SUBMISSION_QUESTION = SubmissionQuestion.builder()
            .questionId("ELIGIBILITY").fieldTitle("Eligibility Statement")
            .displayText("Some admin supplied text describing what it means to be eligible to apply for this grant")
            .questionSuffix("Does your organisation meet the eligibility criteria?")
            .responseType(ResponseTypeEnum.YesNo)
            .validation(SubmissionQuestionValidation.builder().mandatory(true).build()).response("Yes").build();

    public static final SubmissionQuestion ORG_TYPE_SUBMISSION_QUESTION = SubmissionQuestion.builder()
            .questionId("APPLICANT_TYPE").fieldTitle("Choose your organisation type").profileField("ORG_TYPE")
            .hintText("Choose the option that best describes your organisation").responseType(ResponseTypeEnum.Dropdown)
            .validation(SubmissionQuestionValidation.builder().mandatory(true).build()).options(new String[]{
                    "Limited company", "Non-limited company", "Registered charity", "Unregistered charity", "Other"})
            .response("Limited company").build();

    public static final SubmissionQuestion ORG_NAME_SUBMISSION_QUESTION = SubmissionQuestion.builder()
            .questionId("APPLICANT_ORG_NAME").profileField("ORG_NAME").fieldTitle("Enter the name of your organisation")
            .hintText(
                    "This is the official name of your organisation. It could be the name that is registered with Companies House or the Charities Commission")
            .responseType(ResponseTypeEnum.ShortAnswer)
            .validation(SubmissionQuestionValidation.builder().mandatory(true).minLength(5).maxLength(100).build())
            .response("Some company name").build();

    public static final SubmissionQuestion ORG_AMOUNT_SUBMISSION_QUESTION = SubmissionQuestion.builder()
            .questionId("APPLICANT_AMOUNT").profileField("ORG_AMOUNT")
            .fieldTitle("Enter the money you would wish to receive")
            .hintText(
                    "This is the official name of your organisation. It could be the name that is registered with Companies House or the Charities Commission")
            .responseType(ResponseTypeEnum.ShortAnswer)
            .validation(SubmissionQuestionValidation.builder().mandatory(true).minLength(5).maxLength(100).build())
            .response("500").build();

    public static final SubmissionQuestion ORG_ADDRESS_SUBMISSION_QUESTION = SubmissionQuestion.builder()
            .questionId("APPLICANT_ORG_ADDRESS").profileField("ORG_ADDRESS")
            .fieldTitle("Enter your organisation's address").responseType(ResponseTypeEnum.AddressInput)
            .validation(SubmissionQuestionValidation.builder().mandatory(true).build())
            .multiResponse(new String[]{"9-10 St Andrew Square", "", "Edinburgh", "", "EH2 2AF"}).build();

    public static final SubmissionQuestion ORG_COMPANIES_NO_SUBMISSION_QUESTION = SubmissionQuestion.builder()
            .questionId("APPLICANT_ORG_COMPANIES_HOUSE").profileField("ORG_COMPANIES_HOUSE")
            .fieldTitle("Please supply the Companies House number for your organisation - if applicable")
            .hintText(
                    "Funding organisation might use this to identify your organisation when you apply for a grant. It might also be used to check your organisation is legitimate.")
            .responseType(ResponseTypeEnum.ShortAnswer)
            .validation(SubmissionQuestionValidation.builder().mandatory(true).build()).response("Yes").build();

    public static final SubmissionQuestion ORG_CHARITY_NO_SUBMISSION_QUESTION = SubmissionQuestion.builder()
            .questionId("APPLICANT_ORG_CHARITY_NUMBER").profileField("ORG_CHARITY_COMMISSION_NUMBER")
            .fieldTitle("Please supply the Charity Commission number for your organisation - if applicable")
            .hintText(
                    "Funding organisation might use this to identify your organisation when you apply for a grant. It might also be used to check your organisation is legitimate.")
            .responseType(ResponseTypeEnum.ShortAnswer)
            .validation(SubmissionQuestionValidation.builder().mandatory(true).build()).response("12738494").build();

    public static final SubmissionQuestion ORG_BENEFICIARY_LOC_SUBMISSION_QUESTION = SubmissionQuestion.builder()
            .questionId("BENEFITIARY_LOCATION").fieldTitle("Where will this funding be spent?")
            .hintText(
                    "Select the location where the grant funding will be spent. You can choose more than one, if it is being spent in more than one location.\\n\\nSelect all that apply:")
            .adminSummary("where the funding will be spent").responseType(ResponseTypeEnum.MultipleSelection)
            .validation(SubmissionQuestionValidation.builder().mandatory(true).build())
            .options(new String[]{"North East England", "North West England", "South East England",
                    "South West England", "Midlands", "Scotland", "Wales", "Northern Ireland"})
            .multiResponse(new String[]{"Scotland", "North East England"}).build();

    public static final SubmissionSection ELIGIBILITY_SECTION_SUBMISSION = SubmissionSection.builder()
            .sectionId("ELIGIBILITY").sectionTitle("Eligibility").sectionStatus(SubmissionSectionStatus.COMPLETED)
            .questions(Collections.singletonList(ELIGIBILITY_SUBMISSION_QUESTION)).build();

    public static final SubmissionSection ESSENTIAL_SECTION_SUBMISSION = SubmissionSection.builder()
            .sectionId("ESSENTIAL").sectionTitle("Essential Information")
            .sectionStatus(SubmissionSectionStatus.COMPLETED)
            .questions(Arrays.asList(ORG_TYPE_SUBMISSION_QUESTION, ORG_NAME_SUBMISSION_QUESTION,
                    ORG_AMOUNT_SUBMISSION_QUESTION, ORG_ADDRESS_SUBMISSION_QUESTION,
                    ORG_COMPANIES_NO_SUBMISSION_QUESTION, ORG_CHARITY_NO_SUBMISSION_QUESTION,
                    ORG_BENEFICIARY_LOC_SUBMISSION_QUESTION))
            .build();

    public static final SubmissionSection CUSTOM_SECTION_SUBMISSION = SubmissionSection.builder()
            .sectionId("26215de2-2211-48e4-9c82-835cd227daad").sectionTitle("My Custom Section")
            .sectionStatus(SubmissionSectionStatus.COMPLETED)
            .questions(Arrays.asList(ORG_TYPE_SUBMISSION_QUESTION, ORG_NAME_SUBMISSION_QUESTION,
                    ORG_AMOUNT_SUBMISSION_QUESTION, ORG_ADDRESS_SUBMISSION_QUESTION,
                    ORG_COMPANIES_NO_SUBMISSION_QUESTION, ORG_CHARITY_NO_SUBMISSION_QUESTION,
                    ORG_BENEFICIARY_LOC_SUBMISSION_QUESTION,
                    SubmissionQuestion.builder()
                            .fieldTitle("Upload an important file")
                            .responseType(ResponseTypeEnum.SingleFileUpload)
                            .response("file_name.pdf").build()))
            .build();

    public static final List<SubmissionSection> SUBMISSION_SECTIONS_LIST = Arrays.asList(ELIGIBILITY_SECTION_SUBMISSION,
            ESSENTIAL_SECTION_SUBMISSION, CUSTOM_SECTION_SUBMISSION);

    public static final Submission SUBMISSION = Submission.builder().legalName("Test Org Name").schemeName("Scheme Name")
            .gapId("GAP-LL-20220927-00001").submittedDate(Instant.parse("2022-02-15T18:35:24.00Z")).sections(SUBMISSION_SECTIONS_LIST).build();

}
