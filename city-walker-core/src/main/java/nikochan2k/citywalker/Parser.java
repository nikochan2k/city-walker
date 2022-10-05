package nikochan2k.citywalker;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.citygml4j.CityGMLContext;
import org.citygml4j.ade.iur.UrbanRevitalizationADEContext;
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
import org.citygml4j.model.gml.basicTypes.Code;
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
	private CoordinateReferenceSystem inputCRS;
	private CoordinateReferenceSystem outputCRS;

	private String joinCodes(List<Code> codes) {
		StringBuilder builder = new StringBuilder();
		for (Code code : codes) {
			if (builder.length() != 0) {
				builder.append(',');
			}
			builder.append(code.getValue());
		}
		return builder.toString();
	}

	public Parser(Factory factory) {
		this.factory = factory;
		if (factory.getInputSRS() != null) {
			try {
				inputCRS = crsFactory.createFromName(factory.getInputSRS());
			} catch (RuntimeException e) {
				LOGGER.warning(e.toString());
			}
		}
		if (factory.getOutputSRS() != null) {
			try {
				outputCRS = crsFactory.createFromName(factory.getOutputSRS());
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
		for (int i = 0, end = dl.size(); i < end; i += 3) {
			try {
				double x = dl.get(i);
				double y = dl.get(i + 1);
				if (factory.isFlipXY()) {
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
				Coordinates coords = new Coordinates(x, y, 0.0);
				item.vertexes.add(coords);
			} catch (RuntimeException e) {
				LOGGER.warning(e.toString());
			}
		}
		Map<String, Serializable> props = item.props;
		Length length = b.getMeasuredHeight();
		if (length != null) {
			props.put("measuredHeight", length.getValue());
		}

		if (!factory.isNoAttributes()) {
			String id = b.getId();
			if (id != null) {
				props.put("id", id);
			}
			Code clazz = b.getClazz();
			if (clazz != null) {
				props.put("class", b.getClazz().getValue());
			}
			List<Code> function = b.getFunction();
			if (function != null && 0 < function.size()) {
				props.put("function", joinCodes(function));
			}
			List<Code> usage = b.getUsage();
			if (usage != null && 0 < usage.size()) {
				props.put("usage", joinCodes(usage));
			}
			LocalDate yearOfConstruction = b.getYearOfConstruction();
			if (yearOfConstruction != null) {
				props.put("yearOfConstruction", yearOfConstruction);
			}
			LocalDate yearOfDemolition = b.getYearOfDemolition();
			if (yearOfDemolition != null) {
				props.put("yearOfDemolition", yearOfDemolition);
			}
			Code roofType = b.getRoofType();
			if (roofType != null) {
				props.put("roofType", roofType.getValue());
			}
			Integer storeysAboveGround = b.getStoreysAboveGround();
			if (storeysAboveGround != null) {
				props.put("storeysAboveGround", storeysAboveGround);
			}
			Integer storeysBelowGround = b.getStoreysBelowGround();
			if (storeysBelowGround != null) {
				props.put("storeysBelowGround", storeysBelowGround);
			}
			List<AbstractGenericAttribute> attributes = b.getGenericAttribute();
			for (AbstractGenericAttribute attr : attributes) {
				if (attr instanceof StringAttribute) {
					StringAttribute sa = (StringAttribute) attr;
					props.put(sa.getName(), sa.getValue());
				} else if (attr instanceof UriAttribute) {
					UriAttribute ua = (UriAttribute) attr;
					try {
						URI uri = new URI(ua.getValue());
						props.put(ua.getName(), uri);
					} catch (URISyntaxException e) {
						LOGGER.fine(e.toString());
					}
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
					DateAttribute da = (DateAttribute) attr;
					LocalDate date = da.getValue();
					props.put(da.getName(), date);
				}
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

	public void parse(File input) throws CityWalkerException {
		HashMap<String, Object> defaultProps = new HashMap<>();
		defaultProps.put(CityGMLInputFactory.FAIL_ON_MISSING_ADE_SCHEMA, false);
		defaultProps.put(CityGMLInputFactory.USE_VALIDATION, false);
		parse(input, new HashMap<String, Object>(0));
	}

	public void parse(File input, Map<String, Object> props) throws CityWalkerException {
		try {
			CityGMLContext ctx = CityGMLContext.getInstance();
			ctx.registerADEContext(new UrbanRevitalizationADEContext());
			CityGMLBuilder builder = ctx.createCityGMLBuilder();
			CityGMLInputFactory in = builder.createCityGMLInputFactory();
			for(Entry<String, Object> entry : props.entrySet()) {
				in.setProperty(entry.getKey(), entry.getValue());
			}
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
}
