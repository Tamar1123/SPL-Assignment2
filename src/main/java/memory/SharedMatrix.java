package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        this.vectors = new SharedVector[0];
    }

    public SharedMatrix(double[][] matrix) {
        loadRowMajor(matrix);
   }

    public void loadRowMajor(double[][] matrix) {
        int rows = matrix.length;
        SharedVector[] newerVectors = new SharedVector[rows];

        for (int i = 0; i < rows; i++) {
            newerVectors[i] = new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
        }

        this.vectors = newerVectors;
    }


    public void loadColumnMajor(double[][] matrix) {
       int rows = matrix.length;

        // handle an empty matrix
        if (rows == 0) {
            vectors = new SharedVector[0];
            return;
        }

        int collums = matrix[0].length;
        SharedVector[] newerVectors = new SharedVector[collums];

        for (int j = 0; j < collums; j++) {
            double[] col = new double[rows];
            for (int i = 0; i < rows; i++) {
                col[i] = matrix[i][j];
            }
            newerVectors[j] = new SharedVector(col, VectorOrientation.COLUMN_MAJOR);
        }

        vectors = newerVectors;

    }

    

    public double[][] readRowMajor() {        
        if (vectors.length == 0) {
            return new double[0][0];
        }

        SharedVector[] vecs_array = vectors;

        VectorOrientation orientation = vecs[0].getOrientation();
        int rows, cols;

        if (orientation == VectorOrientation.ROW_MAJOR) {
            rows = vecs_array.length;
            cols = vecs_array[0].length();
        } 
        else {
            rows = vecs_array[0].length();
            cols = vecs_array.length;
        }

        double[][] result = new double[rows][cols];

        acquireAllVectorReadLocks(vecs_array);
        try {
            if (orientation == VectorOrientation.ROW_MAJOR) {
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        result[i][j] = vecs_array[i].get(j);
                    }
                }
            } 
            else { 
                for (int j = 0; j < cols; j++) {
                    for (int i = 0; i < rows; i++) {
                        result[i][j] = vecs_array[j].get(i);
                    }
                }
            }
        } finally {
            releaseAllVectorReadLocks(vecs_array);
        }

        return result;

    }

    public SharedVector get(int index) {
        return vectors[index];
    }

    public int length() {
        return vectors.length;
    }

    public VectorOrientation getOrientation() {
        if (vectors.length == 0) {
        return VectorOrientation.ROW_MAJOR; //if it's empty
        }
        return vectors[0].getOrientation();
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        for (int i = 0; i < vecs.length; i++) {
        vecs[i].readLock();
        }
    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        for (int i = vecs.length-1; i >= 0; i--) {
        vecs[i].readUnlock();
        }
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        for (int i=0; i <vecs.length; i++) {
        vecs[i].writeLock();
        }
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        for (int i = vecs.length-1; i >= 0; i--) {
        vecs[i].writeUnlock();
        }
    }
}
