package cl;

import static org.jocl.CL.*;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.cl_image_desc;
import org.jocl.cl_image_format;
import org.jocl.cl_mem;

public class CLImage
{
	private final CLContext context;

	private final cl_image_format image_format;

	private final cl_image_desc image_desc;

	private final cl_mem memobj;

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
		image_format.image_channel_order = CL_R;
		image_format.image_channel_data_type = CL_FLOAT;

		image_desc = new cl_image_desc();
		if ( dims.length == 1 )
		{
			image_desc.image_type = CL_MEM_OBJECT_IMAGE1D;
			image_desc.image_width = dims[ 0 ];
		}
		else if ( dims.length == 2 )
		{
			image_desc.image_type = CL_MEM_OBJECT_IMAGE2D;
			image_desc.image_width = dims[ 0 ];
			image_desc.image_height = dims[ 1 ];
		}
		else if ( dims.length == 3 )
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
}
