package nikochan2k.citywalker;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.citygml4j.CityGMLContext;
import org.citygml4j.builder.jaxb.CityGMLBuilder;
import org.citygml4j.model.citygml.CityGML;
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.building.Building;
import org.citygml4j.model.citygml.core.AbstractCityObject;
import org.citygml4j.model.citygml.core.CityModel;
import org.citygml4j.model.citygml.core.CityObjectMember;
import org.citygml4j.model.citygml.generics.AbstractGenericAttribute;
import org.citygml4j.model.citygml.generics.DateAttribute;
import org.citygml4j.model.citygml.generics.DoubleAttribute;
import org.citygml4j.model.citygml.generics.IntAttribute;
import org.citygml4j.model.citygml.generics.MeasureAttribute;
import org.citygml4j.model.citygml.generics.StringAttribute;
import org.citygml4j.model.citygml.generics.UriAttribute;
import org.citygml4j.model.gml.feature.BoundingShape;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;
import org.citygml4j.model.gml.geometry.complexes.CompositeSurface;
import org.citygml4j.model.gml.geometry.primitives.AbstractSurface;
import org.citygml4j.model.gml.geometry.primitives.DirectPosition;
import org.citygml4j.model.gml.geometry.primitives.DirectPositionList;
import org.citygml4j.model.gml.geometry.primitives.Envelope;
import org.citygml4j.model.gml.geometry.primitives.Exterior;
import org.citygml4j.model.gml.geometry.primitives.LinearRing;
import org.citygml4j.model.gml.geometry.primitives.Polygon;
import org.citygml4j.model.gml.geometry.primitives.PosOrPointPropertyOrPointRep;
import org.citygml4j.model.gml.geometry.primitives.Solid;
import org.citygml4j.model.gml.geometry.primitives.SolidProperty;
import org.citygml4j.model.gml.geometry.primitives.SurfaceProperty;
import org.citygml4j.model.gml.measures.Length;
import org.citygml4j.xml.io.CityGMLInputFactory;
import org.citygml4j.xml.io.reader.CityGMLReader;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

import nikochan2k.citywalker.Item.Coordinates;

public class Parser {

	private static final Logger LOGGER = Logger.getLogger(Parser.class.getName());
	private static final Pattern SRS_LIKE = Pattern.compile("\\d{4,}");

	private final CRSFactory crsFactory = new CRSFactory();
	private final CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
	private final Factory factory;
	private boolean flipXY;
	private CoordinateReferenceSystem inputCRS;
	private CoordinateReferenceSystem outputCRS;

	public Parser(Factory factory) {
		this(factory, null, null);
	}

	public Parser(Factory factory, String inputSRS, String outputSRS) {
		this.factory = factory;
		if (inputSRS != null) {
			try {
				inputCRS = crsFactory.createFromName(inputSRS);
			} catch (RuntimeException e) {
				LOGGER.warning(e.toString());
			}
		}
		if (outputSRS != null) {
			try {
				outputCRS = crsFactory.createFromName(outputSRS);
			} catch (RuntimeException e) {
				LOGGER.warning(e.toString());
			}
		}

		if (outputCRS == null) {
			outputCRS = crsFactory.createFromName("EPSG:4326");
		}
	}

