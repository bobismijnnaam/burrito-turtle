package tests;

import java.util.List;

import org.junit.experimental.ParallelComputer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class ParallelRunner {
	public static void main(String[] args) {
//	      Class[] cls={SprockellTest.class};

	      //Parallel among classes  
//	      JUnitCore.runClasses(SprockellTest.classes(), cls);  

	      //Parallel among methods in a class  
	      Result result = JUnitCore.runClasses(ParallelComputer.methods(), SprockellTest.class);  
	      
	      List<Failure> failures = result.getFailures();
	      System.out.println(failures);

	      //Parallel all methods in all classes  
//	      JUnitCore.runClasses(new ParallelComputer(true, true), cls); 
	}
}
