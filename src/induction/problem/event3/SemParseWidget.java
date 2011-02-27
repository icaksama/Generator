package induction.problem.event3;

import induction.problem.event3.params.Parameters;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 *
 * @author konstas
 */
public class SemParseWidget extends GenWidget
{
    // we use a collection of MRTokens for the gold-standard instead of the
    // conventional arrays because they are not based on N (text length)
    // because
    protected Collection<MRToken> trueMrTokens;
    
    public SemParseWidget(int [][]events, int[][] fields, int[][] gens,
                     int [][] numMethods,
                     int [] values,
                     HashSet<Integer>[] eventTypeAllowedOnTrack,
                     int[] eventTypeIndices)
    {
        super(events, fields, gens, numMethods, values,
              eventTypeAllowedOnTrack, eventTypeIndices);
        scores = new double[Parameters.NUMBER_OF_METRICS_SEM_PAR];
    }

    /**
     * Constructor for gold-standard widget.
     * @param trueMrTokens the true events, for calculating Precision, Recall and F-1
     * @param values the gold-standard values, for calculating generation-oriented metrics
     */
    SemParseWidget(Collection<MRToken> trueMrTokens)
    {
         this(null, null, null, null, null, null, null);
         this.trueMrTokens = trueMrTokens;
    }    
}
