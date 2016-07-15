package cl;

import static org.jocl.CL.*;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.cl_command_queue;
import org.jocl.cl_event;
import org.jocl.cl_image_desc;
import org.jocl.cl_image_format;
import org.jocl.cl_mem;

import net.imglib2.EuclideanSpace;

public class CLImage implements EuclideanSpace
{
	private final CLContext context;

	private final cl_image_format image_format;

	private final cl_image_desc image_desc;

	private final cl_mem memobj;

	private final int numDimensions;

	private Pointer pointer;

	private boolean released;

	/**
	 * Create an OpenCL image.
	 *
	 * @param context
	 *            the shared OpenCL context.
	 * @param flags
	 *            A bit-field that is used to specify usage information. Refer
	 *            to the table for clCreateBuffer for a description of flags.
	 *            Only the values CL_MEM_READ_ONLY, CL_MEM_WRITE_ONLY and
	 *            CL_MEM_READ_WRITE can be used.
	 * @param image_channel_order
	 *            channel order, e.g., {@link CL#CL_R}.
	 * @param image_channel_data_type
	 *            channel data type, e.g., {@link CL#CL_FLOAT}.
	 * @param dims
	 *            image size (in 1, 2, or 3 dimensions).
	 */
	public CLImage(
			final CLContext context,
			final long flags,
			final int image_channel_order,
			final int image_channel_data_type,
			final long... dims )
	{
		this.context = context;

		image_format = new cl_image_format();
		image_format.image_channel_order = image_channel_order;
		image_format.image_channel_data_type = image_channel_data_type;

		image_desc = new cl_image_desc();
		numDimensions = dims.length;
		if ( numDimensions == 1 )
		{
			image_desc.image_type = CL_MEM_OBJECT_IMAGE1D;
			image_desc.image_width = dims[ 0 ];
			image_desc.image_height = 1;
			image_desc.image_depth = 1;
		}
		else if ( numDimensions == 2 )
		{
			image_desc.image_type = CL_MEM_OBJECT_IMAGE2D;
			image_desc.image_width = dims[ 0 ];
			image_desc.image_height = dims[ 1 ];
			image_desc.image_depth = 1;
		}
		else if ( numDimensions == 3 )
		{
			image_desc.image_type = CL_MEM_OBJECT_IMAGE3D; // CL_MEM_OBJECT_IMAGE2D
			image_desc.image_width = dims[ 0 ];
			image_desc.image_height = dims[ 1 ];
			image_desc.image_depth = dims[ 2 ];
		}
		else
			throw new IllegalArgumentException();

		final Pointer host_ptr = null;
		final int errcode_ret[] = null;

	    memobj = clCreateImage( context.getContext(), flags, image_format, image_desc, host_ptr, errcode_ret );
	    released = false;
	}

	public Pointer getPointer()
	{
		if ( pointer == null )
			pointer = Pointer.to( memobj );
		return pointer;
	}

	public void release()
	{
		CL.clReleaseMemObject( memobj );
		released = true;
	}

	public void enqueueWrite( final Pointer ptr )
	{
		final long[] origin = new long[] { 0, 0, 0 };
		final long[] region = new long[] {
				image_desc.image_width,
				image_desc.image_height,
				image_desc.image_depth };
		enqueueWrite( ptr, origin, region );
	}

	public void enqueueWrite(
			final Pointer ptr,
			final long ... originAndRegion )
	{
		if ( originAndRegion.length != 2 * numDimensions )
			throw new IllegalArgumentException();
		final long[] origin = new long[] { 0, 0, 0 };
		final long[] region = new long[] { 1, 1, 1 };
		for ( int d = 0; d < numDimensions; ++d )
		{
			origin[ d ] = originAndRegion[ d ];
			region[ d ] = originAndRegion[ d + numDimensions ];
		}
		enqueueWrite( ptr, origin, region );
	}

	public void enqueueWrite(
			final Pointer ptr,
			final long[] origin,
			final long[] region )
	{
		final cl_command_queue queue = context.getCommandQueue();
		final boolean blocking_write = true;
		final long input_row_pitch = 0;
		final long input_slice_pitch = 0;
		final int num_events_in_wait_list = 0;
		final cl_event[] event_wait_list = null;
		final cl_event event = null;
		clEnqueueWriteImage(
				queue, memobj, blocking_write, origin, region, input_row_pitch, input_slice_pitch, ptr, num_events_in_wait_list, event_wait_list, event );
	}

	@Override
	public int numDimensions()
	{
		return numDimensions;
	}
}
