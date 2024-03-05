package gov.cabinetoffice.gap.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import gov.cabinetoffice.gap.enums.GrantExportStatus;
import gov.cabinetoffice.gap.exceptions.EmptySqsEventException;
import gov.cabinetoffice.gap.model.GrantExportListDTO;
import gov.cabinetoffice.gap.model.Submission;
import gov.cabinetoffice.gap.service.*;
import gov.cabinetoffice.gap.utils.HelperUtils;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

public class Handler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private static final Logger logger = LoggerFactory.getLogger(Handler.class);
    private static final AmazonS3 s3client = AmazonS3ClientBuilder.defaultClient();
    private static final OkHttpClient restClient = new OkHttpClient();
    private static final String ATTACHMENTS_ZIP_FILE_NAME = "attachments";

    @SneakyThrows
    @Override
    public SQSBatchResponse handleRequest(final SQSEvent event, final Context context) {
        if (event.getRecords().isEmpty()) {
            throw new EmptySqsEventException("No records found in SQS event");
        }

        final Map<String, SQSEvent.MessageAttribute> messageAttributes = event.getRecords().get(0)
                .getMessageAttributes();
        final String submissionId = messageAttributes.get("submissionId").getStringValue();
        final String emailAddress = messageAttributes.get("emailAddress").getStringValue();
        final String exportBatchId = messageAttributes.get("exportBatchId").getStringValue();
        final String applicationId = messageAttributes.get("applicationId").getStringValue();
        String schemeName = "";
        String filename = "";
        String gapId = "";
        Submission submission = null;

        try {
            logger.info("Received message with submissionId: {} and exportBatchId: {}", submissionId, exportBatchId);

            // STEP 0 - update export record to PROCESSING
            ExportRecordService.updateExportRecordStatus(restClient, exportBatchId, submissionId, GrantExportStatus.PROCESSING);

            // STEP 1 - get submission from database
            // legal name is assigned from the response they give in the essential questions section
            submission = SubmissionService.getSubmissionData(restClient, exportBatchId, submissionId);
            String legalName = submission.getSchemeVersion() == 1 ?
                    submission.getSectionById("ESSENTIAL").getQuestionById("APPLICANT_ORG_NAME").getResponse()
                    :
                    submission.getSectionById("ORGANISATION_DETAILS").getQuestionById("APPLICANT_ORG_NAME").getResponse();

            submission.setLegalName(legalName);
            schemeName = submission.getSchemeName();
            gapId = submission.getGapId();

            // STEP 2 - generate .odt from submission
            filename = HelperUtils.generateFilename(submission.getLegalName(), submission.getGapId());
            OdtService.generateSingleOdt(submission, filename);

            // STEP 3 - download all relevant attachments and zip along with .odt
            ZipService.createZip(s3client, filename, applicationId, submissionId, true);

            // STEP 4 - upload zip to S3
            String zipObjectKey = ZipService.uploadZip(submission.getGapId(), filename);

            // Step 5 - Add S3 object key to export
            ExportRecordService.addS3ObjectKeyToExportRecord(restClient, exportBatchId, submissionId, zipObjectKey);

            // STEP 6 - update export record to COMPLETE
            ExportRecordService.updateExportRecordStatus(restClient, exportBatchId, submissionId, GrantExportStatus.COMPLETE);

            // STEP 7 - if final submission, email admin
            final Long outstandingCount = ExportRecordService.getRemainingExportsCount(restClient, exportBatchId);

            if (Objects.equals(outstandingCount, 0L)) {

                ZipService.deleteTmpDirContents();
                logger.info("Tmp dir cleared before creating super zip");
                try {
                    ExportRecordService.updateGrantExportBatchRecordStatus(restClient, exportBatchId, GrantExportStatus.PROCESSING);

                    final GrantExportListDTO completedGrantExports = ExportRecordService.getCompletedExportRecordsByBatchId(restClient, exportBatchId);
                    logger.info("Finished fetching completedGrantExports with size of: {}", completedGrantExports.getGrantExports().size());

                    ZipService.createSuperZip(completedGrantExports.getGrantExports());

                    final String superZipFilename = HelperUtils.generateFilename(schemeName, "");

                    final String superZipObjectKey = ZipService.uploadZip(submission.getSchemeId() + "/" + exportBatchId, superZipFilename);

                    ExportRecordService.addS3ObjectKeyToGrantExportBatchRecord(restClient, exportBatchId, superZipObjectKey);
                    ExportRecordService.updateGrantExportBatchRecordStatus(restClient, exportBatchId, GrantExportStatus.COMPLETE);
                } catch (Exception e) {
                    logger.error("Could not process message while trying to create super zip", e);
                    ExportRecordService.updateGrantExportBatchRecordStatus(restClient, exportBatchId, GrantExportStatus.FAILED);
                } finally {
                    NotifyService.sendConfirmationEmail(restClient, emailAddress, exportBatchId, submission.getSchemeId(),
                            submissionId);
                }

            } else {
                logger.info(
                        "Outstanding exports for export batch {}: {}", exportBatchId, outstandingCount);
            }

            // STEP 9 - clear tmp dir as this is preserved between frequent invocations
            ZipService.deleteTmpDirContents();
            logger.info("Tmp dir cleared");
        } catch (Exception e) {
            logger.error("Could not process message", e);
            ExportRecordService.updateExportRecordStatus(restClient, exportBatchId, submissionId, GrantExportStatus.FAILED);

            try {
                logger.info("Trying to create attachment zip");

                if(submission !=null && submission.isHasAttachments()) {
                    logger.info("Creating attachments zip for failed submission with ID {}", submissionId);
                    // download all relevant attachments and zip without the .odt
                    ZipService.createZip(s3client, filename, applicationId, submissionId, false);
                    final String zipObjectKey = ZipService.uploadZip(gapId, ATTACHMENTS_ZIP_FILE_NAME);
                    ExportRecordService.addS3ObjectKeyToExportRecord(restClient, exportBatchId, submissionId, zipObjectKey);
                } else if(submission !=null) {
                    logger.info("Updating location to null for submission {}", submissionId);
                    ExportRecordService.addS3ObjectKeyToExportRecord(restClient, exportBatchId, submissionId, null);
                }
            }
            catch (Exception error) {
                logger.error("Couldn't create attachments zip for submission with ID " + submissionId,  error);
            }

        } finally {
            final Long remainingExports = ExportRecordService.getRemainingExportsCount(restClient, exportBatchId);
            logger.info(String.format("Submissions export complete. There are %s remaining exports.", remainingExports));

            if(Objects.equals(remainingExports, 0L)) {
                final Long failedSubmissionsCount = ExportRecordService.getFailedExportsCount(restClient, exportBatchId);
                logger.info("There are {} failed submissions.", failedSubmissionsCount);
                if (failedSubmissionsCount > 0L) {
                    String outcome = new SnsService((AmazonSNSClient) AmazonSNSClientBuilder.defaultClient())
                            .failureInExport(schemeName, failedSubmissionsCount);
                    logger.info(outcome);
                }
            }
        }

        logger.info("Message processed successfully");
        return new SQSBatchResponse();
    }

}