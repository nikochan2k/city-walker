package nikochan2k.citywalker;

import java.io.File;

public abstract class Converter extends Processor {

	protected final File output;

	protected Converter(File input) {
		this(input, null);
	}

	protected Converter(File input, File outDir) {
		String fileName = input.getName();
		int lastIndex = fileName.lastIndexOf('.');
		String baseName;
		if(0 < lastIndex) {
			baseName = fileName.substring(0, lastIndex);
		} else {
			baseName = fileName;
		}
		String outName = baseName + getExtension();
		if (outDir != null) {
			this.output = new File(outDir, outName);
		} else {
			this.output = new File(input.getParent(), outName);
		}
	}
	
	protected abstract String getExtension();

}
