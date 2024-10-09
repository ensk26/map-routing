package com.whengreen.map.routing;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;

public class CustomTagParser implements TagParser {
    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way, IntsRef relationFlags) {

        if (way.hasTag("highway", "footway") && way.hasTag("footway", "crossing") && way.hasTag("crossing", "traffic_signals")) {
            // 신호등이 있는 경우
//            way.setTag("has_traffic_signals", true);
//            System.out.println("신호등 잇음: "+way.getTag("has_traffic_signals"));
            if (way.hasTag("highway")) {
                String highwayValue = way.getTag("highway");
//            edge.set(highwayEncoded, highwayValue);
            }
        }
    }
}
