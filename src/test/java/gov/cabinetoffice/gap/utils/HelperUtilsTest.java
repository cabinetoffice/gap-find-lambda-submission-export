package gov.cabinetoffice.gap.utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelperUtilsTest {

    @Nested
    class getRedirectUrl {

        @Test
        void successfullyGenerateURLUsingDomain() {

            String url = HelperUtils.getRedirectUrl("12345", "12345-6789-12345");

            assertEquals("test.co.uk/apply/testing/scheme/12345/12345-6789-12345", url);

        }

    }

    @Nested
    class generateFilename {

        @Test
        void successfullyGenerateFilename() {

            String generatedFilename = HelperUtils.generateFilename("Totally Legit Org", "GAP-GAP-20221019");

            assertThat(generatedFilename).isEqualTo("Totally_Legit_Org_GAP_GAP_20221019");

        }

        @Test
        void specialCharactersAreReplaced() {
            String generatedFilename = HelperUtils.generateFilename("Org<with>lots/of\\special*characters?in_its name", "GAP-GAP-20221019");

            assertThat(generatedFilename).isEqualTo("Org_with_lots_of_special_characters_in_its_name_GAP_GAP_20221019");
        }

        @Test
        void legalNamesLongerThan50CharsAreTrimmed() {
            final String longLegalName = "a legal name with more than fifty characters that needs to be trimmed by the document export lambda so that file name length limits in windows don't cause problems";
            final String trimmedLegalName = longLegalName.substring(0, 50)
                    .trim()
                    .replace(" ", "_");

            String generatedFilename = HelperUtils.generateFilename(longLegalName, "GAP-GAP-20221019");

            assertThat(generatedFilename).isEqualTo(trimmedLegalName + "_GAP_GAP_20221019");
        }

        @Test
        void nullLegalNameGenerateFilename() {

            assertThatThrownBy(() -> HelperUtils.generateFilename(null, "GAP-GAP-20221019"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("legalName and gapId cannot be null");

        }

        @Test
        void nullGapIdGenerateFilename() {

            assertThatThrownBy(() -> HelperUtils.generateFilename("Totally Legit Org", null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("legalName and gapId cannot be null");

        }

    }

}
