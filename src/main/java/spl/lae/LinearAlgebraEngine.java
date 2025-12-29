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
        ComputationNodeType type = node.getNodeType();
        List<ComputationNode> children = node.getChildren();

        double[][] left = children.get(0).getMatrix();
        leftMatrix.loadRowMajor(left);

        if(children.size() > 1){
            double[][] right = children.get(1).getMatrix();
            rightMatrix.loadRowMajor(right);
        }

        switch (type) {
            case ADD:
                createAddTasks();
                break;
            case MULTIPLY:
                createMultiplyTasks();
                break;
            case NEGATE:
                createNegateTasks();
                break;
            case TRANSPOSE:
                createTransposeTasks();
        
            default:
                throw new IllegalArgumentException("Unknown/unsupported computation type " + type);
        }


        List<Runnable> tasks = null;

        executor.submitAll(tasks);
        /* while(true){
            try{
                executor.shutdown();
                break;
            }
            catch(new InterruptedException());
        } */

        double[][] result = leftMatrix.readRowMajor();
        node.resolve(result);

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
        int rows = leftMatrix.length();
        List<Runnable> tasks = new java.util.ArrayList<>();

        for (int i = 0; i < rows; i++){
            final int row = i;

            tasks.add(() -> {
                SharedVector vec = leftMatrix.get(row);
                vec.transpose();
            });
        }
        return tasks;
    }

    public String getWorkerReport() {
        // TODO: return summary of worker activity
        StringBuilder sb = new StringBuilder();
        sb.append("Worker Report:\n");

        double[][] l = leftMatrix.readRowMajor();
        sb.append(String.format("Left matrix: %d x %d%n", l.length, (l.length==0?0:l[0].length)));
        
        double[][] r = rightMatrix.readRowMajor();
        sb.append(String.format("Right matrix: %d x %d%n", r.length, (r.length==0?0:r[0].length)));
        
        if (executor == null) {
            sb.append("No executor available\n");
        } 
        else {
            sb.append(executor.getWorkerReport()); // reuse the executor's detailed lines
        }
        return sb.toString();
        
    }
}
