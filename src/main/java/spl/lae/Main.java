package spl.lae;
import java.io.IOException;

import parser.ComputationNode;
import parser.ComputationNodeType;
import parser.InputParser;
import parser.OutputWriter;

public class Main {
  public static void main(String[] args) throws IOException {
    int numThreads = Integer.parseInt(args[0]);
    String inputPath = args[1];
    String outputPath = args[2];
    InputParser parser = new InputParser();
    LinearAlgebraEngine lae = null;
      try {
        ComputationNode root = parser.parse(inputPath);   // parse JSON → computation tree
        if (root == null) {
            throw new IllegalArgumentException("Parsed computation tree is null");
        }
        else if (root.getNodeType() == ComputationNodeType.MATRIX) {
          // if only a single matrix is provided, write it directly to output
          OutputWriter.write(root.getMatrix(), outputPath);
        }
        else {
        lae = new LinearAlgebraEngine(numThreads);
        ComputationNode resultNode = lae.run(root);       // run engine until root is MATRIX
        double[][] result = resultNode.getMatrix();      // get final matrix
        OutputWriter.write(result, outputPath);          // write JSON output
      }
    } catch (Exception e) {
        // write error JSON
        OutputWriter.write(e.getMessage(), outputPath);
    }

    
  }
}