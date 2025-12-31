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
        
        if (other == null) 
            throw new IllegalArgumentException("other is null");
        if (this.length() != other.length())
            throw new IllegalArgumentException("add requires both vectors to have the same length");

        other.readLock();
        this.writeLock();

        try {
            for (int i = 0; i < vector.length; i++) {
                vector[i] += other.vector[i];
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

        other.readLock();
        this.readLock();

        try {
            if (this.vector.length != other.vector.length) {
                throw new IllegalArgumentException("Dot product needs both vectors to have the same length");
            }

            double sum = 0.0;
            for (int i = 0; i <this.vector.length; i++) {
                sum += this.vector[i] * other.vector[i];
            }
            return sum;

        } finally {
            this.readUnlock();
            other.readUnlock();
        }
    }

    public void vecMatMul(SharedMatrix matrix) {

        if (matrix == null) {
            throw new IllegalArgumentException("matrix is null");
        }


        if (this.getOrientation() != VectorOrientation.ROW_MAJOR) {
            throw new IllegalStateException("vecMatMul needs this vector to be a ROW_MAJOR");
        }

        double[] vectorCopy;
        readLock();
        try {
            vectorCopy = this.vector.clone();
        } finally {
            readUnlock();
        }

        double[][] m = matrix.readRowMajor();

        if (m.length != vectorCopy.length) {
            throw new IllegalArgumentException(
                "Dimension mismatch: vector length=" + vectorCopy.length + " but matrix rows=" + m.length
            );
        }

        if (m.length == 0) {
            writeLock();
            try {
                this.vector = new double[0];
                this.orientation = VectorOrientation.ROW_MAJOR;
            } finally {
                writeUnlock();
            }
            return;
        }

        int cols = m[0].length;
        double[] result = new double[cols];

        for (int j = 0; j < cols; j++) {
            double sum = 0.0;
            for (int i = 0; i<m.length; i++) {
                sum += vectorCopy[i] * m[i][j];
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