	private Item createItem(Building b, CoordinateTransform ct) {
		Polygon polygon = getPolygon(b.getLod0FootPrint());
		if (polygon == null)
			polygon = getPolygon(b.getLod0RoofEdge());
		if (polygon == null)
			polygon = getPolygon(b.getLod1Solid());
		if (polygon == null) {
			return null;
		}
		Exterior ex = (Exterior) polygon.getExterior();
		LinearRing lr = (LinearRing) ex.getRing();
		DirectPositionList dpl = (DirectPositionList) lr.getPosList();
		List<Double> dl;
		if (dpl != null) {
			dl = dpl.toList3d();
		} else {
			dl = new ArrayList<>();
			List<PosOrPointPropertyOrPointRep> list = lr.getPosOrPointPropertyOrPointRep();
			if (list == null) {
				return null;
			}
			for (PosOrPointPropertyOrPointRep item : list) {
				DirectPosition dp = item.getPos();
				if (dp == null) {
					continue;
				}
				dl.addAll(dp.getValue());
			}
		}
		if (dl.size() == 0) {
			return null;
		}
		Item item = new Item(b.getId());
		double roof = 0.0;
		for (int i = 0, end = dl.size(); i < end; i += 3) {
			try {
				double x = dl.get(i);
				double y = dl.get(i + 1);
				if (flipXY) {
					double tmp = x;
					x = y;
					y = tmp;
				}
				if (ct != null) {
					ProjCoordinate result = new ProjCoordinate();
					ct.transform(new ProjCoordinate(x, y), result);
					x = result.x;
					y = result.y;
				}
				double z = dl.get(i + 2);
				Coordinates coords = new Coordinates(x, y, 0.0);
				item.vertexes.add(coords);
				roof = Math.max(roof, z);
			} catch (RuntimeException e) {
				LOGGER.warning(e.toString());
			}
		}
		Map<String, Serializable> props = item.props;
		Length length = b.getMeasuredHeight();
		if (length != null) {
			props.put("measuredHeight", length.getValue());
		} else {
			props.put("measuredHeight", roof);
		}
		List<AbstractGenericAttribute> attributes = b.getGenericAttribute();
		for (AbstractGenericAttribute attr : attributes) {
			if (attr instanceof StringAttribute) {
				StringAttribute sa = (StringAttribute) attr;
				props.put(sa.getName(), sa.getValue());
			} else if (attr instanceof UriAttribute) {
				UriAttribute ua = (UriAttribute) attr;
				props.put(ua.getName(), ua.getValue());
			} else if (attr instanceof MeasureAttribute) {
				MeasureAttribute ma = (MeasureAttribute) attr;
				props.put(ma.getName(), ma.getValue().getValue());
			} else if (attr instanceof IntAttribute) {
				IntAttribute ia = (IntAttribute) attr;
				props.put(ia.getName(), ia.getValue());
			} else if (attr instanceof DoubleAttribute) {
				DoubleAttribute da = (DoubleAttribute) attr;
				props.put(da.getName(), da.getValue());
			} else if (attr instanceof DateAttribute) {
				DateAttribute st = (DateAttribute) attr;
				LocalDate date = st.getValue();
				DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssX");
				props.put(st.getName(), date.atStartOfDay().atOffset(ZoneOffset.UTC).format(dtf));
			}
		}
		return item;
	}

	private double getArea(Polygon polygon) {
		Exterior ex = (Exterior) polygon.getExterior();
		LinearRing lr = (LinearRing) ex.getRing();
		DirectPositionList dpl = (DirectPositionList) lr.getPosList();
		List<Double> coords;
		if (dpl != null) {
			coords = dpl.toList3d();
		} else {
			coords = new ArrayList<>();
			List<PosOrPointPropertyOrPointRep> list = lr.getPosOrPointPropertyOrPointRep();
			if (list == null) {
				return 0.0;
			}
			for (PosOrPointPropertyOrPointRep item : list) {
				DirectPosition dp = item.getPos();
				if (dp == null) {
					continue;
				}
				coords.addAll(dp.getValue());
			}
		}
		double minX = Double.MAX_VALUE, maxX = 0, minY = Double.MAX_VALUE, maxY = 0;
		for (int i = 0, end = coords.size(); i < end; i += 3) {
			double x = coords.get(i);
			double y = coords.get(i + 1);
			minX = Math.min(minX, x);
			maxX = Math.max(maxX, x);
			minY = Math.min(minY, y);
			maxY = Math.max(maxY, y);
		}
		double diffX = maxX - minX;
		double diffY = maxY - minY;
		return diffX * diffY;
	}

