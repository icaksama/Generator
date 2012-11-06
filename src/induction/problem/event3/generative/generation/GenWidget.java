package induction.problem.event3.generative.generation;

import edu.berkeley.nlp.ling.Tree;
import induction.problem.event3.CFGRule;
import induction.problem.event3.Widget;
import induction.problem.event3.params.Parameters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author konstas
 */
public class GenWidget extends Widget
{
    protected int[] text, nums;
    protected double[] scores;
    protected Collection<Integer> trueEvents;
    LinkedList<Tree<String>> queue;
    
    public GenWidget(int [][]events, int[][] fields, int[][] gens,
                     int [][] numMethods,
                     int [] text,
                     HashSet<Integer>[] eventTypeAllowedOnTrack,
                     Map<Integer, Integer> eventTypeIndices)
    {
        super(events, fields, gens, numMethods, null,
              eventTypeAllowedOnTrack, eventTypeIndices);
        if(text != null)
        {
            this.text = text;
            nums = new int[text.length];
            Arrays.fill(nums, -1);
        }
        scores = new double[Parameters.NUMBER_OF_METRICS_GEN];
    }

    /**
     * Constructor for gold-standard widget.
     * @param text the gold-standard text, for calculating generation-oriented metrics
     */
    public GenWidget(int[] text)
    {
         this(null, null, null, null, text, null, null);
    }

    /**
     * Constructor for gold-standard widget.
     * @param events the true events, for calculating Precision, Recall and F-1
     * @param text the gold-standard text, for calculating generation-oriented metrics
     */
    public GenWidget(int[][] events, int[] text)
    {
         this(events, null, null, null, text, null, null);
         trueEvents = new ArrayList<Integer>();
         int prev = -5, cur;
         // consider 1st track first seperately
         for(int j = 0; j < events[0].length; j++)
         {
             cur = new Integer(events[0][j]);
             if(prev != cur)
                 trueEvents.add(cur);
             prev = cur;
         }
         // consider next tracks as supplementary (only >-1)
         for(int i = 1; i < events.length; i++)
         {
             for(int j = 0; j < events[0].length; j++)
             {
                 cur = new Integer(events[i][j]);
                 if(prev != cur && cur > -1)
                     trueEvents.add(cur);
                 prev = cur;
             }
         }
    }

    public GenWidget(int [][]events, int[][] fields, int[][] gens,
                     int [][] numMethods,
                     int [] text,
                     HashSet<Integer>[] eventTypeAllowedOnTrack,
                     Map<Integer, Integer> eventTypeIndices, String startSymbol)
    {
        this(events, fields, gens, numMethods, text, eventTypeAllowedOnTrack, eventTypeIndices);        
        if(startSymbol != null)
        {            
            recordTree = new Tree<String>(startSymbol, false);
            queue = new LinkedList<Tree<String>>();            
            queue.push(recordTree);
        }
    }
    
    public GenWidget(int[][] events, int[] text, Tree<String> recordTree)
    {
        this(events, text);
        this.recordTree = recordTree;
    }
    
    void addEdge(CFGRule rule)
    {
        Tree<String> first = queue.poll();
        if(first.getLabel().equals(rule.getLhsToString()))
        {
            List<Tree<String>> newChildren = new ArrayList(2);
            for(String rhs : rule.getRhsListToString())
                newChildren.add(new Tree<String>(rhs, false));
            first.setChildren(newChildren);
            queue.addAll(0, newChildren);
        }
        else
        {
            addEdge(rule);
        }        
    }
    
    public int[] getNums()
    {
        return nums;
    }

    public int[] getText()
    {
        return text;
    }  
    
    public double[] getScores()
    {
        return scores;
    }
        
    @Override
    public String toString()
    {
        String out = "";
        for(int i = 0; i < text.length; i++)
        {
            out += (nums[i] > -1 ? nums[i] :
                    text[i]) + " ";
        }
        return out.trim();
    }


}
