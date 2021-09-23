package nikochan2k.citywalker;

import java.io.File;

public interface Factory {

	Processor createProcessor(File input, String srs);
	
	String getTypeName();
	
	void setOutputDir(File outDir);
	
}
