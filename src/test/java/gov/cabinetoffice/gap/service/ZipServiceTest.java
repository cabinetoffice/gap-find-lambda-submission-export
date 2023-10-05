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
    private final AmazonS3 client = Mockito.mock(AmazonS3.class);

    private File testGapIDFile;
    private File testHelloWorldFile;

    @BeforeEach
    void beforeEach() throws Exception {
        // Mock list objects v2 to return a single file named "hello-world.txt"
        final ListObjectsV2Result res = Mockito.mock(ListObjectsV2Result.class);
        final List<S3ObjectSummary> objectSummaryList = new ArrayList<>();
        final S3ObjectSummary s3ObjectSummary = new S3ObjectSummary();
        s3ObjectSummary.setKey("hello-world.txt");
        s3ObjectSummary.setLastModified(new Date());
        objectSummaryList.add(s3ObjectSummary);
        when(client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(res);
        when(res.getObjectSummaries())
                .thenReturn(objectSummaryList);

        // Create mock ODT file in the right directory
        testGapIDFile = new File("/tmp/testGapID.odt");
        FileWriter myWriter = new FileWriter("/tmp/testGapID.odt");
        myWriter.write("Test gap id odt file");
        myWriter.close();

        // Creating this file manually, as the createZip method does not generate it due to the reliance on an s3Client method
        testHelloWorldFile = new File("/tmp/hello-world.txt");
        myWriter = new FileWriter("/tmp/hello-world.txt");
        myWriter.write("Test hello world file");
        myWriter.close();
    }

    @AfterEach
    void afterEach() {
        testGapIDFile.delete();
        testHelloWorldFile.delete();
        new File("/tmp/submission.zip").delete();
    }

    @Test
    void createZip_zipExists() throws Exception {
        ZipService.createZip(client, "testGapID", "testApplicationId", "testSubmissionId");

        assertTrue(Files.exists(Path.of("/tmp/submission.zip")));
    }

    @Test
    void createZip_zippedFilesExist() throws Exception {
        ZipService.createZip(client, "testGapID", "testApplicationId", "testSubmissionId");

        final String fileZip = "/tmp/submission.zip";
        try(final ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip))) {
            final ZipEntry helloWorldFile = zis.getNextEntry();
            final ZipEntry generatedODTFile = zis.getNextEntry();
            zis.closeEntry();

            assertEquals("hello-world_1.txt", helloWorldFile.getName());
            assertEquals("testGapID_2.odt", generatedODTFile.getName());
        }
    }

    @Test
    void createZip_zipFilesContentMatches() throws Exception {
        ZipService.createZip(client, "testGapID", "testApplicationId", "testSubmissionId");

        final String fileZip = "/tmp/submission.zip";
        try(final ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip))) {
            zis.getNextEntry();
            final File unzippedHelloWorldFile = unzipFile(zis, "/tmp/hello-world.txt");
            zis.getNextEntry();
            final File unzippedODTFile = unzipFile(zis, "/tmp/testGapID.odt");

            assertTrue(FileUtils.contentEquals(testHelloWorldFile, unzippedHelloWorldFile), "The files differ!");
            assertTrue(FileUtils.contentEquals(testGapIDFile, unzippedODTFile), "The files differ!");
        }
    }

    @Test
    void createZip_onlyTwoFilesCreated() throws Exception {
        ZipService.createZip(client, "testGapID", "testApplicationId", "testSubmissionId");

        final String fileZip = "/tmp/submission.zip";
        try(final ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip))) {
            zis.getNextEntry();
            zis.getNextEntry();

            assertNull(zis.getNextEntry());

            zis.closeEntry();
        }
    }

    @Test
    void shouldReturnLastPartOfSubStringWhenFileNameIsParsed() {
        String result = ZipService.parseFileName("s3://gap-attachments" +
                "/13" +
                "/035f31f6-2877-4f87-8a9b-32649a527668" +
                "/9d4a3ed6-5a26-4d02-be49-bab13c11a2bf" +
                "/test File Name.odt", 1);

        assertEquals("test File Name_1.odt", result);
    }

    @Test
    void shouldHandleMultiplePeriodsInFilename() {
        String result = ZipService.parseFileName("s3://gap-attachments" +
                "/13" +
                "/035f31f6-2877-4f87-8a9b-32649a527668" +
                "/9d4a3ed6-5a26-4d02-be49-bab13c11a2bf" +
                "/test... File Name.FLL. odt.odt.blarg", 1);

        assertEquals("test File NameFLL odtodt_1.blarg", result);
    }

    @Test
    void shouldStripSpecialCharsFromFilename() {
        String result = ZipService.parseFileName("s3://gap-attachments" +
                "/13" +
                "/035f31f6-2877-4f87-8a9b-32649a527668" +
                "/9d4a3ed6-5a26-4d02-be49-bab13c11a2bf" +
                "/test... File {{{}}} Name.???FLL. odt.&*%*$odt.xls", 1);

        assertEquals("test File  NameFLL odtodt_1.xls", result);
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
