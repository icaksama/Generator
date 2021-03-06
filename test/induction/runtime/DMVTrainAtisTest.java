package induction.runtime;

import fig.exec.Execution;
import induction.LearnOptions;
import induction.Options;
import induction.Options.InitType;
import induction.problem.dmv.generative.GenerativeDMVModel;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author konstas
 */
public class DMVTrainAtisTest
{
    LearnOptions lopts;
    String name;
    GenerativeDMVModel model;
    
    @Before
    public void setUp() 
    {        
         String args = "-modelType dmv "
                    + "-Options.stage1.numIters 30 "
                    + "-numThreads 2 "                    
                    + "-examplesInSingleFile "                    
                    + "-inputLists "
//                    + "../wsj/3.0/parsed/mrg/atis/atis3_clean_pos_cut.mrg "
//                    + "data/atis/train/atis5000.sents.full "
                    + "data/atis/train/atis5000.sents.full.tagged.CDnumbers "
                    + "-inputFileExt events "
                    + "-inputFormat raw "
                    + "-dontOutputParams "
                    + "-useTagsAsWords "
                    + "-posAtSurfaceLevel ";
//                    + "-usePosTagger "            

         
        /*initialisation procedure from Induction class*/
        Options opts = new Options();
        Execution.init(args.split(" "), new Object[] {opts}); // parse input params        
        model = new GenerativeDMVModel(opts);
        model.readExamples();
        model.logStats();
        model.preInit();
        model.init(InitType.random, opts.initRandom, "stage1");
        opts.outputIterFreq = opts.stage1.numIters;
        lopts = opts.stage1;
        name = "stage1";
    }
   
    /**
     * Test of run method, of class Induction.
     */
    @Test
    public void testRun()
    {
        System.out.println("run");        
        System.out.println(model.testInitLearn(name, lopts));
    }
}
