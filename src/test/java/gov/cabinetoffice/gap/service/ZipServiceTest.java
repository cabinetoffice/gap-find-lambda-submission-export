package gov.cabinetoffice.gap.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ZipServiceTest {
    private final AmazonS3 s3client = Mockito.mock(AmazonS3.class);
    private File testGapIDFile;
    private File testHelloWorldFile1;
    private File testHelloWorldFile2;

    @BeforeEach
    void beforeEach() throws Exception {
        // Mock list objects v2 to return a single file named "hello-world.txt"
        final ListObjectsV2Result res = Mockito.mock(ListObjectsV2Result.class);
        final List<S3ObjectSummary> objectSummaryList = new ArrayList<>();
        final S3ObjectSummary s3ObjectSummary1 = new S3ObjectSummary();
        s3ObjectSummary1.setKey("/some/random/prefix/hello-world1.txt");
        s3ObjectSummary1.setLastModified(new Date());
        objectSummaryList.add(s3ObjectSummary1);
        Thread.sleep(1000);
        final S3ObjectSummary s3ObjectSummary2 = new S3ObjectSummary();
        s3ObjectSummary2.setKey("/some/random/prefix/hello-world2.txt");
        s3ObjectSummary2.setLastModified(new Date());
        objectSummaryList.add(s3ObjectSummary2);

        when(s3client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(res);
        when(res.getObjectSummaries())
                .thenReturn(objectSummaryList);

        // Create mock ODT file in the right directory
        testGapIDFile = new File("/tmp/testGapID.odt");
        FileWriter myWriter = new FileWriter("/tmp/testGapID.odt");
        myWriter.write("Test gap id odt file");
        myWriter.close();

        // Creating this file manually, as the createZip method does not generate it due to the reliance on an s3Client method
        new File("/tmp/some/random/prefix").mkdirs();
        testHelloWorldFile1 = new File("/tmp/some/random/prefix/hello-world1.txt");
        testHelloWorldFile2 = new File("/tmp/some/random/prefix/hello-world2.txt");
        myWriter = new FileWriter("/tmp/some/random/prefix/hello-world1.txt");
        myWriter.write("Test hello world 1 file");
        myWriter = new FileWriter("/tmp/some/random/prefix/hello-world2.txt");
        myWriter.write("Test hello world 2 file");
        myWriter.close();
    }

    @AfterEach
    void afterEach() {
        testGapIDFile.delete();
        testHelloWorldFile1.delete();
        testHelloWorldFile2.delete();
        new File("/tmp/submission.zip").delete();
    }

    @Test
    void createZip_zipExists() throws Exception {
        ZipService.createZip(s3client, "testGapID", "testApplicationId", "testSubmissionId");

        assertTrue(Files.exists(Path.of("/tmp/submission.zip")));
    }

    @Test
    void createZip_zippedFilesExist() throws Exception {
        ZipService.createZip(s3client, "testGapID", "testApplicationId", "testSubmissionId");

        final String fileZip = "/tmp/submission.zip";
        try(final ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip))) {
            final ZipEntry helloWorldFile = zis.getNextEntry();
            final ZipEntry generatedODTFile = zis.getNextEntry();
            zis.closeEntry();

            assertEquals("hello-world2_1.txt", helloWorldFile.getName());
            assertEquals("testGapID_2.odt", generatedODTFile.getName());
        }
    }

    @Test
    void createZip_zipFilesContentMatches() throws Exception {
        ZipService.createZip(s3client, "testGapID", "testApplicationId", "testSubmissionId");

        final String fileZip = "/tmp/submission.zip";
        try(final ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip))) {
            zis.getNextEntry();
            final File unzippedHelloWorld1File = unzipFile(zis, "/tmp/some/random/prefix/hello-world1.txt");
            zis.getNextEntry();
            final File unzippedHelloWorld2File = unzipFile(zis, "/tmp/some/random/prefix/hello-world2.txt");
            zis.getNextEntry();
            final File unzippedODTFile = unzipFile(zis, "/tmp/testGapID.odt");

            assertTrue(FileUtils.contentEquals(testHelloWorldFile1, unzippedHelloWorld1File), "The files differ!");
            assertTrue(FileUtils.contentEquals(testHelloWorldFile2, unzippedHelloWorld2File), "The files differ!");
            assertTrue(FileUtils.contentEquals(testGapIDFile, unzippedODTFile), "The files differ!");
        }
    }

    @Test
    void createZip_onlyTwoFilesCreated() throws Exception {
        ZipService.createZip(s3client, "testGapID", "testApplicationId", "testSubmissionId");

        final String fileZip = "/tmp/submission.zip";
        try(final ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip))) {
            zis.getNextEntry();
            zis.getNextEntry();

            assertNull(zis.getNextEntry());

            zis.closeEntry();
        }
    }

    @Test
    void getSubmissionAttachmentFileNames() {
        final List<String> result = ZipService.getSubmissionAttachmentFileNames(s3client, "testApplicationId", "testSubmissionId");

        assertEquals(1, result.size());
        assertEquals("/some/random/prefix/hello-world2.txt", result.get(0));
    }

    @Test
    void shouldHandleMultiplePeriodsInFilename() {
        String result = ZipService.parseFileName("330/submission/folder/file.odt.w..pdf", 1, "330","submission");

        assertEquals("file.odt.w._1.pdf", result);
    }
    @Test
    void shouldHandleFileNameThatAreNotInTheRegex() {
        String result = ZipService.parseFileName("330/submission/folder/file.word", 1, "330","submission");

        assertEquals("file_1.word", result);
    }

    @Test
    void shouldHandleFileNameThatAreNotInTheRegexWithMoreDots() {
        String result = ZipService.parseFileName("330/submission/folder/file.a.b.c.d.word", 1, "330","submission");

        assertEquals("file.a.b.c.d_1.word", result);
    }

    @Test
    void shouldReturnFileNameWithSuffix() {
        String result = ZipService.parseFileName("330/submission/folder/file.pdf", 1, "330","submission");

        assertEquals("file_1.pdf", result);
    }

    @Test
    void shouldReturnFileNameWithSuffixWhenFileNameHasLoadsOfSpecialCharacter() {
        String result = ZipService.parseFileName("330/submission/folder//test... /File {{{}}} Name.???FLL. odt.<>\"/\\|?*"+ "odt.xls", 1, "330","submission");

        assertEquals("_test... _File {{{}}} Name.___FLL. odt.________odt_1.xls", result);
    }

    private File unzipFile(final ZipInputStream zis, final String filePath) throws Exception {
        final byte[] buffer = new byte[1024];
        final File unzippedFile = new File(filePath);

        try(final FileOutputStream fos = new FileOutputStream(unzippedFile)) {
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }

        return unzippedFile;
    }
}
