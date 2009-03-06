/* *********************************************************************** *
 * project: org.matsim.*
 * ThetaExplorer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package playground.johannes.graph.mcmc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

import playground.johannes.graph.Graph;
import playground.johannes.graph.GraphStatistics;
import playground.johannes.graph.PlainGraph;
import playground.johannes.graph.SparseEdge;
import playground.johannes.graph.SparseVertex;
import playground.johannes.graph.generators.ErdosRenyiGenerator;
import playground.johannes.graph.generators.PlainGraphFactory;

/**
 * @author illenberger
 *
 */
public class ThetaExplorer {

	private static final double theta_min = 0.5;
	
	private static final double theta_max = 0.6;
	
	private static final double theta_step = 0.001;
	
	private static final int burninTime = (int)1E7;
	
	private static int N;
	
	public static void main(String[] args) throws IOException {
		N = 200;//Integer.parseInt(args[0]);
		BufferedWriter writer = new BufferedWriter(new FileWriter(args[1]));
		writer.write("theta_density\ttheta_twostars\ttheta_triangles\tm\t<k>\tc\tsigma_2");
		writer.newLine();
		
		ErdosRenyiGenerator<PlainGraph, SparseVertex, SparseEdge> generator = new ErdosRenyiGenerator<PlainGraph, SparseVertex, SparseEdge>(new PlainGraphFactory());
		PlainGraph graph = generator.generate(N, 0.01, 0);
		
		dump(graph);
		
		GibbsSampler sampler = new GibbsSampler(0);
		
		Ergm ergm = new Ergm();
		
		ErgmTerm[] terms = new ErgmTerm[3];
		terms[0] = new ErgmDensity();
		terms[1] = new ErgmTwoStars();
		terms[2] = new ErgmTriangles();
		
		ergm.setErgmTerms(terms);
		
//		for(double theta_density = theta_min; theta_density <= theta_max; theta_density += theta_step) {
		double theta_density = Math.log(0.01/(1-0.01));
//			for(double theta_twostars = theta_min; theta_twostars <= theta_max; theta_twostars += theta_step) {
		double theta_twostars = 0.004;
				for(double theta_triangles = theta_min; theta_triangles <= theta_max; theta_triangles += theta_step) {
					terms[0].setTheta(theta_density);
					terms[1].setTheta(theta_twostars);
//					double theta_triangles = 0.5;
					terms[2].setTheta(theta_triangles);
					
					AdjacencyMatrix m = new AdjacencyMatrix(graph);
					
					System.out.println(String.format("Simulation with theta_density=%1$s, theta_twostars=%2$s, theta_triangles=%3$s", theta_density, theta_twostars, theta_triangles));
					long time = System.currentTimeMillis();
					sampler.sample(m, ergm, burninTime);
					System.out.println("Simualtion took "+(System.currentTimeMillis() - time)+" ms.");
					
					Graph g = m.getGraph(new PlainGraphFactory());
					dump(g);
					
					int edges = g.getEdges().size();
					double k = GraphStatistics.getDegreeStatistics(g).getMean();
					double c = GraphStatistics.getClusteringStatistics(g).getMean();
					int sigma_2 = GraphStatistics.getNumTwoStars(g);
					
					writer.write(String.format(Locale.US, "%1$.2f", theta_density));
					writer.write("\t");
					writer.write(String.format(Locale.US, "%1$.2f", theta_twostars));
					writer.write("\t");
					writer.write(String.format(Locale.US, "%1$.2f", theta_triangles));
					writer.write("\t");
					writer.write(String.valueOf(edges));
					writer.write("\t");
					writer.write(String.valueOf((float)k));
					writer.write("\t");
					writer.write(String.valueOf((float)c));
					writer.write("\t");
					writer.write(String.valueOf(sigma_2));
					writer.newLine();
					writer.flush();
//				}
			}
//		}
		
		
		
	}

	private static void dump(Graph g) {
		System.out.println(String.format("m=%1$s, <k>=%2$s, c=%3$s, 2-stars=%4$s",
				g.getEdges().size(),
				GraphStatistics.getDegreeStatistics(g).getMean(),
				GraphStatistics.getClusteringStatistics(g).getMean(),
				GraphStatistics.getNumTwoStars(g)));
	}
}
