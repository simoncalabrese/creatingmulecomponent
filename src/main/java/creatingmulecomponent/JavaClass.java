package creatingmulecomponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

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
				collection.add(new SingleMessageExec(muleEventContext, payload, "testFlow"));
			}
			ExecutorService exec = Executors.newFixedThreadPool(collection.size());
			returnedFuture = exec.invokeAll(collection);
			
		} else {
			throw new Exception();
		}
		return returnedFuture;
	}

}
