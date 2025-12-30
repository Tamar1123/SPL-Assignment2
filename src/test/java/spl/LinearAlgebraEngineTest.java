package spl; // Updated to match the directory /src/test/java/spl/

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows; // Explicit import needed if packages differ
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import parser.ComputationNode;
import parser.ComputationNodeType;
import spl.lae.LinearAlgebraEngine;

public class LinearAlgebraEngineTest {

    private LinearAlgebraEngine lae;

    @BeforeEach
    public void setUp() {
        // Initialize the engine with 4 threads
        lae = new LinearAlgebraEngine(4);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
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
}