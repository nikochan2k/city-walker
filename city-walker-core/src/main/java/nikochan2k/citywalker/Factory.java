package nikochan2k.citywalker;

import java.io.File;

public abstract class Factory {

	private boolean flipXY;
	private String inputSRS;
	private boolean noAttributes;
	private File outputDir;
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

	public boolean isFlipXY() {
		return flipXY;
	}

	public boolean isNoAttributes() {
		return noAttributes;
	}

	public void setFlipXY(boolean flipXY) {
		this.flipXY = flipXY;
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
