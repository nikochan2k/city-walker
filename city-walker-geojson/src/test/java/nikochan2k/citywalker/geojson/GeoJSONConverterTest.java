package nikochan2k.citywalker.geojson;

import java.io.File;
import java.net.URL;

import org.junit.jupiter.api.Test;

import nikochan2k.citywalker.CityWalkerException;
import nikochan2k.citywalker.Parser;

class GeoJSONConverterTest {

	private void parse(String name, boolean flip) throws CityWalkerException {
		URL url = GeoJSONConverterTest.class.getResource(name);
		String path = url.getFile();
		File file = new File(path);
		GeoJSONFactory factory = new GeoJSONFactory();
		factory.setFlipXY(flip);
		Parser parser = new Parser(factory);
		parser.parse(file);
	}

	@Test
	void testPlateau() throws CityWalkerException {
		parse("53392633_bldg_6697_op2.gml", true);
	}

	@Test
	void testKashikaOrJp() throws CityWalkerException {
		parse("533954364.xml", false);
	}

	@Test
	void testHawaii() throws CityWalkerException {
		parse("Hawaii-15001-002.gml", false);
	}

	@Test
	void testHelsinki() throws CityWalkerException {
		parse("Helsinki3D_CityGML_Kalasatama_20190326.gml", false);
	}
}
