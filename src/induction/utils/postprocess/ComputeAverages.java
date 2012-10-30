package induction.utils.postprocess;

import induction.Options.InitType;
import induction.problem.event3.Event3Model;
import induction.problem.event3.generative.GenerativeEvent3Model;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sinantie
 */
public class ComputeAverages
{
    ComputeAveragesOptions opts;

    private Action action;
    Event3Model model;
    List<String[]> examples;
    
    public ComputeAverages(ComputeAveragesOptions opts)
    {
        this.opts = opts;
    }

    public ComputeAverages(String inputPath, boolean examplesInOneFile, Action action)
    {
        this.action = action;
    }
        
    public void execute()
    {
        model = new GenerativeEvent3Model(opts.modelOpts);
        model.init(InitType.staged, opts.modelOpts.initRandom, "");
        model.readExamples();
        examples = new ArrayList<String[]>(model.getExamples().size());
        for(String[] example : examples)
        {
            action.act(example);
            System.out.println(action.result());
        }
        
    }
    
    public static void main(String[] args)
    {
        // average alignments per example
//        final String inputPath = "data/atis/test/atis-test.txt";
//        Action action = new AverageAlignmentsPerExample();
        // average number of fields with no value in the 'flight' record
        final String inputPath = "data/atis/train/atis5000.sents.full.prototype";
        Action action = new AverageFieldsWithNoValuePerRecord("flight", 13);
        ComputeAverages ca = new ComputeAverages(inputPath, true, action);
        ca.execute();
    }
    
    /**
     * find the average number of gold standard alignments per example
     */
    static class AverageAlignmentsPerExample implements Action {
        int totalAlignedEvents = 0, totalExamples = 0;
        @Override
        public Object act(Object in)
        {
            String[] example = (String[])in;
            // 4th entry are the alignments that have the format line_no [event_id]+
            // we just count the number of events
            totalAlignedEvents += example[3].split(" ").length - 1;
            totalExamples++;
            return null;
        }

        @Override
        public Object result()
        {
            return new Double((double)totalAlignedEvents / (double)totalExamples);
        }        
    }
    
    static class AverageFieldsWithNoValuePerRecord implements Action {

        private String record;
        private int totalNumberOfFields, totalEmpty, totalExamples;
        
        public AverageFieldsWithNoValuePerRecord(String record, int totalNumberOfFields)
        {
            this.record = record;
            this.totalNumberOfFields = totalNumberOfFields;
        }
        
        @Override
        public Object act(Object in)
        {
            String[] example = (String[])in;
            // 3rd entry are the events
            String events[] = example[2].split("\n");
            String recordEvent = null;
            // capture the line with the event we are looking for
            for(String event : events)
                if(event.contains(".type:"+record))
                    recordEvent = event;
            if(recordEvent != null)
            {
                // count the number of non-empty fields. 
                // Then subtract from the total number of fields.
                // Note that some records have a fixed number of fields, whereas others
                // have a variable number, of only the non-empty. That's why
                // it's safer to subtract from the total
                int total = 0;
                for(String fieldValue : recordEvent.split("\t"))
                {
                    // we need actual fields
                    if(fieldValue.contains("@") || fieldValue.contains("$") || fieldValue.contains("#"))
                    {
                        if(!fieldValue.contains("--")) // capture only the non-empty
                            total++;
                    }
                } // for                
                totalEmpty += totalNumberOfFields - total;
            } // if 
            totalExamples++;
            return null;
        }

        @Override
        public Object result()
        {
            return new Double((double)totalEmpty / (double)totalExamples);
        }
        
    }
    interface Action {
        public Object act(Object in);        
        public Object result();
    }
}
