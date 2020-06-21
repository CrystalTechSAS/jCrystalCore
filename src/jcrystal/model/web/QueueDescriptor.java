package jcrystal.model.web;

import java.util.ArrayList;

import jcrystal.reflection.annotations.async.Queue;
import jcrystal.utils.StringUtils;

public class QueueDescriptor {
	public final String userName;
	public final String internalId;
	public final Queue queue;
	public ArrayList<JCrystalWebService> tasks = new ArrayList<>();
	public QueueDescriptor(Queue queue) {
		this.queue = queue;
		this.userName = StringUtils.camelizar(queue.name());
		this.internalId = queue.name();
	}
}
