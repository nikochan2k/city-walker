package nikochan2k.citywalker.geojson;

import java.io.File;

import nikochan2k.citywalker.Factory;
import nikochan2k.citywalker.Processor;

public class GeoJSONFactory implements Factory {

	private final File outDir;
	
	public GeoJSONFactory() {
		this.outDir = null;
	}
	
	public GeoJSONFactory(File outDir) {
		this.outDir = outDir;
	}
	
	@Override
	public Processor createProcessor(File input, String srs) {
		return new GeoJSONConverter(input, outDir, srs);
	}

}
