package induction.problem.event3.planning;

import edu.uci.ics.jung.graph.Graph;
import fig.basic.FullStatFig;
import fig.basic.ListUtils;
import fig.basic.LogInfo;
import fig.exec.Execution;
import fig.record.Record;
import induction.LearnOptions;
import induction.Options;
import induction.Utils;
import induction.problem.AExample;
import induction.problem.AInferState;
import induction.problem.AParams;
import induction.problem.APerformance;
import induction.problem.InferSpec;
import induction.problem.event3.Event;
import induction.problem.event3.Event3Model;
import induction.problem.event3.params.Parameters;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author konstas
 */
public abstract class PlanningEvent3Model extends Event3Model implements Serializable
{

    Set<Permutation> goldPermutations;
    
    public PlanningEvent3Model(Options opts) 
    {
        super(opts);
        goldPermutations = new HashSet<Permutation>();
    }
        
    @Override
    protected APerformance newPerformance() 
    {
        return new PlanningPerformance(this);
    } 

    @Override
    protected AInferState newInferState(AExample ex, AParams params, AParams counts, InferSpec ispec, Graph graph) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void saveParams(String name) {
        throw new UnsupportedOperationException("Not supported");
    }
   
    @Override
    public void learn(String name, LearnOptions lopts) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    protected void readExamples(String input, int maxExamples)
    {
        String eventInput, name = "", alignInput = "";        
        boolean alignInputExists = false;
        if(opts.examplesInSingleFile)
        {
            String[] res = Utils.extractExampleFromString(input);
            name = res[0];            
            eventInput = res[2];
            alignInput = res[3];                        
            alignInputExists = alignInput != null;            
        }
        else
        {
            eventInput = input;     
            name = alignInput;
            alignInput = input.replaceAll("\\."+ opts.inputFileExt, ".align");
            alignInputExists = new File(alignInput).exists();            
        }
        if (!opts.useOnlyLabeledExamples || alignInputExists)
        {
            final HashSet<String> excludedFields = new HashSet<String>();
            excludedFields.addAll(Arrays.asList(opts.excludedFields));
            final HashSet<String> excludedEventTypes = new HashSet<String>();
            excludedEventTypes.addAll(Arrays.asList(opts.excludedEventTypes));
            // create a set of event ids that have been excluded from the model by the user
            HashSet<Integer> excludedEventsIndices = new HashSet<Integer>();            
            //Read events
            Map<Integer, Event> events  = null;
            try
            {
                events = readEvents(opts.examplesInSingleFile ?
                                            eventInput.split("\n") :
                                            Utils.readLines(eventInput),
                                        excludedEventTypes, excludedFields, excludedEventsIndices);
            }
            catch(Exception e) 
            {
                LogInfo.error(e);
                Execution.finish();                
            }                                              
            HashMap<Integer, Integer> eventTypeIndices = new HashMap<Integer, Integer>(events.size());
            for(Event e : events.values())
            {
                eventTypeIndices.put(e.getId(), e.getEventTypeIndex());
            }
            Permutation goldAlignments = parseAlignments(alignInput, eventTypeIndices, excludedEventsIndices);
            if(goldPermutations.add(goldAlignments)) // add unique permutations
            {
                // add the reference plan to get the model's probability for it. 
                // NOTE: It should always be added before the random permutations.
                int[] goldAlignmentsIntAr = goldAlignments.toInt();
                examples.add(new PlanningExample(this, goldAlignmentsIntAr, events, new PlanningWidget(goldAlignmentsIntAr), name + "_GOLD"));
                for(int i = 0; i < opts.randomPermutations; i++)
                {                                    
                    examples.add(new PlanningExample(this, goldAlignments.newRandomPermutation().toInt(), 
                                 events, new PlanningWidget(goldAlignmentsIntAr), name));
                }
            }
        } // if
    }
    
