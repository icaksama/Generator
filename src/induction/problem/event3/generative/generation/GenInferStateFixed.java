package induction.problem.event3.generative.generation;

import induction.problem.event3.generative.GenerativeEvent3Model;
import edu.uci.ics.jung.graph.Graph;
import induction.Hypergraph;
import induction.ngrams.NgramModel;
import induction.problem.InferSpec;
import induction.problem.event3.Event;
import induction.problem.event3.Example;
import induction.problem.event3.Widget;
import induction.problem.event3.nodes.TrackNode;
import induction.problem.event3.params.Parameters;
import induction.problem.event3.params.Params;
import induction.problem.event3.params.TrackParams;

/**
 *
 * @author konstas
 */
public class GenInferStateFixed extends GenInferState
{
    public GenInferStateFixed(GenerativeEvent3Model model, Example ex, Params params,
            Params counts, InferSpec ispec, NgramModel ngramModel)
    {
        super(model, ex, params, counts, ispec, ngramModel);
    }

    public GenInferStateFixed(GenerativeEvent3Model model, Example ex, Params params,
            Params counts, InferSpec ispec, NgramModel ngramModel, Graph graph)
    {
        super(model, ex, params, counts, ispec, ngramModel, graph);
    }

    @Override
    protected Object genTrack(final int i, final int j, final int t0, final int c,
                       boolean allowNone, boolean allowReal)
    {

        final TrackParams cparams = params.trackParams[c];
        final TrackParams ccounts = counts.trackParams[c];

        if(i == j)
        {
            if(indepEventTypes())
                return hypergraph.endNode;
            else
            {
                return genStopNode(i, t0, cparams, ccounts);
            } // else
        } // if (i == j)
//        TrackNode node = new TrackNode(i, j, t0, c, allowNone, allowReal);
        TrackNode node = new TrackNode(i, j, t0, c);
        // WARNING: allowNone/allowReal might not result in any valid nodes
        if(hypergraph.addSumNode(node))
        {
            // (1) Choose the none event
          if (allowNone && (!trueInfer || ex.getTrueWidget() == null ||
              ex.getTrueWidget().hasNoReachableContiguousEvents(i, j, c)))
          {
              final int remember_t = opts.conditionNoneEvent ? cparams.none_t : t0; // Condition on none_t or not
              Object recurseNode = (c == 0) ? genEvents(j, remember_t) : hypergraph.endNode;
              if(opts.useEventTypeDistrib)
              {
                  hypergraph.addEdge(node,
                      genNoneEvent(i, j, c), recurseNode,
                      new Hypergraph.HyperedgeInfo<Widget>() {
                          public double getWeight() {
                              if(prevIndepEventTypes())
                                  return get(cparams.getEventTypeChoices()[cparams.boundary_t], cparams.none_t);
                              else
                                  return get(cparams.getEventTypeChoices()[t0], cparams.none_t);
                          }
                          public void setPosterior(double prob) { }
                          public Widget choose(Widget widget) {
                              for(int k = i; k < j; k++)
                              {
                                  widget.getEvents()[c][k] = Parameters.none_e;
                              }
                              return widget;
                          }
                      });
              } // if
              else
              {
                  hypergraph.addEdge(node,
                      genNoneEvent(i, j, c), recurseNode,
                      new Hypergraph.HyperedgeInfo<Widget>() {
                          public double getWeight() {
                              return 1.0;
                          }
                          public void setPosterior(double prob) { }
                          public Widget choose(Widget widget) {
                              for(int k = i; k < j; k++)
                              {
                                  widget.getEvents()[c][k] = Parameters.none_e;
                              }
                              return widget;
                          }
                      });
              } // else
          } // if
          // (2) Choose an event type t and event e for track c
          for(final Event e : ex.events.values())
          {
              final int eventId = e.getId();
              final int eventTypeIndex = e.getEventTypeIndex();
              if (allowReal &&
                      (!trueInfer || ex.getTrueWidget() == null ||
                      ex.getTrueWidget().hasContiguousEvents(i, j, eventId)))
              {
                  final int remember_t = (indepEventTypes()) ? cparams.boundary_t : eventTypeIndex;
                  final Object recurseNode = (c == 0) ? genEvents(j, remember_t) : hypergraph.endNode;
                  if (opts.useEventTypeDistrib)
                  {
                      hypergraph.addEdge(node,
                      genEvent(i, j, c, eventId), recurseNode,
                      new Hypergraph.HyperedgeInfo<Widget>() {
                          public double getWeight()
                          {
                              if(prevIndepEventTypes())
                                  return get(cparams.getEventTypeChoices()[cparams.boundary_t],
                                          eventTypeIndex) *
                                          (1.0d/(double)ex.getEventTypeCounts()[eventTypeIndex]); // remember_t = t under indepEventTypes
                              else
                                  return get(cparams.getEventTypeChoices()[t0], eventTypeIndex) *
                                          (1.0/(double)ex.getEventTypeCounts()[eventTypeIndex])*(1-segPenalty[j-i]);
                          }
                          public void setPosterior(double prob) { }
                          public Widget choose(Widget widget) {
                              for(int k = i; k < j; k++)
                              {
                                  widget.getEvents()[c][k] = eventId;
                              }
                              return widget;
                          }
                      });
                  } // if
                  else
                  {
                      hypergraph.addEdge(node,
                      genEvent(i, j, c, eventId), recurseNode,
                      new Hypergraph.HyperedgeInfo<Widget>() {
                          public double getWeight() {
                                  return 1.0;
                          }
                          public void setPosterior(double prob) { }
                          public Widget choose(Widget widget) {
                              for(int k = i; k < j; k++)
                              {
                                  widget.getEvents()[c][k] = eventId;
                              }
                              return widget;
                          }
                      });
                  } // else
              } // if
          } // for
          // (3) Choose to STOP
          hypergraph.addEdge(node, genStopNode(i, t0, cparams, ccounts));
        } // if
        return node;
    }
}
