package nikochan2k.citywalker.geojson;

import java.io.File;

import nikochan2k.citywalker.Factory;
import nikochan2k.citywalker.Processor;

public class GeoJSONFactory extends Factory {

	@Override
	public String getTypeName() {
		return "geojson";
	}

	@Override
	public Processor createProcessor(File input, String srs) {
		return new GeoJSONConverter(input, this.getOutputDir(), srs);
	}

}
