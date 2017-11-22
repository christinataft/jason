package jason.stdlib;

import jason.JasonException;
import jason.asSemantics.Event;
import jason.asSemantics.GoalListener;
import jason.asSemantics.GoalListener.FinishStates;
import jason.asSemantics.IMCondition;
import jason.asSemantics.IntendedMeans;
import jason.asSemantics.Intention;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;
import jason.asSyntax.Term;
import jason.asSyntax.Trigger.TEOperator;

/**
  <p>Internal action:
  <b><code>.fail_goal(<i>G</i>)</code></b>.

  <p>Description: aborts goals <i>G</i> in the agent circumstance as if a plan
  for such goal had failed. Assuming that one of the plans requiring <i>G</i> was
  <code>G0 &lt;- !G; ...</code>, an event <code>-!G0</code> is generated. In
  case <i>G</i> was triggered by <code>!!G</code> (and therefore it is
  not a subgoal, as happens also when an "achieve" performative is used),
  the generated event is <code>-!G</code>.  A literal <i>G</i>
  is a goal if there is a triggering event <code>+!G</code> in any plan within
  any intention; also note that intentions can be suspended hence appearing
  in sets E, PA, or PI of the agent's circumstance as well.
  <br/>
  The meta-event <code>^!G[state(failed)]</code> is produced.

  <p>Example:<ul>

  <li> <code>.fail_goal(go(1,3))</code>: aborts any attempt to achieve
  goals such as <code>!go(1,3)</code> as if a plan for it had failed. Assuming that
  it is a subgoal in the plan <code>get_gold(X,Y) &lt;- !go(X,Y); pick.</code>, the
  generated event is <code>-!get_gold(1,3)</code>.

  </ul>

  (Note: this internal action was introduced in a DALT 2006 paper, where it was called .dropGoal(G,false).)

  @see jason.stdlib.intend
  @see jason.stdlib.desire
  @see jason.stdlib.drop_all_desires
  @see jason.stdlib.drop_all_events
  @see jason.stdlib.drop_all_intentions
  @see jason.stdlib.drop_intention
  @see jason.stdlib.drop_desire
  @see jason.stdlib.succeed_goal
  @see jason.stdlib.current_intention
  @see jason.stdlib.suspend
  @see jason.stdlib.suspended
  @see jason.stdlib.resume

 */
public class fail_goal extends succeed_goal {

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        drop(ts, (Literal)args[0], un);
        return true;
    }

    /* returns: >0 the intention was changed
     *           1 = intention must continue running
     *           2 = fail event was generated and added in C.E
     *           3 = simply removed without event
     */
    @Override
    public int dropIntention(Intention i, IMCondition c, TransitionSystem ts, Unifier un) throws JasonException {
        if (i != null) {
        	IntendedMeans im = i.dropGoal(c, un);
        	if (im != null) {
                // notify listener
                if (ts.hasGoalListener())
                    for (GoalListener gl: ts.getGoalListeners())
                        gl.goalFailed(im.getTrigger());

                // generate failure event
                Event failEvent = null;
                if (!i.isFinished()) {
                    failEvent = ts.findEventForFailure(i, i.peek().getTrigger());
                } else { // should be an "else" (see SMC pattern) 
                	// it was an intention with g as the only IM (that was dropped), normally when !! is used
                    failEvent = ts.findEventForFailure(i, im.getTrigger()); // find fail event for the goal just dropped
                }
                if (failEvent != null) {
                    ts.getC().addEvent(failEvent);
                    ts.getLogger().fine("'.fail_goal("+im.getTrigger()+")' is generating a goal deletion event: " + failEvent.getTrigger());
                    return 2;
                } else { // i is finished or without failure plan
                    ts.getLogger().fine("'.fail_goal("+im.getTrigger()+")' is removing the intention without event:\n" + i);
                    if (ts.hasGoalListener())
                        for (GoalListener gl: ts.getGoalListeners())
                            gl.goalFinished(im.getTrigger(), FinishStates.unachieved);

                    i.fail(ts.getC());
                    return 3;
                }
            }
        }
        return 0;
    }

    @Override
    void dropInEvent(TransitionSystem ts, Event e, Intention i) throws Exception {
        e.getTrigger().setTrigOp(TEOperator.del);
    }
}
