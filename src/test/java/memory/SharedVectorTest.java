package memory;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

public class SharedVectorTest {

    @Test
    public void testBasicVectorOperations() {
        double[] data1 = {1.0, 2.0, 3.0};
        double[] data2 = {4.0, 5.0, 6.0};
        
        SharedVector v1 = new SharedVector(data1, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(data2, VectorOrientation.ROW_MAJOR);

        // (1*4 + 2*5 + 3*6) = 32
        assertEquals(32.0, v1.dot(v2), 0.0001, "Dot product calculation is incorrect.");

        v1.add(v2);
        assertArrayEquals(new double[]{5.0, 7.0, 9.0}, new double[]{v1.get(0), v1.get(1), v1.get(2)}, 0.0001);

        v1.negate();
        assertArrayEquals(new double[]{-5.0, -7.0, -9.0}, new double[]{v1.get(0), v1.get(1), v1.get(2)}, 0.0001);
    }

    @Test
    public void testTranspose() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        
        v.transpose();
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
        
        v.transpose();
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
    }

    @Test
    public void testIllegalAddition() {
        SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);

        // should throw exception if dimentions don't match
        assertThrows(IllegalArgumentException.class, () -> v1.add(v2));
    }


    @Test
    public void testVecMatMulIdentity() {
        double[] vecData = {1.0, 2.0};
   
        double[][] matData = {
            {1.0, 0.0},
            {0.0, 1.0}
        };
        
        SharedVector vector = new SharedVector(vecData, VectorOrientation.ROW_MAJOR);
        SharedMatrix matrix = new SharedMatrix(matData);
        
        vector.vecMatMul(matrix);
        
        assertEquals(2, vector.length(), "Vector length should remain 2");
        assertEquals(1.0, vector.get(0), 0.0001, "First element should be 1.0");
        assertEquals(2.0, vector.get(1), 0.0001, "Second element should be 2.0");
        assertEquals(VectorOrientation.ROW_MAJOR, vector.getOrientation(), "Orientation should be ROW_MAJOR");
    }



    @Test
    public void testVecMatMulResizing() {
        
        SharedVector v = new SharedVector(new double[]{1, 1, 1}, VectorOrientation.ROW_MAJOR);
    
        double[][] matData = {
            {1, 2},
            {1, 2},
            {1, 2}
        };
        SharedMatrix m = new SharedMatrix(matData);

        v.vecMatMul(m);

        assertEquals(2, v.length(), "Vector should have resized from 3 to 2");
        assertEquals(3.0, v.get(0), 0.0001); // 1+1+1
        assertEquals(6.0, v.get(1), 0.0001); // 2+2+2
    }

    @Test
    public void testDotProductOrthogonal() {
        // result should be 0.0
        SharedVector v1 = new SharedVector(new double[]{1, 0}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{0, 1}, VectorOrientation.ROW_MAJOR);

        assertEquals(0.0, v1.dot(v2), 0.0001);
    }

    @Test
    public void testAddNullThrowsException() {
        // should throw exception because it's trying to add null
        SharedVector v1 = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> v1.add(null));
    }
}