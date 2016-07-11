package cl;

import static org.jocl.CL.clSetKernelArg;

import org.jocl.CL;
import org.jocl.Sizeof;
import org.jocl.cl_kernel;

public class CLKernel
{
	private final String name;

	private final cl_kernel kernel;

	private boolean released;

	CLKernel( final CLProgram program, final String name )
	{
		this.name = name;
		kernel = CL.clCreateKernel( program.getProgram(), name, null );
		released = false;
	}

	public void setArg(
			final int argIndex,
			final CLSharedTexture texture )
	{
		if ( released )
			throw new IllegalStateException();

		clSetKernelArg( kernel, argIndex, Sizeof.cl_mem, texture.getPointer() );
	}

	public cl_kernel getKernel()
	{
		return kernel;
	}

	public void release()
	{
		CL.clReleaseKernel( kernel );
		released = true;
	}

	@Override
	public String toString()
	{
		return kernel + " (name = " + name + ")";
	}
}
