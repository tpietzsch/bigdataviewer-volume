package bdv.volume;

import bdv.BigDataViewer;
import bdv.export.ProgressWriterConsole;
import bdv.viewer.ViewerOptions;
import mpicbg.spim.data.SpimDataException;

public class Main
{
	public static void main( final String[] args ) throws SpimDataException
	{
		final String fn = "/Users/pietzsch/workspace/data/111010_weber_full.xml";

		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		final BigDataViewer bdv = BigDataViewer.open(
				fn,
				"bdv",
				new ProgressWriterConsole(),
				ViewerOptions.options() );


	}
}