	private Polygon getPolygon(AbstractSurface surface) {
		if (surface instanceof Polygon) {
			return (Polygon) surface;
		}
		if (surface instanceof CompositeSurface) {
			CompositeSurface cs = (CompositeSurface) surface;
			List<SurfaceProperty> spl = cs.getSurfaceMember();
			if (spl.size() == 0) {
				return null;
			}
			double maxArea = 0.0;
			Polygon result = null;
			for (SurfaceProperty sp : spl) {
				Polygon polygon = getPolygon(sp.getGeometry());
				if (polygon == null) {
					continue;
				}
				double area = getArea(polygon);
				if (maxArea < area) {
					result = polygon;
				}
			}
			return result;
		}
		return null;
	}

	private Polygon getPolygon(MultiSurfaceProperty msp) {
		if (msp == null) {
			return null;
		}
		List<SurfaceProperty> spl = msp.getMultiSurface().getSurfaceMember();
		if (spl.size() == 0) {
			return null;
		}
		AbstractSurface surface = spl.get(0).getGeometry();
		return getPolygon(surface);
	}

	private Polygon getPolygon(SolidProperty sp) {
		if (sp == null) {
			return null;
		}
		Solid solid = (Solid) sp.getObject();
		return getPolygon(solid.getExterior().getGeometry());
	}

	public boolean isFlipXY() {
		return flipXY;
	}

	public void parse(File input) throws CityWalkerException {
		try {
			CityGMLContext ctx = CityGMLContext.getInstance();
			CityGMLBuilder builder = ctx.createCityGMLBuilder();
			CityGMLInputFactory in = builder.createCityGMLInputFactory();
			try (CityGMLReader reader = in.createCityGMLReader(input)) {
				Processor processor = factory.createProcessor(input, outputCRS.getName());
				LOGGER.info(String.format("Parsing \"%s\"", input.getAbsolutePath()));
				while (reader.hasNext()) {
					CityGML citygml = reader.nextFeature();
					if (citygml.getCityGMLClass() == CityGMLClass.CITY_MODEL) {
						parseCity(processor, citygml);
					}
				}
				processor.finish();
			}
		} catch (Exception e) {
			throw new CityWalkerException(e.getMessage(), e);
		}
	}

	private void parseCity(Processor processor, CityGML citygml) {
		CityModel cityModel = (CityModel) citygml;
		for (CityObjectMember cityObjectMember : cityModel.getCityObjectMember()) {
			try {
				AbstractCityObject cityObject = cityObjectMember.getCityObject();
				if (cityObject.getCityGMLClass() != CityGMLClass.BUILDING) {
					continue;
				}

				CoordinateReferenceSystem inputCRS = this.inputCRS;
				if (inputCRS == null) {
					String inputSRS = "EPSG:4326";
					BoundingShape bs = cityObject.getBoundedBy();
					if (bs != null) {
						Envelope envelope = bs.getEnvelope();
						if (envelope != null) {
							inputSRS = envelope.getSrsName();
						}
					}

					try {
						inputCRS = crsFactory.createFromName(inputSRS);
					} catch (RuntimeException e) {
					}
					if (inputCRS == null) {
						Matcher m = SRS_LIKE.matcher(inputSRS);
						if (m.find()) {
							inputSRS = "EPSG:" + m.group();
						}
						try {
							inputCRS = crsFactory.createFromName(inputSRS);
						} catch (RuntimeException e) {
							LOGGER.warning(e.toString());
						}
					}
					if (inputCRS == null) {
						inputCRS = crsFactory.createFromName("EPSG:4326");
					}
				}
				CoordinateTransform ct = null;
				if (!inputCRS.equals(outputCRS)) {
					ct = ctFactory.createTransform(inputCRS, outputCRS);
				}

				Building b = (Building) cityObject;
				Item item = createItem(b, ct);
				if (item == null) {
					continue;
				}
				processor.process(item);
			} catch (RuntimeException e) {
				LOGGER.warning(e.toString());
			}
		}
	}

	public void setFlipXY(boolean flip) {
		this.flipXY = flip;
	}
}
