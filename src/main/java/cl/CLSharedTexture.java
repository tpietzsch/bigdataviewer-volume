package cl;

import static org.jocl.CL.clCreateFromGLTexture;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.cl_mem;

import cleargl.GLTexture;

public class CLSharedTexture
{
	private final CLSharedContext context;

	private final GLTexture texture;

	private final cl_mem memobj;

	private Pointer pointer;

	/**
	 * Set up a shared OpenGL texture.
	 *
	 * @param context
	 *            the shared OpenCL context.
	 * @param texture
	 *            GL texture to share.
	 * @param flags
	 *            A bit-field that is used to specify usage information. Refer
	 *            to the table for clCreateBuffer for a description of flags.
	 *            Only the values CL_MEM_READ_ONLY, CL_MEM_WRITE_ONLY and
	 *            CL_MEM_READ_WRITE can be used.
	 */
	public CLSharedTexture(
			final CLSharedContext context,
			final GLTexture texture,
			final long flags )
	{
		this.context = context;
		this.texture = texture;
		this.memobj = clCreateFromGLTexture(
				context.getContext(),
				flags,
				texture.getTextureTarget(),
				// GL.GL_TEXTURE_2D or GL.GL_TEXTURE_3D
				0,
				texture.getId(),
				null );
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
	}
}
