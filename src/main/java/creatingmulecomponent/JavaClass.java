package creatingmulecomponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.collections.CollectionUtils;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

@SuppressWarnings({ "rawtypes", "deprecation", "unchecked" })
public class JavaClass implements Callable {

	@Override
	public Object onCall(MuleEventContext muleEventContext) throws Exception {
		org.mule.api.ThreadSafeAccess.AccessControl.setAssertMessageAccess(false);
		final List payloadAsList;
		List<Future<MuleEvent>> returnedFuture;
		if (Collection.class.isAssignableFrom(muleEventContext.getMessage().getPayload().getClass())) {
			payloadAsList = muleEventContext.getMessage().getPayload(List.class);
			final List<SingleMessageExec> collection = new ArrayList<>();
			for (int index = 0; index < payloadAsList.size(); index++) {
				Object payload = payloadAsList.get(index);
				collection.add(
						new SingleMessageExec(muleEventContext, payload, getCallignFlow(RequestContext.getEvent())));
			}
			ExecutorService exec = Executors.newFixedThreadPool(collection.size());
			returnedFuture = exec.invokeAll(collection);
			return test(returnedFuture, RequestContext.getEvent());
		} else {
			throw new Exception();
		}
	}

	private List test(List<Future<MuleEvent>> list, MuleEvent actualEvent) throws Exception {
		if (CollectionUtils.isNotEmpty(list)) {
			List payload = new ArrayList();
			Set<String> flowVarsNames = new HashSet<>();
			for (Future<MuleEvent> f : list) {
				final MuleEvent event = f.get();
				payload.add(event.getMessage().getPayload());
				flowVarsNames.addAll(event.getFlowVariableNames());
				for (String s : flowVarsNames) {
					actualEvent.setFlowVariable(s, event.getFlowVariable(s));
	}
			}
			return payload;
		} else {
			throw new Exception();
		}
	}

	private String getCallignFlow(MuleEvent event) {
		return event.getFlowVariable("flowToCall");
	}
}
