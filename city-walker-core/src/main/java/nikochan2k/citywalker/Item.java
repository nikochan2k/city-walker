package nikochan2k.citywalker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Item {
	
	public static class Coordinates {
		public final double x;
		public final double y;
		public final double z;
		public Coordinates(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
	
	public final String id;
	public final List<Coordinates> vertexes = new ArrayList<>();
	public final Map<String, Serializable> props = new LinkedHashMap<>();
	
	public Item(String id) {
		this.id = id;
	}
	
}
