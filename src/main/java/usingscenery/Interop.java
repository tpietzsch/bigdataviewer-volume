package usingscenery;

import cleargl.ClearGLDefaultEventListener;
import cleargl.ClearGLWindow;

public class Interop
{

	static class MyEventListener extends ClearGLDefaultEventListener
	{
		private ClearGLWindow clearGLWindow;

		@Override
		public void setClearGLWindow( final ClearGLWindow clearGLWindow )
		{
			this.clearGLWindow = clearGLWindow;
		}

		@Override
		public ClearGLWindow getClearGLWindow()
		{
			return clearGLWindow;
		}
	}

}
