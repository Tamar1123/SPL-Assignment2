package memory;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

public class SharedMatrixTest {

    @Test
    public void testLoadAndReadRowMajor() {
        double[][] input = {
            {1.0, 2.0, 3.0},
            {4.0, 5.0, 6.0}
        };
        SharedMatrix matrix = new SharedMatrix(input);
        
        double[][] output = matrix.readRowMajor();
        
        assertEquals(input.length, output.length);
        for (int i = 0; i < input.length; i++) {
            assertArrayEquals(input[i], output[i], 0.0001);
        }
    }

    @Test
    public void testLoadColumnMajor() {
        double[][] input = {
            {1.0, 2.0},
            {3.0, 4.0},
            {5.0, 6.0}
        };
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(input);

        // Even if loaded as column-major, readRowMajor should reconstruct the original 2D array
        double[][] output = matrix.readRowMajor();
        
        assertEquals(input.length, output.length);
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.get(0).getOrientation());
        for (int i = 0; i < input.length; i++) {
            assertArrayEquals(input[i], output[i]);
        }
    }

    @Test
    public void testEmptyMatrix() {
        SharedMatrix matrix = new SharedMatrix(new double[0][0]);
        assertEquals(0, matrix.length());
        assertArrayEquals(new double[0][0], matrix.readRowMajor());
    }
    
    @Test
    public void testOrientationSwitchDuringUpdate() {
        // 1. Start as Row Major
        double[][] data = {{1, 2}, {3, 4}};
        SharedMatrix matrix = new SharedMatrix(data);
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());

        // 2. Overwrite with Column Major data
        matrix.loadColumnMajor(data);

        // 3. Verify orientation changed
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
        
        // 4. Verify data integrity is maintained despite orientation change
        double[][] output = matrix.readRowMajor();
        assertArrayEquals(data[0], output[0], 0.0001);
    }

    @Test
    public void testDataUpdateAfterInitialization() {
        // 1. Initialize with original data
        double[][] originalData = {
            {1.0, 1.0},
            {1.0, 1.0}
        };
        SharedMatrix matrix = new SharedMatrix(originalData);
        
        // Verify initial state
        assertArrayEquals(originalData[0], matrix.readRowMajor()[0], 0.0001);

        // 2. Upload new data to the same matrix instance
        double[][] newData = {
            {9.9, 8.8},
            {7.7, 6.6}
        };
        matrix.loadRowMajor(newData);

        // 3. Verify the internal vectors were replaced
        double[][] currentData = matrix.readRowMajor();
        assertEquals(9.9, currentData[0][0], 0.0001);
        assertEquals(6.6, currentData[1][1], 0.0001);
        assertNotEquals(1.0, currentData[0][0]);
    }
}