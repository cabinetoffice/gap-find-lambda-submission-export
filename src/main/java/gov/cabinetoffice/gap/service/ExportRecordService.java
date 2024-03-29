package gov.cabinetoffice.gap.service;

import gov.cabinetoffice.gap.enums.GrantExportStatus;
import gov.cabinetoffice.gap.lambda.Handler;
import gov.cabinetoffice.gap.model.AddingS3ObjectKeyDTO;
import gov.cabinetoffice.gap.model.FailedExportCountDTO;
import gov.cabinetoffice.gap.model.GrantExportListDTO;
import gov.cabinetoffice.gap.model.OutstandingExportCountDTO;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportRecordService {

    private static final Logger logger = LoggerFactory.getLogger(Handler.class);

    public static void updateExportRecordStatus(OkHttpClient restClient, String exportId, String submissionId, GrantExportStatus newStatus)
            throws Exception {

        final String postEndpoint = "/submissions/" + submissionId + "/export-batch/" + exportId + "/status";
        logger.info("Sending postRequest to {}", postEndpoint);
        RestService.sendPostRequest(restClient, "\"" + newStatus.toString() + "\"", postEndpoint);
    }

    public static long getOutstandingExportsCount(OkHttpClient restClient, String exportId) throws Exception {
        final String getEndpoint = "/grant-export/" + exportId + "/outstandingCount";
        logger.info("Sending getRequest to {}", getEndpoint);
        return RestService.sendGetRequest(restClient, null, getEndpoint, OutstandingExportCountDTO.class).getOutstandingCount();
    }

    public static void addS3ObjectKeyToExportRecord(OkHttpClient restClient, String exportId, String submissionId, String s3ObjectKey)
            throws Exception {
        final String patchEndpoint = "/submissions/" + submissionId + "/export-batch/" + exportId + "/s3-object-key";
        logger.info("Sending patchRequest to {}", patchEndpoint);
        RestService.sendPatchRequest(restClient, new AddingS3ObjectKeyDTO(s3ObjectKey), patchEndpoint);
    }

    public static GrantExportListDTO getCompletedExportRecordsByBatchId(OkHttpClient restClient, String exportId)
            throws Exception {
        final String getEndpoint = "/grant-export/" + exportId + "/completed";
        logger.info("Sending getRequest to {}", getEndpoint);
        return  RestService.sendGetRequest(restClient, null, getEndpoint, GrantExportListDTO.class);
    }

    public static long getFailedExportsCount(OkHttpClient restClient, String exportId) throws Exception {
        final String getEndpoint = "/grant-export/" + exportId + "/failedCount";
        logger.info("Sending getRequest to {}", getEndpoint);
        return RestService.sendGetRequest(restClient, null, getEndpoint, FailedExportCountDTO.class).getFailedCount();
    }

    public static long getRemainingExportsCount(OkHttpClient restClient, String exportId) throws Exception {
        final String getEndpoint = "/grant-export/" + exportId + "/remainingCount";
        logger.info("Sending getRequest to {}", getEndpoint);
        return RestService.sendGetRequest(restClient, null, getEndpoint, OutstandingExportCountDTO.class).getOutstandingCount();
    }

    public static void updateGrantExportBatchRecordStatus(OkHttpClient restClient, String exportId, GrantExportStatus newStatus)
            throws Exception {
        final String patchEndpoint = "/grant-export/" + exportId + "/batch/status";
        logger.info("Sending patch request to {} to update status to: {} for export batch with ID: {}",
                patchEndpoint, newStatus.toString(), exportId);
        RestService.sendPatchRequest(restClient, newStatus, patchEndpoint);
    }

    public static void addS3ObjectKeyToGrantExportBatchRecord(OkHttpClient restClient, String exportId, String s3ObjectKey)
            throws Exception {
        final String patchEndpoint = "/grant-export/" + exportId + "/batch/s3-object-key";
        logger.info("Sending patch request to {} to update location to: {} for export batch with ID: {}",
                patchEndpoint, s3ObjectKey, exportId);
        RestService.sendPatchRequest(restClient, new AddingS3ObjectKeyDTO(s3ObjectKey), patchEndpoint);
    }

}