    protected Permutation parseAlignments(String alignments, HashMap<Integer, Integer> eventTypeIndices,
                                    HashSet<Integer> excludedEventsIndices)
    {
        List<Integer> out = new ArrayList<Integer>(), eventTypesLine = new ArrayList<Integer>();
        int eventId, eventTypeId;
        for(String line : alignments.split("\n"))
        {
            String[] lineEvents = line.split(" ");           
            for(int i = 1; i < lineEvents.length; i++)
            {
                eventId = Integer.parseInt(lineEvents[i]);
                // -1 means that this line corresponds to an event that's not in the candidate set                
                eventTypeId = eventId != -1 ? eventTypeIndices.get(eventId) : Parameters.none_e;                
                // we don't allow repetitions of record tokens in the same sentence
                if(eventTypesLine.isEmpty() || !eventTypesLine.contains(eventTypeId))                    
                {
                    eventTypesLine.add(eventTypeId);
                }                
            } // for
            if(!eventTypesLine.isEmpty())
            {
                out.addAll(eventTypesLine);
                eventTypesLine.clear();
            }                        
        } // for
        return new Permutation(out.toArray(new Integer[0]));
    }
    
    @Override
    public void generate(String name, LearnOptions lopts) 
    {
        opts.alignmentModel = lopts.alignmentModel; // HACK
//        saveParams("stage1");
//        System.exit(1);   
        FullStatFig complexity = new FullStatFig(); // Complexity inference (number of hypergraph nodes)
        double temperature = lopts.initTemperature;
        testPerformance = newPerformance();       
                        
        // E-step
        Utils.begin_track("Planning Evaluation step " + name);
        Collection<BatchEM> list = new ArrayList(examples.size());
        for(int i = 0; i < examples.size(); i++)
        {
            list.add(new BatchEM(i, examples.get(i), null, temperature, lopts, 0, complexity));                
        }
        Utils.parallelForeach(opts.numThreads, list);
        LogInfo.end_track();
        list.clear();        
        Execution.putOutput("currExample", examples.size());

        // Final
//        testPerformance.output(Execution.getFile(name+".test.performance"));
        Record.begin("planning evaluation");
        record("results", name, complexity, true);
        Record.end();
        LogInfo.end_track();
    }
    
    /**
     * helper method for testing the planning evaluation output. Simulates generate(...) method
     * for a single example without the thread mechanism
     * @return a String with the performance output
     */
    @Override
    public String testGenerate(String name, LearnOptions lopts)
    {
        opts.alignmentModel = lopts.alignmentModel;
        FullStatFig complexity = new FullStatFig();
        double temperature = lopts.initTemperature;
        testPerformance = newPerformance();
//        AParams counts = newParams();        
        AInferState inferState = null;
        for(AExample ex : examples)
        {
            inferState =  createInferState(ex, 1, null, temperature, lopts, 0, complexity);
            testPerformance.add(ex, inferState.bestWidget);
        }
        return testPerformance.output();        
    }
    
    protected class Permutation
    {
        Integer[] sequence;

        public Permutation(Integer[] sequence)
        {
            this.sequence = sequence;
        }
        
        int[] toInt()
        {
            return ListUtils.toInt(sequence);
        }

        Permutation newRandomPermutation()
        {
            Integer[] clone = Arrays.copyOf(sequence, sequence.length);
            do
            {   // TO-DO: UNCOMMENT!!!
                //ListUtils.randomPermute(clone, opts.initRandom);
            }
            while(Arrays.deepEquals(clone, sequence));
            return new Permutation(clone);
        }
        
        @Override
        public boolean equals(Object obj)
        {
            assert obj instanceof Permutation;
            Permutation p = (Permutation)obj;
            
            for(int i = 0; i < sequence.length; i++)
            {
                if(!sequence[i].equals(p.sequence[i]))
                {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 97 * hash + Arrays.deepHashCode(this.sequence);
            return hash;
        }

        @Override
        public String toString()
        {
            return Arrays.toString(sequence);
        }
        
        
    }
}
