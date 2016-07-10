package usingscenery;

import static org.jocl.CL.*;

import java.io.*;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.stream.Collectors;

import org.jocl.*;

import com.jogamp.common.nio.Buffers;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.opengl.*;

import cleargl.*;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.egl.EGLContext;
import jogamp.opengl.macosx.cgl.CGL;
import jogamp.opengl.macosx.cgl.MacOSXCGLContext;
import jogamp.opengl.windows.wgl.WindowsWGLContext;
import jogamp.opengl.x11.glx.X11GLXContext;

public class TexturedQuad
{
	private final static float vertices[] = {
			0, 0, 0, 1,
			1, 0, 0, 1,
			0, 1, 0, 1,
			1, 1, 0, 1 };

	private final static float texcoords[] = {
			0, 0,
			1, 0,
			0, 1,
			1, 1 };

	private Buffer getTextureBuffer3()
	{
		final int w = 1280;
		final int h = 1280;
		final float[] lFloatArray = new float[ w * h ];
		for ( int y = 0; y < h; ++y )
			for ( int x = 0; x < w; ++x )
				lFloatArray[ y * w + x ] = ( ( float ) x ) / w;
//				lFloatArray[ y * w + x ] = ( ( float ) y ) / h;
//				lFloatArray[ y * w + x ] = ( ( float ) ( y + x ) ) / ( w + h );
		return FloatBuffer.wrap( lFloatArray );
	}

