package usingscenery;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.FloatBuffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLException;

import cleargl.ClearGLDefaultEventListener;
import cleargl.ClearGLDisplayable;
import cleargl.ClearGLWindow;
import cleargl.GLAttribute;
import cleargl.GLProgram;
import cleargl.GLTexture;
import cleargl.GLTypeEnum;
import cleargl.GLUniform;
import cleargl.GLVertexArray;
import cleargl.GLVertexAttributeArray;

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
//				lFloatArray[ y * w + x ] = ( ( float ) x ) / w;
//				lFloatArray[ y * w + x ] = ( ( float ) y ) / h;
				lFloatArray[ y * w + x ] = ( ( float ) ( y + x ) ) / ( w + h );
		return FloatBuffer.wrap( lFloatArray );
	}

	public void demo() throws InterruptedException
	{
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

			@Override
			public void init( final GLAutoDrawable pDrawable )
			{
				super.init( pDrawable );
				try
				{
					final GL lGL = pDrawable.getGL();
					lGL.glDisable( GL.GL_DEPTH_TEST );

					mGLProgram = GLProgram.buildProgram( lGL,
							TexturedQuad.class,
							"vertex.tex.glsl",
							"fragment.tex.glsl" );
//					System.out.println( mGLProgram2.getProgramInfoLog() );

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
