package nikochan2k.citywalker.geojson;

import java.io.File;

import nikochan2k.citywalker.Factory;
import nikochan2k.citywalker.Processor;

public class GeoJSONFactory implements Factory {

	private File outDir;
	
	@Override
	public Processor createProcessor(File input, String srs) {
		return new GeoJSONConverter(input, outDir, srs);
	}

	@Override
	public String getTypeName() {
		return "geojson";
	}

	@Override
	public void setOutputDir(File outDir) {
		this.outDir = outDir;
	}

}
