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
        // TODO: create executor with given thread count
        this.executor = new TiredExecutor(numThreads);
    }

    public ComputationNode run(ComputationNode computationRoot) {
        // TODO: resolve computation tree step by step until final matrix is produced
        if (computationRoot == null) {
        throw new IllegalArgumentException("Computation root is null");
        }
        //computationRoot.associativeNesting();
        // FIX: Call the recursive helper instead of the single node method
        recursiveAssociativeNesting(computationRoot);

        //applyAssociativeNestingForMultiply(computationRoot);
        // needs to keep resolving until the root becomes a matrix
        while (computationRoot.getNodeType() != ComputationNodeType.MATRIX) {

            ComputationNode nodeToResolve = computationRoot.findResolvable();

            if (nodeToResolve == null) {
                throw new IllegalStateException("no resolvable node");
            }

            loadAndCompute(nodeToResolve);
        }

        /* try{
          executor.shutdown();
        }
        catch(InterruptedException e){} */
    
        return computationRoot;
    }

    // public void loadAndCompute(ComputationNode node) {
    //     // TODO: load operand matrices
    //     // TODO: create compute tasks & submit tasks to executor
    //     validateTaskDimensions(node);
    //     ComputationNodeType type = node.getNodeType();
    //     List<ComputationNode> children = node.getChildren();

    //     double[][] left = children.get(0).getMatrix();
    //     leftMatrix.loadRowMajor(left);
        

    //     if(children.size() > 1){
    //         double[][] right = children.get(1).getMatrix();
    //         rightMatrix.loadRowMajor(right);
    //     }

    //     List<Runnable> tasks;
        
    //     switch (type) {
    //         case ADD:
    //             tasks = createAddTasks();
    //             break;
    //         case MULTIPLY:
    //             tasks = createMultiplyTasks();
    //             break;
    //         case NEGATE:
    //             tasks = createNegateTasks();
    //             break;
    //         case TRANSPOSE:
    //             tasks = createTransposeTasks();
    //             break;
        
    //         default:
    //             throw new IllegalArgumentException("Unknown/unsupported computation type " + type);
    //     }
        
    //     executor.submitAll(tasks);
    //     // try {
    //     //      executor.shutdown();
    //     // } catch (InterruptedException e) {
    //     //     throw new RuntimeException("Executor shutdown interrupted");
    //     // }

    //     double[][] result = leftMatrix.readRowMajor();
    //     node.resolve(result);

    // }
    public void loadAndCompute(ComputationNode node) {
    //node.associativeNesting();
    validateTaskDimensions(node);
    ComputationNodeType type = node.getNodeType();
    List<ComputationNode> children = node.getChildren();

    // 1. Deep Copy the Left Matrix to prevent "Dirty Memory" accumulation
    double[][] originalLeft = children.get(0).getMatrix();
    double[][] deepCopyLeft = new double[originalLeft.length][];
    for (int i = 0; i < originalLeft.length; i++) {
        // This ensures M1 gets a fresh memory address
        deepCopyLeft[i] = originalLeft[i].clone(); 
    }
    leftMatrix.loadRowMajor(deepCopyLeft);

    // 2. Load Right Matrix if it exists
    if (children.size() > 1) {
        rightMatrix.loadRowMajor(children.get(1).getMatrix());
    } else {
        rightMatrix.loadRowMajor(new double[0][0]);
    }

    // 3. Your Switch Statement (Now Compiling)
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

    // 4. Submit and Resolve
    executor.submitAll(tasks);
    node.resolve(leftMatrix.readRowMajor());
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

    public void shutdown() throws InterruptedException{
        executor.shutdown();
    }

    private void validateTaskDimensions(ComputationNode node) {
    ComputationNodeType type = node.getNodeType();
    List<ComputationNode> children = node.getChildren();

    // ADD: all matrices must have identical dimensions
    if (type == ComputationNodeType.ADD) {
        for (int i = 1; i < children.size(); i++) {
            double[][] a = children.get(0).getMatrix();
            double[][] b = children.get(i).getMatrix();

            if (a.length != b.length ||
                (a.length > 0 && a[0].length != b[0].length)) {
                throw new IllegalArgumentException("Illegal operation: dimensions mismatch");
            }
        }
    }

    // MULTIPLY: left columns must match right rows
    if (type == ComputationNodeType.MULTIPLY) {
        double[][] left = children.get(0).getMatrix();
        double[][] right = children.get(1).getMatrix();

        if (left.length > 0 && left[0].length != right.length) {
            throw new IllegalArgumentException("Illegal operation: dimensions mismatch");
        }
    }

    // UNARY operators
    if (type == ComputationNodeType.NEGATE || type == ComputationNodeType.TRANSPOSE) {
        if (children.size() != 1) {
            throw new IllegalArgumentException(
                "Unary operator " + type + " requires exactly 1 operand"
            );
        }
    }
    }

    // NEW HELPER METHOD: Recursively applies associative nesting to the whole tree
    private void recursiveAssociativeNesting(ComputationNode node) {
        if (node.getNodeType() == ComputationNodeType.MATRIX) {
            return;
        }

        // 1. Fix the current node's structure (e.g., A+B+C -> (A+B)+C)
        // This modifies the children list of 'node' in place.
        node.associativeNesting();

        // 2. Recursively fix all children.
        // We must do this because node.associativeNesting() stops at binary nodes 
        // and doesn't look deep enough into pre-existing children.
        if (node.getChildren() != null) {
            for (ComputationNode child : node.getChildren()) {
                recursiveAssociativeNesting(child);
            }
        }
    }





}
