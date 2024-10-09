package com.whengreen.map.routing;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.AreaIndex;
import com.graphhopper.routing.util.CustomArea;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.Constants;
import com.graphhopper.util.JsonFeatureCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.graphhopper.util.GHUtility.readCountries;
import static com.graphhopper.util.Helper.createFormatter;
import static com.graphhopper.util.Helper.getMemInfo;
import static org.springframework.util.ObjectUtils.isEmpty;

public class CustomGraphHopper extends GraphHopper {

    private static final Logger logger = LoggerFactory.getLogger(GraphHopper.class);

    @Override
    public GraphHopper importOrLoad() {
        System.out.println("-----importOrLoad()-----");
//        if (!load()) {
        load();
//            System.out.println("load!");
//            printInfo();
        process(false);
//        } else {
//            printInfo();
//        }
        return this;
    }

    @Override
    protected void process(boolean closeEarly) {
        super.process(closeEarly);
        System.out.println("process");
    }

    @Override
    protected void importOSM() {
        System.out.println("-----import osm-------");
//        String osmFile = super.getOSMFile();
////        String ghLocation = super.getGraphHopperLocation();
//        String customAreasDirectory = super.getCustomAreasDirectory();
//        CountryRuleFactory countryRuleFactory = super.getCountryRuleFactory();
//        BaseGraph baseGraph = super.getBaseGraph();
////        OSMParsers osmParsers = super.getOSMParsers();
////        OSMReaderConfig osmReaderConfig = super.getReaderConfig();
//        ElevationProvider eleProvider = super.getElevationProvider();
//        StorableProperties properties = super.getProperties();

//        super.importOSM();
        System.out.println("---import osm----");

        if (super.getOSMFile() == null)
            throw new IllegalStateException("Couldn't load from existing folder: " + getGraphHopperLocation()
                    + " but also cannot use file for DataReader as it wasn't specified!");

        List<CustomArea> customAreas = readCountries();
        if (isEmpty(getCustomAreasDirectory())) {
            logger.info("No custom areas are used, custom_areas.directory not given");
        } else {
            logger.info("Creating custom area index, reading custom areas from: '" + getCustomAreasDirectory() + "'");
            customAreas.addAll(readCustomAreas());
        }

        AreaIndex<CustomArea> areaIndex = new AreaIndex<>(customAreas);
        if (super.getCountryRuleFactory() == null || getCountryRuleFactory().getCountryToRuleMap().isEmpty()) {
            logger.info("No country rules available");
        } else {
            logger.info("Applying rules for the following countries: {}", getCountryRuleFactory().getCountryToRuleMap().keySet());
        }

        logger.info("start creating graph from " + getOSMFile());
        // custom osm 적용

        System.out.println("---------------- custom osm 만들기 -----------------------");

//        System.out.println("getBaseGraph= "+getBaseGraph().getBaseGraph().toString());
        System.out.println("getParse=" + getOSMParsers());
        System.out.println("geetreader=" + getReaderConfig());
        System.out.println("getelevationprovider=" + getElevationProvider());
        System.out.println("getcountryRule" + getCountryRuleFactory());


        System.out.println("----------------osm reader----------------");
        CustomOSMReader reader = new CustomOSMReader(getBaseGraph().getBaseGraph(), getOSMParsers(), getReaderConfig()).setFile(_getOSMFile()).
                setAreaIndex(areaIndex).
                setElevationProvider(getElevationProvider()).
                setCountryRuleFactory(getCountryRuleFactory());
        logger.info("using " + getBaseGraphString() + ", memory:" + getMemInfo());

        createBaseGraphAndProperties();

        try {
            reader.readGraph();
        } catch (IOException ex) {
            throw new RuntimeException("Cannot read file " + getOSMFile(), ex);
        }
        DateFormat f = createFormatter();
        getProperties().put("datareader.import.date", f.format(new Date()));
        if (reader.getDataDate() != null)
            getProperties().put("datareader.data.date", f.format(reader.getDataDate()));
    }

    private List<CustomArea> readCustomAreas() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JtsModule());
        final Path bordersDirectory = Paths.get(getCustomAreasDirectory());
        List<JsonFeatureCollection> jsonFeatureCollections = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(bordersDirectory, "*.{geojson,json}")) {
            for (Path borderFile : stream) {
                try (BufferedReader reader = Files.newBufferedReader(borderFile, StandardCharsets.UTF_8)) {
                    JsonFeatureCollection jsonFeatureCollection = objectMapper.readValue(reader, JsonFeatureCollection.class);
                    jsonFeatureCollections.add(jsonFeatureCollection);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return jsonFeatureCollections.stream().flatMap(j -> j.getFeatures().stream())
                .map(CustomArea::fromJsonFeature)
                .collect(Collectors.toList());
    }

    private void printInfo() {
        BaseGraph baseGraph = getBaseGraph();
        logger.info("version " + Constants.VERSION + "|" + Constants.BUILD_DATE + " (" + Constants.getVersions() + ")");
        if (baseGraph != null)
            logger.info("graph " + getBaseGraphString() + ", details:" + baseGraph.toDetailsString());
    }

    private String getBaseGraphString() {
        BaseGraph baseGraph = getBaseGraph();
        return encodingManager
                + "|" + baseGraph.getDirectory().getDefaultType()
                + "|" + baseGraph.getNodeAccess().getDimension() + "D"
                + "|" + (baseGraph.getTurnCostStorage() != null ? baseGraph.getTurnCostStorage() : "no_turn_cost")
                + "|" + getVersionsString();
    }

    private String getVersionsString() {
        return "nodes:" + Constants.VERSION_NODE +
                ",edges:" + Constants.VERSION_EDGE +
                ",geometry:" + Constants.VERSION_GEOMETRY +
                ",location_index:" + Constants.VERSION_LOCATION_IDX +
                ",string_index:" + Constants.VERSION_KV_STORAGE +
                ",nodesCH:" + Constants.VERSION_NODE_CH +
                ",shortcuts:" + Constants.VERSION_SHORTCUT;
    }
}