	final ClearGLDefaultEventListener lClearGLWindowEventListener = new ClearGLDefaultEventListener()
	{
		private GLProgram mGLProgram;

		private GLAttribute mPosition;

		private GLAttribute mTexCoord;

		private GLUniform mProjectionMatrixUniform;

		private GLUniform mViewMatrixUniform;

		private GLUniform mTexUnitUniform;

		private GLVertexAttributeArray mPositionAttributeArray;

		private GLVertexAttributeArray mTexCoordAttributeArray;

		private GLVertexArray mGLVertexArray;

		private GLTexture mTexture;

		private ClearGLDisplayable mClearGLWindow;

		/**
		 * Initializes the given context properties so that they may be used
		 * to create an OpenCL context for the given GL object.
		 *
		 * @param contextProperties
		 *            The context properties
		 * @param gl
		 *            The GL object
		 */
		private void initContextProperties( final cl_context_properties contextProperties, final GL gl )
		{
			// Adapted from http://jogamp.org/jocl/www/

			final GLContext glContext = gl.getContext();
			if ( !glContext.isCurrent() ) { throw new IllegalArgumentException(
					"OpenGL context is not current. This method should be called " +
							"from the OpenGL rendering thread, when the context is current." ); }

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
				final int CL_CONTEXT_PROPERTY_USE_CGL_SHAREGROUP_APPLE = 268435456;
				final long ctx = CGL.CGLGetCurrentContext();
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

		private cl_kernel loadKernel(
				final cl_context context,
				final File file,
				final String name ) throws FileNotFoundException
		{
			// Create the program from the source code
			final BufferedReader reader = new BufferedReader( new FileReader( file ) );
			final String source = reader.lines().collect( Collectors.joining( "\n" ) );
			final cl_program program = clCreateProgramWithSource( context, 1, new String[] { source }, null, null );

			// Build the program
			clBuildProgram( program, 0, null, null, null, null );

			// Create the kernel
			final cl_kernel kernel = clCreateKernel( program, name, null );

			return kernel;
	    }


		private void initCL( final GL3 gl ) throws FileNotFoundException
		{
			// The platform, device type and device number
			// that will be used
			final int platformIndex = 0;
			final long deviceType = CL_DEVICE_TYPE_GPU;
			final int deviceIndex = 1;

			// Enable exceptions and subsequently omit error checks in this
			// sample
			CL.setExceptionsEnabled( true );

			// Obtain the number of platforms
			final int numPlatformsArray[] = new int[ 1 ];
			clGetPlatformIDs( 0, null, numPlatformsArray );
			final int numPlatforms = numPlatformsArray[ 0 ];

			// Obtain a platform ID
			final cl_platform_id platforms[] = new cl_platform_id[ numPlatforms ];
			clGetPlatformIDs( platforms.length, platforms, null );
			final cl_platform_id platform = platforms[ platformIndex ];

			// Initialize the context properties
			final cl_context_properties contextProperties = new cl_context_properties();
			contextProperties.addProperty( CL_CONTEXT_PLATFORM, platform );
			initContextProperties( contextProperties, gl );

			// Obtain the number of devices for the platform
			final int numDevicesArray[] = new int[ 1 ];
			clGetDeviceIDs( platform, deviceType, 0, null, numDevicesArray );
			final int numDevices = numDevicesArray[ 0 ];

			// Obtain a device ID
			final cl_device_id devices[] = new cl_device_id[ numDevices ];
			clGetDeviceIDs( platform, deviceType, numDevices, devices, null );
			final cl_device_id device = devices[ deviceIndex ];

			System.err.println( getString( device, CL_DEVICE_NAME ) + " running " + getString( device, CL_DEVICE_VERSION ) );

			// Create a context for the selected device
			final cl_context context = clCreateContext(
					contextProperties, 1, new cl_device_id[] { device },
					null, null, null );

			// Create a command-queue for the selected device
			final cl_command_queue commandQueue = clCreateCommandQueue( context, device, 0, null );
//			final cl_command_queue commandQueue = clCreateCommandQueueWithProperties( context, device, null, null );

			mTexture.bind();

			final cl_mem sharedTexture = clCreateFromGLTexture(
					context,
					CL_MEM_WRITE_ONLY,
					GL.GL_TEXTURE_2D,
					0,
					mTexture.getId(),
					null );


			final cl_kernel kernel = loadKernel( context,
					new File( TexturedQuad.class.getResource( "filltexture.cl" ).getFile() ),
					"filltexture" );

			clSetKernelArg( kernel, 0, Sizeof.cl_mem, Pointer.to( sharedTexture ) );


			final long[] global_work_offset = null;
			final long[] global_work_size = new long[] { 1280, 1280 };
			final long[] local_work_size = null;
			final int num_events_in_wait_list = 0;
			final cl_event[] event_wait_list = null;
			final cl_event event = null;

			clEnqueueNDRangeKernel(
					commandQueue,
					kernel,
					2,
					global_work_offset,
					global_work_size,
					local_work_size,
					num_events_in_wait_list,
					event_wait_list,
					event );

			clFinish( commandQueue );

			System.out.println( kernel );


			// Read the program source code and create the program
//	        final String source = readFile("kernels/simpleGL.cl");
//	        final cl_program program = clCreateProgramWithSource(context, 1,
//	            new String[]{ source }, null, null);
//	        clBuildProgram(program, 0, null, "-cl-mad-enable", null, null);
//
//	        // Create the kernel which computes the sine wave pattern
//	        kernel = clCreateKernel(program, "sine_wave", null);
//
//	        // Set the constant kernel arguments
//	        clSetKernelArg(kernel, 1, Sizeof.cl_uint,
//	            Pointer.to(new int[]{ meshWidth }));
//	        clSetKernelArg(kernel, 2, Sizeof.cl_uint,
//	            Pointer.to(new int[]{ meshHeight }));
		}

		String getString( final cl_device_id device, final int paramName )
		{
			// Obtain the length of the string that will be queried
			final long[] size = new long[ 1 ];
			clGetDeviceInfo( device, paramName, 0, null, size );

			// Create a buffer of the appropriate size and fill it with the
			// info
			final byte[] buffer = new byte[ ( int ) size[ 0 ] ];
			clGetDeviceInfo( device, paramName, buffer.length, Pointer.to( buffer ), null );

			// Create a string from the buffer (excluding the trailing \0
			// byte)
			return new String( buffer, 0, buffer.length - 1 );
		}

		@Override
		public void init( final GLAutoDrawable pDrawable )
		{
			super.init( pDrawable );
			try
			{
				final GL lGL = pDrawable.getGL();

				// Initialize the GL_ARB_vertex_buffer_object extension
				if ( !lGL.isExtensionAvailable( "GL_ARB_vertex_buffer_object" ) )
				{
					System.out.println( "GL_ARB_vertex_buffer_object extension not available" );
				}

				lGL.glDisable( GL.GL_DEPTH_TEST );

				mGLProgram = GLProgram.buildProgram( lGL,
						TexturedQuad.class,
						"vertex.tex.glsl",
						"fragment.tex.glsl" );
//				System.out.println( mGLProgram2.getProgramInfoLog() );

				mProjectionMatrixUniform = mGLProgram.getUniform( "projMatrix" );
				mViewMatrixUniform = mGLProgram.getUniform( "viewMatrix" );

				mPosition = mGLProgram.getAttribute( "position" );
				mTexCoord = mGLProgram.getAttribute( "texcoord" );

				mTexUnitUniform = mGLProgram.getUniform( "texUnit" );
				mTexUnitUniform.setInt( 0 );

				mGLVertexArray = new GLVertexArray( mGLProgram );
				mGLVertexArray.bind();
				mPositionAttributeArray = new GLVertexAttributeArray( mPosition, 4 );
				mTexCoordAttributeArray = new GLVertexAttributeArray( mTexCoord, 2 );

				mGLVertexArray.addVertexAttributeArray( mPositionAttributeArray,
						Buffers.newDirectFloatBuffer( vertices ) );
				mGLVertexArray.addVertexAttributeArray( mTexCoordAttributeArray,
						Buffers.newDirectFloatBuffer( texcoords ) );

				mTexture = new GLTexture(
						mGLProgram,
						GLTypeEnum.Float,
						1,
						1280, 1280, 1,
						true,
						1 );
				mTexture.copyFrom( getTextureBuffer3() );
				lGL.glFinish();

				initCL( lGL.getGL3() );
			}
			catch ( GLException | IOException e )
			{
				e.printStackTrace();
			}

		}

		@Override
		public void reshape( final GLAutoDrawable pDrawable,
				final int pX,
				final int pY,
				final int pWidth,
				final int pHeight )
		{
			super.reshape( pDrawable, pX, pY, pWidth, pHeight );
			getClearGLWindow().setOrthoProjectionMatrix(
					0, 1,
					1, 0,
					1, -1 );
		}

		@Override
		public void display( final GLAutoDrawable pDrawable )
		{
			super.display( pDrawable );

			final GL lGL = pDrawable.getGL();

			lGL.glClear( GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT );

			getClearGLWindow().lookAt(
					0, 0, 1,
					0, 0, -1,
					0, 1, 0 );

			mGLProgram.use( lGL );

			mTexture.bind( mGLProgram );

			mProjectionMatrixUniform.setFloatMatrix( getClearGLWindow().getProjectionMatrix(), false );
			mViewMatrixUniform.setFloatMatrix( getClearGLWindow().getViewMatrix(), false );

			mGLVertexArray.draw( GL.GL_TRIANGLE_STRIP );

			// Check out error
			final int error = lGL.glGetError();
			if ( error != 0 )
			{
				System.err.println( "ERROR on render : " + error );
			}
		}

		@Override
		public void dispose( final GLAutoDrawable pDrawable )
		{
			super.dispose( pDrawable );

			mGLVertexArray.close();
			mTexCoordAttributeArray.close();
			mPositionAttributeArray.close();
			mGLProgram.close();
		}

		@Override
		public void setClearGLWindow( final ClearGLWindow pClearGLWindow )
		{
			mClearGLWindow = pClearGLWindow;
		}

		@Override
		public ClearGLDisplayable getClearGLWindow()
		{
			return mClearGLWindow;
		}
	};

	public void demo() throws InterruptedException
	{
		lClearGLWindowEventListener.setDebugMode( false );

		try ( ClearGLDisplayable lClearGLWindow = new ClearGLWindow(
				"demo: ClearGLWindow", 512, 512, lClearGLWindowEventListener ) )
		{
			lClearGLWindow.setVisible( true );

			while ( lClearGLWindow.isVisible() )
			{
				Thread.sleep( 100 );
			}
		}
	}

	public static void main( final String[] args ) throws InterruptedException
	{
//		final AbstractGraphicsDevice lDefaultDevice = GLProfile.getDefaultDevice();
//		final GLProfile lProfile = GLProfile.getMaxProgrammable( true );
//		final GLCapabilities lCapabilities = new GLCapabilities( lProfile );
//
//		System.out.println( "Device: " + lDefaultDevice );
//		System.out.println( "Capabilities: " + lCapabilities );
//		System.out.println( "Profile: " + lProfile );

		final TexturedQuad lClearGLDemo = new TexturedQuad();
		lClearGLDemo.demo();
	}
}
