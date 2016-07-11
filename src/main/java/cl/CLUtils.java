package cl;

import static org.jocl.CL.*;

import java.lang.reflect.Array;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;

import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLContext;

import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.egl.EGLContext;
import jogamp.opengl.macosx.cgl.CGL;
import jogamp.opengl.macosx.cgl.MacOSXCGLContext;
import jogamp.opengl.windows.wgl.WindowsWGLContext;
import jogamp.opengl.x11.glx.X11GLXContext;

public class CLUtils
{
	public static final int CL_CONTEXT_PROPERTY_USE_CGL_SHAREGROUP_APPLE = 268435456;

	/**
	 * Initializes the given context properties so that they may be used to
	 * create an OpenCL context for the given GL object.
	 * <p>
	 * Modified version of http://www.jocl.org/samples/JOCLSimpleGL3.java.
	 * Probably works for OS X (at least on my MacBook).
	 *
	 * @param contextProperties
	 *            The context properties
	 * @param gl
	 *            The GL object
	 */
	public static void initSharedContextProperties(
			final cl_context_properties contextProperties,
			final GL gl )
	{
		final GLContext glContext = gl.getContext();
		if ( !glContext.isCurrent() )
			throw new IllegalArgumentException(
					"OpenGL context is not current. This method should be called " +
							"from the OpenGL rendering thread, when the context is current." );

		final long glContextHandle = glContext.getHandle();
		final GLContextImpl glContextImpl = ( GLContextImpl ) glContext;
		final GLDrawableImpl glDrawableImpl = glContextImpl.getDrawableImpl();
		final NativeSurface nativeSurface = glDrawableImpl.getNativeSurface();

		if ( glContext instanceof X11GLXContext )
		{
			final long displayHandle = nativeSurface.getDisplayHandle();
			contextProperties.addProperty( CL_GL_CONTEXT_KHR, glContextHandle );
			contextProperties.addProperty( CL_GLX_DISPLAY_KHR, displayHandle );
		}
		else if ( glContext instanceof WindowsWGLContext )
		{
			final long surfaceHandle = nativeSurface.getSurfaceHandle();
			contextProperties.addProperty( CL_GL_CONTEXT_KHR, glContextHandle );
			contextProperties.addProperty( CL_WGL_HDC_KHR, surfaceHandle );
		}
		else if ( glContext instanceof MacOSXCGLContext )
		{
			final long ctx = CGL.CGLGetCurrentContext();
			// Note: ctx != gl.getContext().getHandle()
			// I don't know what that means...
			final long sg = CGL.CGLGetShareGroup( ctx );
			contextProperties.addProperty( CL_CONTEXT_PROPERTY_USE_CGL_SHAREGROUP_APPLE, sg );
		}
		else if ( glContext instanceof EGLContext )
		{
			final long displayHandle = nativeSurface.getDisplayHandle();
			contextProperties.addProperty( CL_GL_CONTEXT_KHR, glContextHandle );
			contextProperties.addProperty( CL_EGL_DISPLAY_KHR, displayHandle );
		}
		else
		{
			throw new RuntimeException( "unsupported GLContext: " + glContext );
		}
	}

	public static final String getDeviceInfo(
			final cl_device_id device,
			final int paramName )
	{
		// Obtain the length of the string that will be queried
		final long[] size = new long[ 1 ];
		clGetDeviceInfo( device, paramName, 0, null, size );

		// Create a buffer of the appropriate size and fill it with the info
		final byte[] buffer = new byte[ ( int ) size[ 0 ] ];
		clGetDeviceInfo( device, paramName, buffer.length, Pointer.to( buffer ), null );

		// Create a string from the buffer (excluding the trailing \0 byte)
		return new String( buffer, 0, buffer.length - 1 );
	}

	@FunctionalInterface
	public static interface CLFunction< T >
	{
		void fn( int thingsSize, T[] things, int[] num );
	}

	public static < T > T[] query( final Class< T > klass, final CLFunction< T > fn )
	{
		final int[] num = new int[ 1 ];
		fn.fn( 0, null, num );
		@SuppressWarnings( "unchecked" )
		final T[] things = ( T[] ) Array.newInstance( klass, num[ 0 ] );
		fn.fn( num[ 0 ], things, null );
		return things;
	}

	// TODO remove!?
	public static void setExceptionsEnabled( final boolean enabled )
	{
		CL.setExceptionsEnabled( enabled );
	}
}
