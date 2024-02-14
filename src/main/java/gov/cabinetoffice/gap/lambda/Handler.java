package gov.cabinetoffice.gap.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import gov.cabinetoffice.gap.enums.GrantExportStatus;
import gov.cabinetoffice.gap.model.GrantExportDTO;
import gov.cabinetoffice.gap.model.Submission;
import gov.cabinetoffice.gap.service.*;
import gov.cabinetoffice.gap.utils.HelperUtils;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Handler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private static final Logger logger = LoggerFactory.getLogger(Handler.class);
    private static final AmazonS3 s3client = AmazonS3ClientBuilder.defaultClient();
    private static final OkHttpClient restClient = new OkHttpClient();

    @SneakyThrows
    @Override
    public SQSBatchResponse handleRequest(final SQSEvent event, final Context context) {
        if(event.getRecords().size() <1) {
            throw new RuntimeException("No records found in SQS event");
        }

        final Map<String, SQSEvent.MessageAttribute> messageAttributes = event.getRecords().get(0)
                .getMessageAttributes();
        final String submissionId = messageAttributes.get("submissionId").getStringValue();
        final String emailAddress = messageAttributes.get("emailAddress").getStringValue();
        final String exportBatchId = messageAttributes.get("exportBatchId").getStringValue();
        final String applicationId = messageAttributes.get("applicationId").getStringValue();

        try {
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
            String zipObjectKey = ZipService.uploadZip(submission.getGapId(), filename);

            // Step 5 - Add S3 object key to export
            ExportRecordService.addS3ObjectKeyToExportRecord(restClient, exportBatchId, submissionId, zipObjectKey);

            // STEP 6 - update export record to COMPLETE
            ExportRecordService.updateExportRecordStatus(restClient, exportBatchId, submissionId, GrantExportStatus.COMPLETE);

            // STEP 7 - if final submission, email admin
            final Long outstandingCount = ExportRecordService.getOutstandingExportsCount(restClient, exportBatchId);

            if (Objects.equals(outstandingCount, 0L)) {
                ZipService.deleteTmpDirContents();
                logger.info("Tmp dir cleared before super zip");
                // TODO should we add status for processing etc?
                try {
                    logger.error("Fetching completedGrantExports to create super zip with exportBatchId: " + exportBatchId );
                    final List<GrantExportDTO> completedGrantExports = ExportRecordService.getCompletedExportRecordsByBatchId(restClient, exportBatchId);

                    ZipService.createSuperZip(completedGrantExports);
                    logger.error("Super zip complete");

                    final String superZipFilename = HelperUtils.generateFilename(submission.getSchemeName(), ""); // TODO what should we name this

                    logger.error("Starting to upload super zip to s3");
                    String superZipObjectKey = ZipService.uploadZip(submission.getSchemeId(), superZipFilename);

                    GrantExportBatchService.addS3ObjectKeyToGrantExportBatchRecord(restClient, exportBatchId, superZipObjectKey);
                    logger.error("Super zip location updated");
                    GrantExportBatchService.updateGrantExportBatchRecordStatus(restClient, exportBatchId, GrantExportStatus.COMPLETE);
                    logger.error("Super zip status updated");

                    NotifyService.sendConfirmationEmail(restClient, emailAddress, exportBatchId, submission.getSchemeId(),
                            submissionId);
                } catch (Exception e) {
                    logger.error("Could not process message while trying to create super zip", e);
                    GrantExportBatchService.updateGrantExportBatchRecordStatus(restClient, exportBatchId, GrantExportStatus.FAILED);
                }
            } else {
                logger.info(
                        String.format("Outstanding exports for export batch %s: %s", exportBatchId, outstandingCount));
                SnsService.failureInExport(submission.getSchemeName(), outstandingCount);
            }

            // STEP 9 - clear tmp dir as this is preserved between frequent invocations
            ZipService.deleteTmpDirContents();
            logger.info("Tmp dir cleared");
        } catch (Exception e) {
            logger.error("Could not process message", e);
            ExportRecordService.updateExportRecordStatus(restClient, exportBatchId, submissionId, GrantExportStatus.FAILED);
        }

        logger.info("Message processed successfully");
        return new SQSBatchResponse();
    }

}