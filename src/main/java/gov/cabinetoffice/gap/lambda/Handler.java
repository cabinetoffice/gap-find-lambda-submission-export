package gov.cabinetoffice.gap.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import gov.cabinetoffice.gap.enums.GrantExportStatus;
import gov.cabinetoffice.gap.model.Submission;
import gov.cabinetoffice.gap.service.ExportRecordService;
import gov.cabinetoffice.gap.service.NotifyService;
import gov.cabinetoffice.gap.service.OdtService;
import gov.cabinetoffice.gap.service.SubmissionService;
import gov.cabinetoffice.gap.service.ZipService;
import gov.cabinetoffice.gap.utils.HelperUtils;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

public class Handler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private static final Logger logger = LoggerFactory.getLogger(Handler.class);
    private static final AmazonS3 s3client = AmazonS3ClientBuilder.defaultClient();
    private static final OkHttpClient restClient = new OkHttpClient();

    @Override
    public SQSBatchResponse handleRequest(final SQSEvent event, final Context context) {
        try {
            final Map<String, SQSEvent.MessageAttribute> messageAttributes = event.getRecords().get(0)
                    .getMessageAttributes();
            final String submissionId = messageAttributes.get("submissionId").getStringValue();
            final String emailAddress = messageAttributes.get("emailAddress").getStringValue();
            final String exportBatchId = messageAttributes.get("exportBatchId").getStringValue();
            final String applicationId = messageAttributes.get("applicationId").getStringValue();

            logger.info("Received message with submissionId: {} and exportBatchId: {}", submissionId, exportBatchId);

            // STEP 0 - update export record to PROCESSING
            ExportRecordService.updateExportRecordStatus(restClient, exportBatchId, submissionId, GrantExportStatus.PROCESSING);

            // STEP 1 - get submission from database
            // legal name is assigned from the response they give in the essential questions section
            final Submission submission = SubmissionService.getSubmissionData(restClient, exportBatchId, submissionId);
            String legalName = submission.getSchemeVersion() == 1 ?
                    submission.getSectionById("ESSENTIAL").getQuestionById("APPLICANT_ORG_NAME").getResponse()
                    :
                    submission.getSectionById("ORGANISATION_DETAILS").getQuestionById("APPLICANT_ORG_NAME").getResponse();

            submission.setLegalName(legalName);

            // STEP 2 - generate .odt from submission
            final String filename = HelperUtils.generateFilename(submission.getLegalName(), submission.getGapId());
            OdtService.generateSingleOdt(submission, filename);

            // STEP 3 - download all relevant attachments and zip along with .odt
            ZipService.createZip(s3client, filename, applicationId, submissionId);

            // STEP 4 - upload zip to S3
            String zipObjectKey = ZipService.uploadZip(submission, filename);

            // Step 5 - Add S3 object key to export
            ExportRecordService.addS3ObjectKeyToExportRecord(restClient, exportBatchId, submissionId, zipObjectKey);

            // STEP 6 - update export record to COMPLETE
            ExportRecordService.updateExportRecordStatus(restClient, exportBatchId, submissionId, GrantExportStatus.COMPLETE);

            // STEP 7 - if final submission, email admin
            final Long outstandingCount = ExportRecordService.getOutstandingExportsCount(restClient, exportBatchId);

            if (Objects.equals(outstandingCount, 0L)) {
                NotifyService.sendConfirmationEmail(restClient, emailAddress, exportBatchId, submission.getSchemeId(),
                        submissionId);
            } else {
                logger.info(
                        String.format("Outstanding exports for export batch %s: %s", exportBatchId, outstandingCount));
            }

            // STEP 9 - clear tmp dir as this is preserved between frequent invocations
            ZipService.deleteTmpDirContents();
            logger.info("Tmp dir cleared");
        } catch (Exception e) {
            logger.error("Could not process message", e);
            throw new RuntimeException(e);
        }

        logger.info("Message processed successfully");
        return new SQSBatchResponse();
    }

}