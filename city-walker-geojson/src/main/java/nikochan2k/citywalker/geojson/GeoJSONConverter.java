package nikochan2k.citywalker.geojson;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.github.filosganga.geogson.gson.GeometryAdapterFactory;
import com.github.filosganga.geogson.model.Feature;
import com.github.filosganga.geogson.model.Feature.Builder;
import com.github.filosganga.geogson.model.FeatureCollection;
import com.github.filosganga.geogson.model.LinearRing;
import com.github.filosganga.geogson.model.Point;
import com.github.filosganga.geogson.model.Polygon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import nikochan2k.citywalker.Converter;
import nikochan2k.citywalker.Item;
import nikochan2k.citywalker.Item.Coordinates;

public class GeoJSONConverter extends Converter {

	private static final Logger LOGGER = Logger.getLogger(GeoJSONConverter.class.getName());

	private final List<Feature> features = new ArrayList<Feature>();
	private final Gson gson;

	protected GeoJSONConverter(File input, File outputDir, String srs) {
		super(input, outputDir);
		gson = new GsonBuilder().registerTypeAdapterFactory(new GeometryAdapterFactory()).create();
	}

	@Override
	public void finish() {
		FeatureCollection fc = new FeatureCollection(features);
		String json = gson.toJson(fc);
		try (OutputStream os = new BufferedOutputStream(new FileOutputStream(output))) {
			try (Writer writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
				writer.write(json);
			}
		} catch (IOException e) {
			LOGGER.warning(e.toString());
		}
	}

	@Override
	protected String getExtension() {
		return ".json";
	}

	@Override
	public void process(Item item) {
		List<Point> points = new ArrayList<Point>(item.vertexes.size());
		for (Coordinates coords : item.vertexes) {
			Point point = Point.from(coords.x, coords.y, coords.z);
			points.add(point);
		}
		LinearRing lr = LinearRing.of(points);
		Polygon polygon = Polygon.of(lr);
		Map<String, JsonElement> map = new HashMap<>();
		for (Entry<String, Serializable> entry : item.props.entrySet()) {
			Serializable value = entry.getValue();
			if (value instanceof String) {
				map.put(entry.getKey(), new JsonPrimitive((String) value));
			} else if (value instanceof Number) {
				map.put(entry.getKey(), new JsonPrimitive((Number) value));
			}
		}
		Builder builder = Feature.builder();
		if(item.id != null) {
			builder.withId(item.id);
		}
		Feature feature = builder.withGeometry(polygon).withProperties(map).build();
		features.add(feature);
	}

}
