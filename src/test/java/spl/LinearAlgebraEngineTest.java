package spl; // Updated to match the directory /src/test/java/spl/

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertArrayEquals; // This resolves the tempDir variable
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import parser.ComputationNode;
import parser.ComputationNodeType;
import spl.lae.LinearAlgebraEngine;


public class LinearAlgebraEngineTest {

    private LinearAlgebraEngine lae;

    @BeforeEach
    public void setUp() {
        lae = new LinearAlgebraEngine(4);
    }

    @AfterEach
    public void shutDown() throws InterruptedException {
        // Ensure the executor is shut down cleanly after each test
        if (lae != null) {
            lae.shutdown();
        }
    }

    @Test
    public void testBasicAddition() {
        double[][] m1 = {{1, 2}, {3, 4}};
        double[][] m2 = {{5, 6}, {7, 8}};
        
        ComputationNode node1 = new ComputationNode(m1);
        ComputationNode node2 = new ComputationNode(m2);
        ComputationNode root = new ComputationNode(ComputationNodeType.ADD, List.of(node1, node2));

        ComputationNode resultNode = lae.run(root);
        double[][] result = resultNode.getMatrix();

        assertArrayEquals(new double[]{6, 8}, result[0], 0.0001);
        assertArrayEquals(new double[]{10, 12}, result[1], 0.0001);
    }

    @Test
    public void testNaryAddition() {
        double[][] val = {{1.0}};
        List<ComputationNode> operands = new ArrayList<>();
        operands.add(new ComputationNode(val));
        operands.add(new ComputationNode(val));
        operands.add(new ComputationNode(val));

        ComputationNode root = new ComputationNode(ComputationNodeType.ADD, operands);
        // Ensure associative nesting is applied to convert N-ary to binary steps
        root.associativeNesting();

        ComputationNode result = lae.run(root);
        assertEquals(3.0, result.getMatrix()[0][0], 0.0001);
    }

    @Test
    public void testDimensionMismatchError() {
        double[][] m1 = {{1.0}};
        double[][] m2 = {{1.0, 2.0}, {3.0, 4.0}};
        
        ComputationNode node1 = new ComputationNode(m1);
        ComputationNode node2 = new ComputationNode(m2);
        ComputationNode root = new ComputationNode(ComputationNodeType.ADD, List.of(node1, node2));

        // Verifies the error message required by instructions
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            lae.run(root);
        });

        assertEquals("Illegal operation: dimensions mismatch", exception.getMessage());
    }

    @Test
    public void testMemoryIsolationBetweenRuns() {
    // Run 1: 1 + 1 = 2
    double[][] one = {{1.0}};
    ComputationNode run1 = new ComputationNode(ComputationNodeType.ADD, 
        List.of(new ComputationNode(one), new ComputationNode(one)));
    lae.run(run1); // Result should be 2.0

    // Run 2: 1 + 1 should still be 2
    ComputationNode run2 = new ComputationNode(ComputationNodeType.ADD, 
        List.of(new ComputationNode(one), new ComputationNode(one)));
    ComputationNode resultNode = lae.run(run2);
    
    assertEquals(2.0, resultNode.getMatrix()[0][0], "Memory leaked from previous run!");
    }
    @Test
    public void testAssociativeNestingStructure() {
        double[][] val = {{1.0}};
        List<ComputationNode> children = new ArrayList<>(List.of(
            new ComputationNode(val), 
            new ComputationNode(val), 
            new ComputationNode(val)
        ));
        ComputationNode root = new ComputationNode(ComputationNodeType.ADD, children);
        
        // Apply nesting
        root.associativeNesting();
        // After nesting, root should have 2 children.
        assertEquals(2, root.getChildren().size(), "Root should be binary after nesting");
        assertEquals(ComputationNodeType.ADD, root.getChildren().get(0).getNodeType(), 
            "Left child should be a nested ADD node");
    }

    @Test
    public void testDeepNestedAddition() {
        // Operation: (((1+1)+1)+1) = 4
        double[][] one = {{1.0}};
        ComputationNode node1 = new ComputationNode(one);
        ComputationNode node2 = new ComputationNode(one);
        
        // Nesting 4 layers deep
        ComputationNode layer1 = new ComputationNode(ComputationNodeType.ADD, List.of(node1, node2)); // 2
        ComputationNode layer2 = new ComputationNode(ComputationNodeType.ADD, List.of(layer1, node1)); // 3
        ComputationNode root = new ComputationNode(ComputationNodeType.ADD, List.of(layer2, node1));  // 4

        ComputationNode resultNode = lae.run(root);
        assertEquals(4.0, resultNode.getMatrix()[0][0], 0.0001, "Deep nesting caused memory accumulation!");
    }

    @Test
    public void testTransposeAdditionMixed() {
        double[][] a = {{1.0, 2.0}};
        ComputationNode nodeA = new ComputationNode(a);
        ComputationNode transNode = new ComputationNode(ComputationNodeType.TRANSPOSE, List.of(nodeA));
        ComputationNode root = new ComputationNode(ComputationNodeType.ADD, List.of(nodeA, transNode));

        // This should fail because [[1, 2]] is 1x2 and Transpose is 2x1
        assertThrows(IllegalArgumentException.class, () -> lae.run(root), 
            "Engine failed to detect mismatch after a unary operation!");
    }

    @Test
    public void testMultiplicationIdentity() {
        // [1, 2] * [[1, 0], [0, 1]] = [1, 2]
        double[][] rowVector = {{1.0, 2.0}};
        double[][] identity = {{1.0, 0.0}, {0.0, 1.0}};
        
        ComputationNode node1 = new ComputationNode(rowVector);
        ComputationNode node2 = new ComputationNode(identity);
        ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY, List.of(node1, node2));

        ComputationNode resultNode = lae.run(root);
        double[][] res = resultNode.getMatrix();
        
        assertEquals(1, res.length);
        assertEquals(2, res[0].length);
        assertArrayEquals(new double[]{1.0, 2.0}, res[0], 0.0001);
    }

    @Test
    public void testLargeMatrixParallelism() {
        int size = 100;
        double[][] large = new double[size][size];
        for(int i=0; i<size; i++) for(int j=0; j<size; j++) large[i][j] = 1.0;

        ComputationNode node1 = new ComputationNode(large);
        ComputationNode node2 = new ComputationNode(large);
        ComputationNode root = new ComputationNode(ComputationNodeType.ADD, List.of(node1, node2));

        ComputationNode resultNode = lae.run(root);
        double[][] res = resultNode.getMatrix();

        // Every cell should be 2.0
        for(int i=0; i<size; i++) {
            for(int j=0; j<size; j++) {
                assertEquals(2.0, res[i][j], 0.0001, "Race condition detected at index ["+i+"]["+j+"]");
            }
        }
    }

