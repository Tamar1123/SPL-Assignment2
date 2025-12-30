package spl;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test; // Matches your provided package spl.lae
import org.junit.jupiter.api.io.TempDir;

import spl.lae.Main;

public class MainTest {

    @TempDir
    Path tempDir;

    @Test
    public void testFullExecutionSuccess() throws Exception {
        // Valid JSON input
        String inputJson = "{\"operator\": \"+\", \"operands\": [[[1.0, 1.0]], [[2.0, 2.0]]]}";
        Path inputPath = tempDir.resolve("input.json");
        Path outputPath = tempDir.resolve("output.json");
        Files.writeString(inputPath, inputJson);

        String[] args = {"4", inputPath.toString(), outputPath.toString()};
        Main.main(args);

        assertTrue(Files.exists(outputPath), "Output file should exist.");
        String content = Files.readString(outputPath);
        assertTrue(content.contains("\"result\""), "Should have 'result' key.");
        assertTrue(content.contains("3.0"), "1.0 + 2.0 should be 3.0.");
    }

    @Test
    public void testDimensionMismatchError() throws Exception {
        // Invalid input: adding 1x2 matrix to 1x1 matrix
        String inputJson = "{\"operator\": \"+\", \"operands\": [[[1.0, 2.0]], [[5.0]]]}";
        Path inputPath = tempDir.resolve("error_input.json");
        Path outputPath = tempDir.resolve("error_output.json");
        Files.writeString(inputPath, inputJson);

        String[] args = {"2", inputPath.toString(), outputPath.toString()};
        Main.main(args);

        assertTrue(Files.exists(outputPath));
        String content = Files.readString(outputPath);
  
        assertTrue(content.contains("\"error\""), "Output should contain an error message.");
    }

    @Test
    public void testNaryAdditionPersistence() throws Exception {
        String inputJson = "{\"operator\": \"+\", \"operands\": [[[1.0]], [[1.0]], [[1.0]]]}";
        Path inputPath = tempDir.resolve("nary_input.json");
        Path outputPath = tempDir.resolve("nary_output.json");
        Files.writeString(inputPath, inputJson);

        String[] args = {"4", inputPath.toString(), outputPath.toString()};
        Main.main(args);

        String content = Files.readString(outputPath);
        
        assertTrue(content.contains("3.0"), "N-ary addition result should be 3.0.");
        assertFalse(content.contains("4.0"), "Bug detected: intermediate results leaked into final sum.");
    }
}