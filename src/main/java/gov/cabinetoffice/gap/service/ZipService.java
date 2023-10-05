package gov.cabinetoffice.gap.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import gov.cabinetoffice.gap.model.Submission;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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

    private static AmazonS3 s3Client;

    public static void createZip(final AmazonS3 client, final String filename, final String applicationId,
            final String submissionId) throws IOException {
        s3Client = client;
        final List<String> submissionAttachmentFileNames = getSubmissionAttachmentFileNames(applicationId,
                submissionId);
        for (String fileName : submissionAttachmentFileNames) {
            downloadFile(fileName);
        }

        final List<String> fileNamesToZIP = new ArrayList<>(submissionAttachmentFileNames);
        fileNamesToZIP.add(filename + ".odt");

        zipFiles(fileNamesToZIP, applicationId, submissionId);
    }

    public static String uploadZip(final Submission submission, final String zipFilename) {
        try {
            final String objectKey = submission.getGapId() + "/" + zipFilename + ".zip";
            s3Client.putObject(System.getenv("SUBMISSION_EXPORTS_BUCKET_NAME"), objectKey,
                    new File(TMP_DIR + LOCAL_ZIP_FILE_NAME));

            return objectKey;
        } catch (Exception e) {
            logger.error("Could not upload to S3", e);
            throw e;
        }
    }

    private static List<String> getSubmissionAttachmentFileNames(final String applicationId,
            final String submissionId) {
        final ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(SUBMISSION_ATTACHMENTS_BUCKET_NAME)
                .withPrefix(applicationId + "/" + submissionId);
        final ListObjectsV2Result listing = s3Client.listObjectsV2(req);
        final List<S3ObjectSummary> objectSummaries = listing.getObjectSummaries();
        final List<S3ObjectSummary> mostRecentObjectSummaries = objectSummaries.stream()
                .filter(objectSummary -> {
                    final List<String> keyParts = List.of(objectSummary.getKey().split("/"));
                    final String prefix = keyParts.stream().limit(3).collect(Collectors.joining("/"));
                    final List<S3ObjectSummary> matchingObjectSummaries = getAllFromPrefix(objectSummaries, prefix);
                    return matchingObjectSummaries.stream()
                            .allMatch(os -> os.getLastModified().before(objectSummary.getLastModified()) || os.equals(objectSummary)
                            );

                })
                .collect(Collectors.toList());
        return mostRecentObjectSummaries.stream()
                .map(S3ObjectSummary::getKey)
                .filter(filename -> filename.contains("."))
                .collect(Collectors.toList());
    }

    private static List<S3ObjectSummary> getAllFromPrefix(final List<S3ObjectSummary> objectSummaries, final String prefix) {
        return objectSummaries.stream()
                .filter(objectSummary -> objectSummary.getKey().startsWith(prefix))
                .collect(Collectors.toList());
    }

    private static void downloadFile(final String fileName) {
        try {
            File localFile = new File(TMP_DIR + fileName);
            s3Client.getObject(new GetObjectRequest(SUBMISSION_ATTACHMENTS_BUCKET_NAME, fileName), localFile);
        } catch (AmazonServiceException e) {
            logger.error("Could not download file: " + fileName + " from bucket: " + SUBMISSION_ATTACHMENTS_BUCKET_NAME,
                    e);
            throw e;
        }
    }

    public static String parseFileName(final String objectKey, int suffix, final String applicationId,
                                       final String submissionId) {
        //an object key is formed by applicationId/submissionId/s3bucketRandomFolderName/filename
        final String applicationIdAndSubmissionId = applicationId + "/" + submissionId + "/";
        final String filenameWithoutApplicationIdAndSubmissionId = objectKey.replace(applicationIdAndSubmissionId, "");
        final String folderNameToRemove = filenameWithoutApplicationIdAndSubmissionId.split("/")[0];
        final String filenameWithoutFolderName = filenameWithoutApplicationIdAndSubmissionId.replace(folderNameToRemove + "/", "");
        //the file that we allow to attach can only be of the following types: .doc, .docx, .odt, .pdf, .xls, .xlsx, .zip
        String filenameWithoutExtension = filenameWithoutFolderName.split("\\.(doc|docx|odt|pdf|xls|xlsx|zip)$")[0];
        String fileExtension = filenameWithoutFolderName.replace(filenameWithoutExtension, "");
        //edge scenario where a file doesn't have the expected extension.
        if(filenameWithoutFolderName.equals(filenameWithoutExtension)){
            final String[] fileNameParts = filenameWithoutFolderName.split("\\.");
             fileExtension = "." + fileNameParts[fileNameParts.length - 1];
            filenameWithoutExtension = filenameWithoutFolderName.replace( fileExtension, "");
        }
        return filenameWithoutExtension.concat("_" + suffix + fileExtension);
    }

    public static void deleteTmpDirContents() {
        try {
            FileUtils.cleanDirectory(new File(TMP_DIR));
        } catch (IOException e) {
            logger.error("Could not delete the contents of the tmp directory", e);
        }
    }

    private static void zipFiles(final List<String> files,final String applicationId,
                                 final String submissionId) throws IOException {
        try (
            final FileOutputStream fout = new FileOutputStream(TMP_DIR + LOCAL_ZIP_FILE_NAME);
            final ZipOutputStream zout = new ZipOutputStream(fout)) {
            int index = 1;
            for (String filename : files) {
                logger.info("filename " + filename);
                addFileToZip(filename, zout, index, applicationId, submissionId);
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
                                     final int index,final String applicationId,
                                     final String submissionId) throws IOException {
        try (final FileInputStream fis = new FileInputStream(TMP_DIR + filename)) {
            // Create zip entry within the zipped file

            final ZipEntry ze = new ZipEntry(parseFileName(filename, index, applicationId, submissionId));
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

}
