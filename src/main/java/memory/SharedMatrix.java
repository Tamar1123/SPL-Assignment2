package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        this.vectors = new SharedVector[0];

    }

    public SharedMatrix(double[][] matrix) {
        // TODO: construct matrix as row-major SharedVectors
        loadRowMajor(matrix);
   }

    public void loadRowMajor(double[][] matrix) {
        // TODO: replace internal data with new row-major matrix
        int rows = matrix.length;
        SharedVector[] newVectors = new SharedVector[rows];

        for (int i = 0; i < rows; i++) {
            newVectors[i] = new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
        }

        vectors = newVectors;
    }

    public void loadColumnMajor(double[][] matrix) {
        // TODO: replace internal data with new column-major matrix
       int rows = matrix.length;

        // Handling empty matrix
        if (rows == 0) {
            vectors = new SharedVector[0];
            return;
        }

        int cols = matrix[0].length;
        SharedVector[] newVectors = new SharedVector[cols];

        for (int j = 0; j < cols; j++) {
            double[] col = new double[rows];
            for (int i = 0; i < rows; i++) {
                col[i] = matrix[i][j];
            }
            newVectors[j] = new SharedVector(col, VectorOrientation.COLUMN_MAJOR);
        }

        vectors = newVectors;

    }

    public double[][] readRowMajor() {
        // TODO: return matrix contents as a row-major double[][]
            
        if (vectors.length == 0) {
            return new double[0][0];
        }

        SharedVector[] vecs = vectors;

        VectorOrientation orientation = vecs[0].getOrientation();
        int rows, cols;

        if (orientation == VectorOrientation.ROW_MAJOR) {
            rows = vecs.length;
            cols = vecs[0].length();
        } 
        else {
            rows = vecs[0].length();
            cols = vecs.length;
        }

        double[][] result = new double[rows][cols];

        acquireAllVectorReadLocks(vecs);
        try {
            if (orientation == VectorOrientation.ROW_MAJOR) {
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        result[i][j] = vecs[i].get(j);
                    }
                }
            } 
            else { // COLUMN_MAJOR
                for (int j = 0; j < cols; j++) {
                    for (int i = 0; i < rows; i++) {
                        result[i][j] = vecs[j].get(i);
                    }
                }
            }
        } finally {
            releaseAllVectorReadLocks(vecs);
        }

        return result;

    }

    public SharedVector get(int index) {
        // TODO: return vector at index
        return vectors[index];
    }

    public int length() {
        // TODO: return number of stored vectors
        return vectors.length;
    }

    public VectorOrientation getOrientation() {
        // TODO: return orientation
        if (vectors.length == 0) {
        return VectorOrientation.ROW_MAJOR; // arbitrary default for empty
        }
        return vectors[0].getOrientation();
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: acquire read lock for each vector
        for (int i = 0; i < vecs.length; i++) {
        vecs[i].readLock();
        }
    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: release read locks
        for (int i = vecs.length - 1; i >= 0; i--) {
        vecs[i].readUnlock();
        }
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: acquire write lock for each vector
        for (int i = 0; i < vecs.length; i++) {
        vecs[i].writeLock();
        }
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: release write locks
        for (int i = vecs.length - 1; i >= 0; i--) {
        vecs[i].writeUnlock();
        }
    }
}
