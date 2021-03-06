package induction.problem.event3.generative.alignment;

import fig.basic.StopWatchSet;
import induction.problem.event3.generative.GenerativeEvent3Model;
import induction.problem.event3.params.EventTypeParams;
import induction.problem.event3.params.Params;
import induction.problem.event3.params.Parameters;
import induction.problem.event3.params.TrackParams;
import induction.Hypergraph;
import induction.Hypergraph.HyperpathResult;
import induction.ngrams.NgramModel;
import induction.problem.AModel;
import induction.problem.InferSpec;
import induction.problem.event3.Constants;
import induction.problem.event3.Event;
import induction.problem.event3.Event3InferState;
import induction.problem.event3.Event3Model;
import induction.problem.event3.Example;
import induction.problem.event3.Widget;
import induction.problem.event3.nodes.EventNode;
import induction.problem.event3.nodes.EventsNode;
import induction.problem.event3.nodes.NoneEventNode;
import induction.problem.event3.nodes.SelectNoEventsNode;
import induction.problem.event3.nodes.WordNode;
import java.util.HashMap;

/**
 *
 * @author konstas
 */
public class InferStateSeg extends Event3InferState
{
    protected NgramModel ngramModel;

    public InferStateSeg(GenerativeEvent3Model model, Example ex, Params params,
            Params counts, InferSpec ispec, NgramModel ngramModel)
    {
        super(model, ex, params, counts, ispec);
    }

    @Override
    protected void initInferState(AModel model, int textLength)
    {
        super.initInferState(model, textLength);
//        N = words.length;
        words = ex.getText();
        nums = new int[words.length];
        for(int w = 0; w < nums.length; w++)
        {
            nums[w] = Constants.str2num(((Event3Model)model).wordToString(words[w])) ;
        }
        labels = ex.getLabels();

        // Override bestWidget
        if (opts.fullPredRandomBaseline)
        {
            if (!ex.events.isEmpty())
            {
                // Just match each line in the text to a single randomly chosen event
                for(int l = 0; l < ex.getStartIndices().length - 1; l++)
                {
                    final int e = opts.fullPredRandom.nextInt(ex.events.size());
                    for(int i = ex.getStartIndices()[l]; i < ex.getStartIndices()[l+1]; i++)
                    {
                        bestWidget.getEvents()[0][i] = e; // Assume one track
                    } // for
                } // for
            } // if
        } // if
    }

    @Override
    protected Widget newWidget()
    {
         HashMap<Integer, Integer> eventTypeIndices =
                            new HashMap<Integer, Integer>(ex.events.size());
        for(Event e : ex.events.values())
        {
            eventTypeIndices.put(e.getId(), e.getEventTypeIndex());
        }
        return new Widget(newMatrix(), newMatrix(), newMatrix(), newMatrix(),
                               ex.getStartIndices(), ((GenerativeEvent3Model)model).eventTypeAllowedOnTrack,
                               eventTypeIndices);
    }

    @Override
    protected void createHypergraph(Hypergraph<Widget> hypergraph)
    {
        hypergraph.debug = opts.debug;
        // Need this because the pc sets might be inconsistent with the types
        hypergraph.allowEmptyNodes = true;

        if (genLabels() || prevGenLabels())
        {
            // Default is to generate the labels from a generic distribution
            // unless we say otherwise
            for(int i = 0; i < ex.N(); i++)
            {
                final int label = labels[i];
                hypergraph.addEdge(hypergraph.prodStartNode(),
                        new Hypergraph.HyperedgeInfo<Widget>()
                // Default is to generate the labels from a generic distribution
                // unless we say otherwise
                {
                    public double getWeight()
                    {
                        return get(params.genericLabelChoices, label);
                    }
                    public void setPosterior(double prob)
                    {
                        if (genLabels())
                            update(counts.genericLabelChoices, label, prob);
                    }
                    public Widget choose(Widget widget)
                    {
                        return widget;
                    }
                });
            } // for
        } // if

        hypergraph.addEdge(hypergraph.prodStartNode(), genEvents(0, ((GenerativeEvent3Model)model).boundary_t(),
                            opts.allowNoneEvent),
                           new Hypergraph.HyperedgeInfo<Widget>()
        {
            public double getWeight()
            {
                return 1;
            }
            public void setPosterior(double prob)
            { }
            public Widget choose(Widget widget)
            {
                return widget;
            }
        });
    }

    @Override
    public void doInference()
    {        
        StopWatchSet.begin("computePosteriors");
//        hypergraph.computePosteriors(ispec.isHardUpdate());
        hypergraph.computePosteriors(false);
        StopWatchSet.end();
        // Hard inference
        if (hardInfer)
        {
            HyperpathResult result = hypergraph.fetchBestHyperpath(newWidget());
//            HyperpathResult<Widget> result = hypergraph.fetchSampleHyperpath(opts.initRandom, newWidget());
            bestWidget = (Widget)result.widget;
            logVZ = result.logWeight;
        }
        else
        {
            bestWidget = newWidget();
            logVZ = Double.NaN;
        }
        updateStats();
    }
    
