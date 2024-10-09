package com.whengreen.map.routing;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.reader.osm.OSMReader;
import com.graphhopper.reader.osm.WaySegmentParser;
import com.graphhopper.routing.OSMReaderConfig;
import com.graphhopper.routing.util.AreaIndex;
import com.graphhopper.routing.util.CustomArea;
import com.graphhopper.routing.util.OSMParsers;
import com.graphhopper.routing.util.countryrules.CountryRuleFactory;
import com.graphhopper.storage.BaseGraph;

import java.io.File;

public class CustomOSMReader extends OSMReader {

    public CustomOSMReader(BaseGraph baseGraph, OSMParsers osmParsers, OSMReaderConfig config) {
        super(baseGraph, osmParsers, config);
    }

    @Override
    public CustomOSMReader setFile(File osmFile) {
        super.setFile(osmFile);
        return this;
    }

    @Override
    public CustomOSMReader setAreaIndex(AreaIndex<CustomArea> areaIndex) {
        super.setAreaIndex(areaIndex);
        return this;
    }

    @Override
    public CustomOSMReader setElevationProvider(ElevationProvider eleProvider) {
        super.setElevationProvider(eleProvider);
        return this;
    }

    @Override
    public CustomOSMReader setCountryRuleFactory(CountryRuleFactory countryRuleFactory) {
        super.setCountryRuleFactory(countryRuleFactory);
        return this;
    }

    @Override
    protected void preprocessWay(ReaderWay way, WaySegmentParser.CoordinateSupplier coordinateSupplier, WaySegmentParser.NodeTagSupplier nodeTagSupplier) {
        super.preprocessWay(way, coordinateSupplier, nodeTagSupplier);

//        System.out.println("-----preprocessWay----");
////
        System.out.println("way tag = [" + way.getTags().toString());
//        System.out.println("is footway= "+way.hasTag("highway", "footway") );
//         신호등 태그 확인
        if (way.hasTag("highway", "footway") && way.hasTag("footway", "crossing") && way.hasTag("crossing", "traffic_signals")) {
            // 신호등이 있는 경우
//            way.setTag("has_traffic_signals", true);
//            System.out.println("신호등 잇음: "+way.getTag("has_traffic_signals"));


        } else {
            // 신호등이 없는 경우
//            way.setTag("has_traffic_signals", false);
        }
//        list.add(new KVStorage.KeyValue(MOTORWAY_JUNCTION, nodeName));
    }
}
