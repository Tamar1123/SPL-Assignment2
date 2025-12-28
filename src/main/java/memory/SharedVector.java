package memory;

import java.util.concurrent.locks.ReadWriteLock;

public class SharedVector {

    private double[] vector;
    private VectorOrientation orientation;
    private ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public SharedVector(double[] vector, VectorOrientation orientation) {
        this.vector = vector;
        this.orientation = orientation;
    }

    public double get(int index) {
        readLock();
        try {
        return vector[index];
        } 
        finally {
            readUnlock();
        }
    }

    public int length() {
       
        readLock();
        try {
            return vector.length;
        } finally {
            readUnlock();
        }
    }

    public VectorOrientation getOrientation() {
        
        readLock();
        try {
            return orientation;
        } finally {
            readUnlock();
        }
    }

    public void writeLock() {
        
        lock.writeLock().lock();

    }

    public void writeUnlock() {
        
        lock.writeLock().unlock();
    }

    public void readLock() {
        
        lock.readLock().lock();
    }

    public void readUnlock() {
        
        lock.readLock().unlock();
    }

    public void transpose() {
        
        writeLock();
        try {
        if (orientation == VectorOrientation.ROW_MAJOR) {
            orientation = VectorOrientation.COLUMN_MAJOR;
        } else {
            orientation = VectorOrientation.ROW_MAJOR;
        }
        } finally {
            writeUnlock();
        }
    }

    public void add(SharedVector other) {
        
        if (other == null) throw new IllegalArgumentException("other is null");
        if (this.length() != other.length())
            throw new IllegalArgumentException("add requires vectors of equal length");

        // We lock other for reading first, then lock this for writing
        other.readLock();
        this.writeLock();

        try {
            for (int i = 0; i < vector.length; i++) {
                vector[i] += other.vector[i]; // read is safe because other is read-locked
            }
        } finally {
            this.writeUnlock();
            other.readUnlock();
        }
    }

    public void negate() {
        
        writeLock();
        try {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = -vector[i];
            }
        } finally {
            writeUnlock();
        }
    }

    public double dot(SharedVector other) {
        if (other == null) {
            throw new IllegalArgumentException("other is null");
        }

        // we lock other first, then this
        other.readLock();
        this.readLock();

        try {
            if (this.vector.length != other.vector.length) {
                throw new IllegalArgumentException("Dot product requires equal lengths of vectors");
            }

            double sum = 0.0;
            for (int i = 0; i < this.vector.length; i++) {
                sum += this.vector[i] * other.vector[i];
            }
            return sum;

        } finally {
            this.readUnlock();
            other.readUnlock();
        }
    }

    public void vecMatMul(SharedMatrix matrix) {
        // TODO: compute row-vector × matrix

        if (matrix == null) {
            throw new IllegalArgumentException("matrix is null");
        }

        // row-vector × matrix
        if (this.getOrientation() != VectorOrientation.ROW_MAJOR) {
            throw new IllegalStateException("vecMatMul requires this vector to be ROW_MAJOR");
        }

        // copy the vector
        double[] vecCopy;
        readLock();
        try {
            vecCopy = this.vector.clone();
        } finally {
            readUnlock();
        }

        // read matrix as row major
        double[][] mat = matrix.readRowMajor();

        // dimension checks
        if (mat.length != vecCopy.length) {
            throw new IllegalArgumentException(
                "Dimension mismatch: vector length=" + vecCopy.length + " but matrix rows=" + mat.length
            );
        }

        if (mat.length == 0) {
            writeLock();
            try {
                this.vector = new double[0];
                this.orientation = VectorOrientation.ROW_MAJOR;
            } finally {
                writeUnlock();
            }
            return;
        }

        int cols = mat[0].length;
        double[] result = new double[cols];

        for (int j = 0; j < cols; j++) {
            double sum = 0.0;
            for (int i = 0; i < mat.length; i++) {
                sum += vecCopy[i] * mat[i][j];
            }
            result[j] = sum;
        }

        
        writeLock();
        try {
            this.vector = result;
            this.orientation = VectorOrientation.ROW_MAJOR;
        } finally {
            writeUnlock();
        }
    }
}
