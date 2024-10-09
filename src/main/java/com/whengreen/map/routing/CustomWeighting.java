package com.whengreen.map.routing;

import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.weighting.AbstractWeighting;
import com.graphhopper.routing.weighting.TurnCostProvider;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.EdgeIteratorState;

public class CustomWeighting extends AbstractWeighting {

    protected CustomWeighting(BooleanEncodedValue accessEnc, DecimalEncodedValue speedEnc, TurnCostProvider turnCostProvider) {
        super(accessEnc, speedEnc, turnCostProvider);
    }

    @Override
    public double calcMinWeightPerDistance() {
        return 0;
    }

    @Override
    public double calcEdgeWeight(EdgeIteratorState edgeState, boolean reverse) {


        return 0;
    }

    @Override
    public String getName() {
        return null;
    }
}
