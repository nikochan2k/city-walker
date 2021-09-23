package nikochan2k.citywalker;

import java.io.File;

public abstract class Factory {

	private File outputDir;
	private boolean noAttributes;
	private String inputSRS;
	private String outputSRS;
	
	public abstract Processor createProcessor(File input, String srs);
	
	public String getInputSRS() {
		return inputSRS;
	}

	public File getOutputDir() {
		return outputDir;
	}

	public String getOutputSRS() {
		return outputSRS;
	}

	public abstract String getTypeName();

	public boolean isNoAttributes() {
		return noAttributes;
	}

	public void setInputSRS(String inputSRS) {
		this.inputSRS = inputSRS;
	}

	public void setNoAttributes(boolean noAttributes) {
		this.noAttributes = noAttributes;
	}

	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

	public void setOutputSRS(String outputSRS) {
		this.outputSRS = outputSRS;
	}
	
	
}