    @Override
    public void updateCounts()
    {
        synchronized(counts)
        {
          if(ispec.isMixParamsCounts())
          {
              counts.saveSum();
          }
          StopWatchSet.begin("fetchPosteriors");
          hypergraph.fetchPosteriors(ispec.isHardUpdate());
          StopWatchSet.end();
        }
    }
    
    /**
     * Default: don't generate any event (there should be only one of these nodes)
     * Note: we don't need any state, but include i and c so that we get distinct
     * nodes (see note in Hypergraph)
     * @param c the track to generate events for
     * @return
     */
    protected Object selectNoEvents(int c)
    {
        if (ex.events.isEmpty())
            return hypergraph.endNode;
        else
        {
            SelectNoEventsNode node = new SelectNoEventsNode(0, c);
            if (hypergraph.addProdNode(node))
            {
//                for(int e = 0; e < ex.events.length && ex.events[e] != null; e++)
                for(final Event e : ex.events.values())
                {
                    final int eventTypeIndex = e.getEventTypeIndex();
                    final EventTypeParams eventTypeParams = params.eventTypeParams[eventTypeIndex];
                    final EventTypeParams eventTypeCounts = counts.eventTypeParams[eventTypeIndex];
                    hypergraph.addEdge(node, new Hypergraph.HyperedgeInfo<Widget>() {
                        public double getWeight() {
                                return get(eventTypeParams.filters, Parameters.B_FALSE);
                        }
                        public void setPosterior(double prob) {
                             update(eventTypeCounts.filters, Parameters.B_FALSE, prob);
                        }
                        public Widget choose(Widget widget) {
                            return widget;
                        }
                    });
                } // for
            } // if
            return node;
        } // else
    }

    /**
     * Generate no event from i to j; incorporate salience if necessary
     * @param c the track to generate events for
     * @return
     */
    protected Object genNoneEvent(int seqNo)
    {
        if (opts.useEventSalienceModel)
        {
            NoneEventNode node = new NoneEventNode(seqNo, 0, 0);
//            if (hypergraph.addProdNode(node))
//                hypergraph.addEdge(node, genNoneEventWords(c), selectNoEvents(c));
            return node;
        }
        else
        {
//            return genNoneEventWords(c);
            WordNode node = new WordNode(-1, 0, ((GenerativeEvent3Model)model).none_t(), 0);
            hypergraph.addSumNode(node);
            return node;
        }
    }

    /**
     * Generate the event, but make field sets respect efs
     * @param the sequence number of the event
     * @param event
     * @param efs
     * @return
     */
    protected Object genEFSEvent(int seqNo, int event, int efs)
    {
        final EventTypeParams eventTypeParams = params.eventTypeParams[
                ex.events.get(event).getEventTypeIndex()];
        final EventTypeParams eventTypeCounts = counts.eventTypeParams[
                ex.events.get(event).getEventTypeIndex()];
        if (opts.useEventSalienceModel)
        {
            EventNode node = new EventNode(0, 0, seqNo, event);
//            if(hypergraph.addProdNode(node))
//            {
//                // We have to choose event e and not the others
//                hypergraph.addEdge(node,
//                        genFields(c, event, eventTypeParams.boundary_f, efs), selectNoEvents(c),
//                        new Hypergraph.HyperedgeInfo<Widget>() {
//                    public double getWeight() {
//                            return get(eventTypeParams.filters, Parameters.B_TRUE) /
//                                   get(eventTypeParams.filters, Parameters.B_FALSE); // Compensate
//                    }
//                    public void setPosterior(double prob) {
//                         update(eventTypeCounts.filters, Parameters.B_TRUE, prob);
//                         update(eventTypeCounts.filters, Parameters.B_FALSE, -prob); // Compensate
//                    }
//                    public Widget choose(Widget widget) {
//                        return widget;
//                    }
//                });
//            } // if
            return node;
        } // if
        else
        {
//            return genFields(c, event, eventTypeParams.boundary_f, efs);
            WordNode node = new WordNode(-1, 0, event, 0);
            hypergraph.addSumNode(node);
            return node;
        }
    }

