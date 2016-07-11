package cl;

import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateProgramWithSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.jocl.CL;
import org.jocl.cl_program;

public class CLProgram
{
	private final cl_program program;

	private final Map< String, CLKernel > kernels;

	private boolean released;

	public CLProgram(
			final CLContext context,
			final File sourceFile ) throws IOException
	{
		this( context, read( sourceFile ) );
	}

	public CLProgram(
			final CLContext context,
			final String[] source )
	{
		program = clCreateProgramWithSource( context.getContext(), source.length, source, null, null );
		clBuildProgram( program, 0, null, null, null, null );
		kernels = new HashMap<>();
		released = false;
	}

	public CLKernel getKernel( final String name )
	{
		if ( released )
			throw new IllegalStateException();

		CLKernel kernel = kernels.get( name );
		if ( kernel == null)
		{
			kernel = new CLKernel( this, name );
			kernels.put( name, kernel );
		}
		return kernel;
	}

	private static String[] read( final File file ) throws IOException
	{
		try ( BufferedReader reader = new BufferedReader( new FileReader( file ) ) )
		{
			return reader
					.lines()
					.collect( Collectors.toList() )
					.toArray( new String[ 0 ] );
		}
	}

	public cl_program getProgram()
	{
		return program;
	}

	public void release()
	{
		CL.clReleaseProgram( program );
		released = true;
	}
}
