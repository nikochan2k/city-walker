package nikochan2k.citywalker.geojson;
import java.io.File;
import java.net.URL;

import org.citygml4j.builder.jaxb.CityGMLBuilderException;
import org.citygml4j.xml.io.reader.CityGMLReadException;
import org.junit.jupiter.api.Test;

import nikochan2k.citywalker.Parser;

class GeoJSONConverterTest {

	private void parse(String name, boolean flip) throws CityGMLBuilderException, CityGMLReadException {
		URL url = GeoJSONConverterTest.class.getResource(name);
		String path = url.getFile();
		File file = new File(path);
		GeoJSONFactory factory = new GeoJSONFactory();
		Parser parser = new Parser(factory);
		parser.setFlipXY(flip);
		parser.parse(file);
	}
	
	@Test
	void testPlateau() throws CityGMLBuilderException, CityGMLReadException {
		parse("53392633_bldg_6697_op2.gml", true);
	}
	
	@Test
	void testKashikaOrJp() throws CityGMLBuilderException, CityGMLReadException {
		parse("533954364.xml", false);
	}
	
	@Test
	void testHawaii() throws CityGMLBuilderException, CityGMLReadException {
		parse("Hawaii-15001-002.gml", false);
	}

	@Test
	void testHelsinki() throws CityGMLBuilderException, CityGMLReadException {
		parse("Helsinki3D_CityGML_Kalasatama_20190326.gml", false);
	}
}
