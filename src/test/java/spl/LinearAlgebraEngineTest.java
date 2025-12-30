package spl; // Updated to match the directory /src/test/java/spl/

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List; // This resolves the tempDir variable

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir; // Explicit import needed if packages differ

import parser.ComputationNode;
import parser.ComputationNodeType;
import spl.lae.LinearAlgebraEngine;
import spl.lae.Main;


public class LinearAlgebraEngineTest {

    private LinearAlgebraEngine lae;

    @BeforeEach
    public void setUp() {
        // Initialize the engine with 4 threads
        lae = new LinearAlgebraEngine(4);
    }

    @TempDir
    Path tempDir;

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

    @Test
    public void testMemoryIsolationBetweenRuns() {
    // Run 1: 1 + 1 = 2
    double[][] one = {{1.0}};
    ComputationNode run1 = new ComputationNode(ComputationNodeType.ADD, 
        List.of(new ComputationNode(one), new ComputationNode(one)));
    lae.run(run1); // Result should be 2.0

    // Run 2: 1 + 1 should still be 2
    // If there is a bug, this might return 3.0 or 4.0
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

    // Verify structure: Root should now have 2 children, 
    // and the first child should be another ADD node.
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
    
    // If the bug is present, this often returns 5.0 or 8.0 
    // because the 'leftMatrix' memory is being reused while the old result is still in it.
    assertEquals(4.0, resultNode.getMatrix()[0][0], 0.0001, "Deep nesting caused memory accumulation!");
}

@Test
public void testTransposeAdditionMixed() {
    // Operation: A + Transpose(A)
    // A = [[1, 2]]
    // Transpose(A) = [[1], [2]]
    // Result = ERROR (Dimension Mismatch)
    
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

    // Every single cell should be 2.0
    for(int i=0; i<size; i++) {
        for(int j=0; j<size; j++) {
            assertEquals(2.0, res[i][j], 0.0001, "Race condition detected at index ["+i+"]["+j+"]");
        }
    }
}

@Test
public void testIndependentExecutionIsolation() throws Exception {
    // Operation 1: [[1]] + [[1]] = [[2]]
    String input1 = "{\"operator\": \"+\", \"operands\": [[[1.0]], [[1.0]]]}";
    Path in1 = tempDir.resolve("in1.json");
    Path out1 = tempDir.resolve("out1.json");
    Files.writeString(in1, input1);
    Main.main(new String[]{"1", in1.toString(), out1.toString()});

    // Operation 2: [[1]] + [[1]] SHOULD STILL = [[2]]
    // If bug exists, this might return [[3]] or [[4]]
    Path out2 = tempDir.resolve("out2.json");
    Main.main(new String[]{"1", in1.toString(), out2.toString()});

    String content2 = Files.readString(out2);
    assertTrue(content2.contains("2.0"), "Memory leaked from first execution to second!");
    assertFalse(content2.contains("4.0"), "Cumulative error detected!");
}


}