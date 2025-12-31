package spl.lae;

import java.util.List;

import memory.SharedMatrix;
import memory.SharedVector;
import parser.ComputationNode;
import parser.ComputationNodeType;
import scheduling.TiredExecutor;

public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        this.executor = new TiredExecutor(numThreads);
    }

    public ComputationNode run(ComputationNode computationRoot) {
        if (computationRoot == null) {
            throw new IllegalArgumentException("Computation root is null");
        }

        recursiveAssociativeNesting(computationRoot);

        // needs to keep resolving until the root finally becomes a matrix
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

        validateTaskDimensions(node);
        ComputationNodeType type = node.getNodeType();
        List<ComputationNode> children = node.getChildren();

        // we deep copy the left matrix
        double[][] originalLeft = children.get(0).getMatrix();
        double[][] deepCopyLeft = new double[originalLeft.length][];
        for (int i = 0; i < originalLeft.length; i++) {
            deepCopyLeft[i] = originalLeft[i].clone(); 
        }
        leftMatrix.loadRowMajor(deepCopyLeft);

        // if right matrix exists, we load it
        if (children.size() > 1) {
            rightMatrix.loadRowMajor(children.get(1).getMatrix());
        } else {
            rightMatrix.loadRowMajor(new double[0][0]);
        }
       
        List<Runnable> tasks;
        switch (type) {
            case ADD:
                tasks = createAddTasks();
                break;
            case MULTIPLY:
                tasks = createMultiplyTasks();
                break;
            case NEGATE:
                tasks = createNegateTasks();
                break;
            case TRANSPOSE:
                tasks = createTransposeTasks();
                break;
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }

        executor.submitAll(tasks);
        node.resolve(leftMatrix.readRowMajor());
    }

    public List<Runnable> createAddTasks() {
        int rows = leftMatrix.length();

        List<Runnable> tasks = new java.util.ArrayList<>();

        for (int i = 0; i<rows; i++) {
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
        int rows = leftMatrix.length();
        List<Runnable> tasks = new java.util.ArrayList<>();

        for (int i = 0; i< rows; i++) {
            final int row = i;

            tasks.add(() -> {
                SharedVector rowVector = leftMatrix.get(row);

                rowVector.vecMatMul(rightMatrix);
            });
        }

        return tasks;
    }


    public List<Runnable> createNegateTasks() {
        int rows = leftMatrix.length();
        List<Runnable> tasks = new java.util.ArrayList<>();

        for (int i=0; i < rows; i++) {
            final int row = i;

            tasks.add(() -> {
                SharedVector vec = leftMatrix.get(row);
                vec.negate();  
            });
        }

        return tasks;
    }


    public List<Runnable> createTransposeTasks() {
        int rows = leftMatrix.length();
        List<Runnable> tasks = new java.util.ArrayList<>();

        for (int i = 0; i<rows; i++){
            final int row = i;

            tasks.add(() -> {
                SharedVector vec = leftMatrix.get(row);
                vec.transpose();
            });
        }
        return tasks;
    }


    public String getWorkerReport() {
        String report = "Worker Report:\n";

        double[][] l = leftMatrix.readRowMajor();
        int lCollums = 0;
        if (l.length > 0) {
            lCollums = l[0].length;
        }
        report += String.format("Left matrix: %d x %d%n", l.length, lCollums);

        double[][] r = rightMatrix.readRowMajor();
        int rCols = 0;
        if (r.length > 0) {
            rCols = r[0].length;
        }
        report += String.format("Right matrix: %d x %d%n", r.length, rCols);

        if (executor == null) {
            report += "No executor available\n";
        } 
        else {
            report += executor.getWorkerReport();
        }
        
        return report;
        
    }



    //HELPER FUNCTIONS
    private void validateTaskDimensions(ComputationNode node) {
        ComputationNodeType type = node.getNodeType();
        List<ComputationNode> children = node.getChildren();

        //ADD
        if (type == ComputationNodeType.ADD) {
            for (int i=1; i<children.size(); i++) {
                double[][] a = children.get(0).getMatrix();
                double[][] b = children.get(i).getMatrix();

                if (a.length != b.length ||
                    (a.length > 0 && a[0].length != b[0].length)) {
                    throw new IllegalArgumentException("Illegal operation: dimensions mismatch");
                }
            }
        }

        // MULTIPLY
        if (type == ComputationNodeType.MULTIPLY) {
            double[][] left = children.get(0).getMatrix();
            double[][] right = children.get(1).getMatrix();

            if (left.length > 0 && left[0].length != right.length) {
                throw new IllegalArgumentException("Illegal operation: dimensions mismatch");
            }
        }

        // NEGATE/TRANSPOSE
        if (type == ComputationNodeType.NEGATE || type == ComputationNodeType.TRANSPOSE) {
            if (children.size() != 1) {
                throw new IllegalArgumentException(
                    "Unary operator " + type + " needs only 1 operand"
                );
            }
        }
    }


    private void recursiveAssociativeNesting(ComputationNode node) {
        if (node.getNodeType() == ComputationNodeType.MATRIX) {
            return;
        }
        node.associativeNesting();

        if (node.getChildren() != null) {
            for (ComputationNode child : node.getChildren()) {
                recursiveAssociativeNesting(child);
            }
        }
    }


}
