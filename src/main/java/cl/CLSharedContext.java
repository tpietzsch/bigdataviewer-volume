package cl;

import static cl.CLUtils.*;
import static org.jocl.CL.*;

import org.jocl.CL;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_platform_id;

import com.jogamp.opengl.GL;

public class CLSharedContext implements CLContext
{
	public CLSharedContext( final GL gl )
	{
		this( gl,
				// The platform, device type and device number that will be used
				0, // int platformIndex
				CL_DEVICE_TYPE_GPU, // long deviceType
				0, // int deviceIndex
				true // boolean exceptionsEnabled
		);
	}

	private final cl_device_id device;

	private final cl_context context;

	private final cl_command_queue commandQueue;

	public CLSharedContext(
			final GL gl,
			final int platformIndex,
			final long deviceType,
			final int deviceIndex,
			final boolean exceptionsEnabled )
	{
		// Enable exceptions and subsequently omit error checks in this sample
		CL.setExceptionsEnabled( exceptionsEnabled );

		// Obtain the platform ID
		final cl_platform_id[] platforms = query( cl_platform_id.class,
				( l, a, n ) -> clGetPlatformIDs( l, a, n ) );
		final cl_platform_id platform = platforms[ platformIndex ];

		// Initialize the context properties
		final cl_context_properties contextProperties = new cl_context_properties();
		contextProperties.addProperty( CL_CONTEXT_PLATFORM, platform );
		initSharedContextProperties( contextProperties, gl );

		// Obtain a device ID
		final cl_device_id devices[] = query( cl_device_id.class,
				( l, a, n ) -> clGetDeviceIDs( platform, deviceType, l, a, n ) );
		device = devices[ deviceIndex ];

		// TODODEBUG
		System.err.println( getDeviceInfo( device, CL_DEVICE_NAME ) + " running " + getDeviceInfo( device, CL_DEVICE_VERSION ) );

		// Create a context for the selected device
		context = clCreateContext(
				contextProperties, 1, new cl_device_id[] { device },
				null, null, null );

		// Create a command-queue for the selected device
		commandQueue = clCreateCommandQueue( getContext(), device, 0, null );
	}

	@Override
	public cl_context getContext()
	{
		return context;
	}

	@Override
	public cl_command_queue getCommandQueue()
	{
		return commandQueue;
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append( getDeviceInfo( device, CL_DEVICE_NAME ) );
		sb.append( " running " );
		sb.append( getDeviceInfo( device, CL_DEVICE_VERSION ) );
		return sb.toString();
	}
}