    /**
     * Generate event e at the position seqNo of the sequence; incorporate salience if necessary
     * @param seqNo the sequence number of the event
     * @param event
     * @return
     */
    protected Object genEvent(int seqNo, int event)
    {
        final EventTypeParams eventTypeParams = params.eventTypeParams[
                ex.events.get(event).getEventTypeIndex()];
        final EventTypeParams eventTypeCounts = counts.eventTypeParams[
                ex.events.get(event).getEventTypeIndex()];
        if (useFieldSets(ex.events.get(event).getEventTypeIndex()))
        {
            EventNode node = new EventNode(seqNo, 0, 0, event);
            if(hypergraph.addSumNode(node))
            {
                // Choose which fields to use
                for(int fs = 0; fs < eventTypeParams.getAllowed_fs().length; fs++)
                {
                    final int fsIter = fs;
                    hypergraph.addEdge(node,
                        genEFSEvent(seqNo, event, eventTypeParams.fs2efs(fs)),
                        new Hypergraph.HyperedgeInfo<Widget>() {
                            public double getWeight() {
                                    return get(eventTypeParams.fieldSetChoices, fsIter);
                            }
                            public void setPosterior(double prob) {
                                 update(eventTypeCounts.fieldSetChoices, fsIter, prob);
                            }
                            public Widget choose(Widget widget) {
                                return widget;
                            }
                        });
                } // for
            } // if
            return node;
        } // if
        else // Can use any field set
        {
            return genEFSEvent(seqNo, event, eventTypeParams.getDontcare_efs());
        }
    }

    Object genEndNode()
    {
        return hypergraph.endNode;
    }   

    /**
     * Generate sequence of events ignoring segmentation
     * @param seqNo the sequence number of the event to be generated
     * @param t0 previous eventType
     * @return
     */
    protected Object genEvents(int seqNo, final int t0, boolean allowNone)
    {
        EventsNode node = new EventsNode(seqNo, t0);
        final TrackParams cparams = params.trackParams[0];
        final TrackParams ccounts = counts.trackParams[0];
        if(hypergraph.addSumNode(node))
        {
            // (1) Choose the none event
              if (allowNone)
              {
                  final int remember_t = t0; // Don't remember none_t (since t == none_t, skip t)
                  // Check whether we are in the end of our sequence and generate
                  // the final end node (we don't want to get stuck in infinite recursion).
    //             final Object recurseNode = (c == 0) ? (seqNo < ex.trackEvents[c].length ?
                 final Object recurseNode = seqNo < 2 ?
                     genEvents(seqNo+1, remember_t, allowNone): genEndNode();
                  if(opts.useEventTypeDistrib)
                  {
                      hypergraph.addEdge(node,
                          genNoneEvent(seqNo), recurseNode,
                          new Hypergraph.HyperedgeInfo<Widget>() {
                              public double getWeight() {
                                      return get(cparams.getEventTypeChoices()[t0], cparams.none_t);
                              }
                              public void setPosterior(double prob) {
                                   update(ccounts.getEventTypeChoices()[t0], cparams.none_t, prob);
                              }
                              public Widget choose(Widget widget) {
    //                              for(int k = i; k < j; k++)
    //                              {
    //                                  widget.events[c][k] = Parameters.none_e;
    //                              }
                                  return widget;
                              }
                          });
                  } // if
                  else
                  {
                      hypergraph.addEdge(node, genNoneEvent(seqNo), recurseNode);
                  } // else
              } // if (none event)
              // (2) Choose an event type t and event e for track c
              int l=ex.events.values().size();
              for(final Event e: ex.events.values())
              {
                  final int eventId = e.getId();
                  final int eventTypeIndex = e.getEventTypeIndex();
                  final int remember_t = (indepEventTypes()) ? cparams.boundary_t : eventTypeIndex;
                  // Check whether we are in the end of our sequence and generate
                  // the final end node (we don't want to get stuck in infinite recursion).
//                  final Object recurseNode = (c == 0) ? (seqNo < ex.trackEvents[c].length ?
                  final Object recurseNode = seqNo < l ?
                      genEvents(seqNo+1, remember_t, allowNone): genEndNode();
                  if (opts.useEventTypeDistrib)
                  {
                      hypergraph.addEdge(node,
                      genEvent(seqNo, eventId), recurseNode,
                      new Hypergraph.HyperedgeInfo<Widget>() {
                          public double getWeight()
                          {
                              if(prevIndepEventTypes())
                                  return get(cparams.getEventTypeChoices()[((GenerativeEvent3Model)model).none_t()],
                                          eventTypeIndex) *
                                          (1.0d/(double)ex.getEventTypeCounts()[eventTypeIndex]); // remember_t = t under indepEventTypes
                              else
                                  return get(cparams.getEventTypeChoices()[t0], eventTypeIndex) *
                                          (1.0/(double)ex.getEventTypeCounts()[eventTypeIndex]);
                          }
                          public void setPosterior(double prob) {
                               update(ccounts.getEventTypeChoices()[t0], eventTypeIndex, prob);
                          }
                          public Widget choose(Widget widget) {
//                              for(int k = i; k < j; k++)
//                              {
//                                  widget.events[c][k] = eventId;
////                                  System.out.println(String.format("TrackNode i=%d, j=%d, t0=%s, e=%s",
////                                        i, j,
////                                        inferState.eventTypeToString(t0),
////                                        inferState.eventTypeToString(eIter)));
//                              }
                              return widget;
                          }
                      });
                  } // if
                  else
                  {
                      hypergraph.addEdge(node, genEvent(seqNo, eventId), recurseNode);
                  } // else
              } // for
            hypergraph.assertNonEmpty(node);
        }
        return node;
    }   
}
