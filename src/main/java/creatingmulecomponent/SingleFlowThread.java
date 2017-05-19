package creatingmulecomponent;

import java.util.concurrent.Callable;

import org.mule.DefaultMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.construct.Flow;

@SuppressWarnings("deprecation")
public class SingleFlowThread implements Callable<MuleEvent>{

	private MuleEventContext muleEventContext;
	private MuleEvent actualEvent;
	private String flowName;

	public SingleFlowThread(MuleEventContext muleEventContext, Object payload, String flowName) {
		this.muleEventContext = muleEventContext;
		this.actualEvent = DefaultMuleEvent.copy(org.mule.RequestContext.getEvent());
		actualEvent.getMessage().setPayload(payload);
		this.flowName = flowName;
	}

	@Override
	public MuleEvent call() throws Exception {
		Flow flow = (Flow) muleEventContext.getMuleContext().getRegistry().lookupFlowConstruct(this.flowName);
		MuleEvent event = flow.process((MuleEvent) actualEvent);
		return event;
	}
}
