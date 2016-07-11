package cl;

import org.jocl.cl_command_queue;
import org.jocl.cl_context;

public interface CLContext
{
	public cl_context getContext();

	public cl_command_queue getCommandQueue();
}