// --- NEW EDGE CASE TESTS START HERE ---

    @Test
    public void testMultiplicationDimensionChange() {
        // Edge Case: Vector-Matrix multiplication that changes the vector dimension.
        // Input: 1x3 vector * 3x2 matrix = 1x2 vector
        double[][] vec = {{1.0, 2.0, 3.0}};
        double[][] mat = {
            {1.0, 2.0},
            {3.0, 4.0},
            {5.0, 6.0}
        };

        ComputationNode nodeVec = new ComputationNode(vec);
        ComputationNode nodeMat = new ComputationNode(mat);
        ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY, List.of(nodeVec, nodeMat));

        ComputationNode resultNode = lae.run(root);
        double[][] res = resultNode.getMatrix();

        // 1x2 Result expected
        assertEquals(1, res.length);
        assertEquals(2, res[0].length);
        
        // Calculation: 
        // [1*1 + 2*3 + 3*5,  1*2 + 2*4 + 3*6] = [1+6+15, 2+8+18] = [22, 28]
        assertEquals(22.0, res[0][0], 0.0001);
        assertEquals(28.0, res[0][1], 0.0001);
    }

    @Test
    public void testZeroMatrixIdentity() {
        // Edge Case: Adding a Zero Matrix should yield the original matrix
        double[][] original = {{5.0, -3.0}, {2.0, 100.0}};
        double[][] zero = {{0.0, 0.0}, {0.0, 0.0}};

        ComputationNode root = new ComputationNode(ComputationNodeType.ADD, 
            List.of(new ComputationNode(original), new ComputationNode(zero)));

        ComputationNode result = lae.run(root);
        double[][] resMatrix = result.getMatrix();

        assertArrayEquals(original[0], resMatrix[0], 0.0001);
        assertArrayEquals(original[1], resMatrix[1], 0.0001);
    }

    @Test
    public void testMixedNegativeArithmetic() {
        // Edge Case: (A * B) + Negate(C) with negative numbers
        // A=[-1], B=[2] -> -2
        // C=[5] -> Negate(C)=[-5]
        // Result: -2 + (-5) = -7
        
        ComputationNode a = new ComputationNode(new double[][]{{-1.0}});
        ComputationNode b = new ComputationNode(new double[][]{{2.0}});
        ComputationNode c = new ComputationNode(new double[][]{{5.0}});

        ComputationNode mult = new ComputationNode(ComputationNodeType.MULTIPLY, List.of(a, b));
        ComputationNode neg = new ComputationNode(ComputationNodeType.NEGATE, List.of(c));
        ComputationNode root = new ComputationNode(ComputationNodeType.ADD, List.of(mult, neg));

        ComputationNode res = lae.run(root);
        assertEquals(-7.0, res.getMatrix()[0][0], 0.0001);
    }


}