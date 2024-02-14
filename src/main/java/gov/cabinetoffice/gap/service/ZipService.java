package gov.cabinetoffice.gap.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import gov.cabinetoffice.gap.model.GrantExportDTO;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipService {

    private static final Logger logger = LoggerFactory.getLogger(ZipService.class);

    private static final String TMP_DIR = "/tmp/";

    private static final String LOCAL_ZIP_FILE_NAME = "submission.zip";

    private static final String SUBMISSION_ATTACHMENTS_BUCKET_NAME = System
            .getenv("SUBMISSION_ATTACHMENTS_BUCKET_NAME");

    private static final String SUBMISSION_EXPORTS_BUCKET_NAME = System
            .getenv("SUBMISSION_EXPORTS_BUCKET_NAME");

    //regex for any special character that are not allowed in window os : <, >, ", /, \, |, ?, or *
    private static final String SPECIAL_CHARACTER_REGEX = "[<>\"\\/|?*\\\\]";

    public static final Integer LONG_FILE_NAME_LENGTH = 50; //50 characters may be too strict but can revisit if required
    private static AmazonS3 s3Client;

    public static void createSuperZip(List<GrantExportDTO> completedGrantExports) throws IOException {
        final List<String> filenames = new ArrayList<>();
        logger.info("Downloading completed grant export with size: {}", completedGrantExports.size());
        for (GrantExportDTO grantExport: completedGrantExports) {
            logger.info("Looping through completed grant exports");
            logger.info("Grant export with id: {}", grantExport.getExportBatchId());
            final String location = grantExport.getLocation();
            final String folderNameToRemove = location.split("/")[0];
            final String fileName = location.replace(folderNameToRemove + "/", "").replaceAll(SPECIAL_CHARACTER_REGEX, "_");

            filenames.add(fileName);
            downloadFile(fileName, SUBMISSION_EXPORTS_BUCKET_NAME);
        }
        logger.info("Starting to zip completed grant export inside super zip");
        zipFiles(filenames,"");

    }

    public static void createZip(final AmazonS3 client, final String filename, final String applicationId,
                                 final String submissionId) throws IOException {
        s3Client = client;
        final List<String> submissionAttachmentFileNames = getSubmissionAttachmentFileNames(client, applicationId,
                submissionId);
        for (String fileName : submissionAttachmentFileNames) {
            downloadFile(fileName, SUBMISSION_ATTACHMENTS_BUCKET_NAME);
        }

        final List<String> fileNamesToZIP = new ArrayList<>(submissionAttachmentFileNames);
        fileNamesToZIP.add(filename + ".odt");

        zipFiles(fileNamesToZIP, applicationId + "/" + submissionId + "/");

        logger.info("Zip file created");
    }

    public static String uploadZip(final String id, final String zipFilename) {
        try {
            final String objectKey = id + "/" + zipFilename + ".zip";
            s3Client.putObject(System.getenv("SUBMISSION_EXPORTS_BUCKET_NAME"), objectKey,
                    new File(TMP_DIR + LOCAL_ZIP_FILE_NAME));
            logger.info("Zip file uploaded to S3");
            return objectKey;
        } catch (Exception e) {
            logger.error("Could not upload to S3", e);
            throw e;
        }
    }

    public static List<String> getSubmissionAttachmentFileNames(final AmazonS3 s3Client,
                                                                final String applicationId,
                                                                final String submissionId) {
        final ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(SUBMISSION_ATTACHMENTS_BUCKET_NAME)
                .withPrefix(applicationId + "/" + submissionId);
        final ListObjectsV2Result listing = s3Client.listObjectsV2(req);
        final List<S3ObjectSummary> objectSummaries = listing.getObjectSummaries();
        return objectSummaries.stream()
                .filter(objectSummary -> {
                    final List<String> keyParts = List.of(objectSummary.getKey().split("/"));
                    final String prefix = keyParts.stream().limit(3).collect(Collectors.joining("/"));
                    final List<S3ObjectSummary> matchingObjectSummaries = getAllFromPrefix(objectSummaries, prefix);
                    return matchingObjectSummaries.stream()
                      .allMatch(os -> os.getLastModified().before(objectSummary.getLastModified()) || os.getLastModified().equals(objectSummary.getLastModified()));
                })
                .map(S3ObjectSummary::getKey)
                .filter(filename -> filename.contains("."))
                .collect(Collectors.toList());
    }

    private static List<S3ObjectSummary> getAllFromPrefix(final List<S3ObjectSummary> objectSummaries, final String prefix) {
        return objectSummaries.stream()
                .filter(objectSummary -> objectSummary.getKey().startsWith(prefix))
                .collect(Collectors.toList());
    }

    private static void downloadFile(final String fileName, final String bucketName) {
        try {
            File localFile = new File(TMP_DIR + fileName);
            s3Client.getObject(new GetObjectRequest(bucketName, fileName), localFile);
        } catch (AmazonServiceException e) {
            logger.error("Could not download file: " + fileName + " from bucket: " + bucketName,
                    e);
            throw e;
        }
    }

    public static String parseFileName(final String objectKey, int suffix, final String path) {
        final String filenameWithoutFolderName = getFileNameFromS3ObjectKey(objectKey, path);
        final String[] fileNameParts = filenameWithoutFolderName.split("\\.");
        final String fileExtension = "." + fileNameParts[fileNameParts.length - 1];
        final String filenameWithoutExtension = filenameWithoutFolderName.replace(fileExtension, "");

        // Need to trim very long file names to prevent max path length errors in windows
        final String truncatedFileName = filenameWithoutExtension.length() > LONG_FILE_NAME_LENGTH ?
                filenameWithoutExtension.substring(0, LONG_FILE_NAME_LENGTH).trim() : filenameWithoutExtension;

        return truncatedFileName.concat("_" + suffix + fileExtension);
    }

    public static void deleteTmpDirContents() {
        try {
            FileUtils.cleanDirectory(new File(TMP_DIR));
        } catch (IOException e) {
            logger.error("Could not delete the contents of the tmp directory", e);
        }
    }

    private static void zipFiles(final List<String> files, final String path) throws IOException {
        try (
            final FileOutputStream fout = new FileOutputStream(TMP_DIR + LOCAL_ZIP_FILE_NAME);
            final ZipOutputStream zout = new ZipOutputStream(fout)) {
            int index = 1;
            for (String filename : files) {
                addFileToZip(filename, zout, index, path);
                index++;
            }
        } catch (FileNotFoundException e) {
            logger.error("Could not create the locally zipped file: " + LOCAL_ZIP_FILE_NAME, e);
            throw e;
        } catch (IOException e) {
            logger.error("IO exception while creating the empty zipped file", e);
            throw e;
        }
    }

    private static void addFileToZip(final String filename, final ZipOutputStream zout,
                                     final int index, final String path) throws IOException {
        try (final FileInputStream fis = new FileInputStream(TMP_DIR + filename)) {
            // Create zip entry within the zipped file

            final ZipEntry ze = new ZipEntry(parseFileName(filename, index, path));
            zout.putNextEntry(ze);
            // Copy file contents over to zip entry
            int length;
            byte[] buffer = new byte[1024];
            while ((length = fis.read(buffer)) > 0) {
                zout.write(buffer, 0, length);
            }
            // Close streams
            zout.closeEntry();
        } catch (FileNotFoundException e) {
            logger.error("Could not create a zip entry with the name: " + filename, e);
            throw e;
        } catch (IOException e) {
            logger.error("IO exception while creating the zip entry with the name: " + filename, e);
            throw e;
        }
    }

    private static String getFileNameFromS3ObjectKey(String objectKey, String applicationIdAndSubmissionId) {
        //an object key is formed by applicationId/submissionId/s3bucketRandomFolderName/filename
        final String filenameWithoutApplicationIdAndSubmissionId = objectKey.replace(applicationIdAndSubmissionId, "");
        final String folderNameToRemove = filenameWithoutApplicationIdAndSubmissionId.split("/")[0];
        return filenameWithoutApplicationIdAndSubmissionId.replace(folderNameToRemove + "/", "").replaceAll(SPECIAL_CHARACTER_REGEX, "_");
    }
}
