package spl.lae;

import parser.*;
import scheduling.*;
import memory.*;

import java.util.List;

public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        // TODO: create executor with given thread count
        this.executor = new TiredExecutor(numThreads);
    }

    public ComputationNode run(ComputationNode computationRoot) {
        // TODO: resolve computation tree step by step until final matrix is produced
        
        if (computationRoot == null) {
        throw new IllegalArgumentException("Computation root is null");
        }

        // needs to keep resolving until the root becomes a matrix
        while (computationRoot.getNodeType() != ComputationNodeType.MATRIX) {

            ComputationNode nodeToResolve = computationRoot.findResolvable();

            if (nodeToResolve == null) {
                throw new IllegalStateException("no resolvable node");
            }

            loadAndCompute(nodeToResolve);
        }

        return computationRoot;
    }

    public void loadAndCompute(ComputationNode node) {
        // TODO: load operand matrices
        // TODO: create compute tasks & submit tasks to executor
    }

    public List<Runnable> createAddTasks() {
        // TODO: return tasks that perform row-wise addition
        int rows = leftMatrix.length();

        List<Runnable> tasks = new java.util.ArrayList<>();

        for (int i = 0; i < rows; i++) {
            final int row = i;

            tasks.add(() -> {
                SharedVector leftRow  = leftMatrix.get(row);
                SharedVector rightRow = rightMatrix.get(row);


                leftRow.add(rightRow);
            });
        }

        return tasks;
    }

    public List<Runnable> createMultiplyTasks() {
        // TODO: return tasks that perform row × matrix multiplication
        int rows = leftMatrix.length();
        List<Runnable> tasks = new java.util.ArrayList<>();

        for (int i = 0; i < rows; i++) {
            final int row = i;

            tasks.add(() -> {
                SharedVector rowVector = leftMatrix.get(row);

                rowVector.vecMatMul(rightMatrix);
            });
        }

        return tasks;
    }

    public List<Runnable> createNegateTasks() {
        // TODO: return tasks that negate rows
        int rows = leftMatrix.length();
        List<Runnable> tasks = new java.util.ArrayList<>();

        for (int i = 0; i < rows; i++) {
            final int row = i;

            tasks.add(() -> {
                SharedVector vec = leftMatrix.get(row);
                vec.negate();  // locking handled inside SharedVector
            });
        }

        return tasks;
    }

    public List<Runnable> createTransposeTasks() {
        // TODO: return tasks that transpose rows
        return null;
    }

    public String getWorkerReport() {
        // TODO: return summary of worker activity
        return null;
    }
}
