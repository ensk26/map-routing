package com.whengreen.map.routing;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.LMProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.*;
import com.graphhopper.util.shapes.GHPoint;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Locale;

import static com.graphhopper.json.Statement.If;
import static com.graphhopper.json.Statement.Op.LIMIT;
import static com.graphhopper.json.Statement.Op.MULTIPLY;

@Configuration
public class RouteManager {

    // 환경 변수에서 OSM 파일 경로를 주입
    @Value("${OSM_FILE_PATH}")
    private String osmFilePath;

    // Bean으로 GraphHopper 인스턴스를 생성
//    @Bean
    public GraphHopper createGraphHopperInstance() {
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(osmFilePath);  // 환경 변수에서 주입된 파일 경로 설정

        // 그래프 파일을 저장할 위치
        hopper.setGraphHopperLocation("target/routing-graph-cache");


        // 인코딩 값 설정 (차량 접근과 평균 속도)
        hopper.setEncodedValuesString("foot_access, foot_average_speed, hike_rating, foot_priority");

        // 프로파일 설정 (car.json 파일에서 커스텀 모델 로드)
        hopper.setProfiles(new Profile("foot").setCustomModel(GHUtility.loadCustomModelFromJar("foot.json")));

        // car 프로파일에 대해 CH (Contraction Hierarchies) 속도 모드 활성화
        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile("foot"));

        // 데이터를 로드하거나 가져오기
        hopper.importOrLoad();

        return hopper;
    }

    // 최적 경로
    public void routing(GraphHopper hopper, Point startCoordinate, Point endCoordinate) {
        // simple configuration of the request object
        GHRequest req = new GHRequest(startCoordinate.getX(), startCoordinate.getY(), endCoordinate.getX(), endCoordinate.getY())
                .setProfile("foot")
                .setLocale(Locale.KOREA);
        GHResponse rsp = hopper.route(req);

        // handle errors
        if (rsp.hasErrors())
            throw new RuntimeException(rsp.getErrors().toString());

        // use the best path, see the GHResponse class for more possibilities.
        ResponsePath path = rsp.getBest();

        // points, distance in meters and time in millis of the full path
        PointList pointList = path.getPoints();
        double distance = path.getDistance();
        long timeInMs = path.getTime();

        Translation tr = hopper.getTranslationMap().getWithFallBack(Locale.KOREA);
        InstructionList il = path.getInstructions();
        // iterate over all turn instructions
        for (Instruction instruction : il) {
            // System.out.println("distance " + instruction.getDistance() + " for instruction: " + instruction.getTurnDescription(tr));
        }
        assert il.size() == 6;
        assert Helper.round(path.getDistance(), -2) == 600;
    }

    public static void speedModeVersusFlexibleMode(GraphHopper hopper, double startLan, double startLon, double endLan, double endLon) {
        GHRequest req = new GHRequest(startLan, startLon, endLan, endLon)
                .setProfile("foot").setAlgorithm(Parameters.Algorithms.ASTAR_BI).putHint(Parameters.CH.DISABLE, true);
        GHResponse res = hopper.route(req);
        if (res.hasErrors())
            throw new RuntimeException(res.getErrors().toString());
        assert Helper.round(res.getBest().getDistance(), -2) == 600;
    }

    // 최적 경로 포함 대체 경로
    public void alternativeRoute(double startLan, double startLon, double endLan, double endLon) {

        GraphHopper hopper = createGraphHopperInstance();

        // calculate alternative routes between two points (supported with and without CH)
        GHRequest req = new GHRequest().setProfile("foot")
                .addPoint(new GHPoint(startLan, startLon)).addPoint(new GHPoint(endLan, endLon))
                .setAlgorithm(Parameters.Algorithms.ALT_ROUTE);
        req.getHints().putObject(Parameters.Algorithms.AltRoute.MAX_PATHS, 3);
        GHResponse res = hopper.route(req);
        if (res.hasErrors())
            throw new RuntimeException(res.getErrors().toString());
        assert res.getAll().size() == 2;
        assert Helper.round(res.getBest().getDistance(), -2) == 2200;

        ResponsePath path = res.getBest();
        printRoute(path);
    }

    private void printRoute(ResponsePath path) {
        // 번역 설정 (영어로 설정)
        Translation translation = new TranslationMap().doImport().getWithFallBack(Locale.KOREA);

        // 경로 안내 리스트 (InstructionList)
        InstructionList instructions = path.getInstructions();

        // 경로 안내 출력
        for (Instruction instruction : instructions) {
            String text = instruction.getTurnDescription(translation);  // 방향 설명
            double distance = instruction.getDistance();  // 이 구간의 거리
            long time = instruction.getTime() / 1000;  // 이 구간의 소요 시간

            System.out.println(text + " for " + distance + " meters, takes " + time + " seconds.");
        }
    }

    private boolean isHasTrafficSignal(IntsRef flags) {
        // flags의 4번째 비트를 확인하여 has_traffic_signal 여부 판단

        System.out.println(flags.toString() + " " + flags.length);
        int index = 4; // has_traffic_signal의 비트 인덱스
        int value = flags.ints[index]; // index에 해당하는 값 가져오기
        return (value & 1) != 0; // 4는 has_traffic_signal의 비트 인덱스
    }

    public void customizableRouting(double startLat, double startLon, double endLat, double endLon) {
        System.out.println("----------------customizableRouting------------------");
        CustomGraphHopper hopper = new CustomGraphHopper();
        System.out.println("hooper 생성");
        hopper.setOSMFile(osmFilePath);
        hopper.setGraphHopperLocation("target/routing-custom-graph-cache");
        hopper.setEncodedValuesString("foot_access, foot_average_speed, hike_rating, foot_priority");

        hopper.setProfiles(new Profile("foot_custom").setCustomModel(GHUtility.loadCustomModelFromJar("foot.json")));
//        System.out.println(Arrays.toString(hopper.getEncodedValuesString().toCharArray()));

        // The hybrid mode uses the "landmark algorithm" and is up to 15x faster than the flexible mode (Dijkstra).
        // Still it is slower than the speed mode ("contraction hierarchies algorithm") ...
        hopper.getLMPreparationHandler().setLMProfiles(new LMProfile("foot_custom"));
        System.out.println(Arrays.toString(hopper.getEncodedValuesString().toCharArray()));
        System.out.println("-----osm 초기화------");
        System.out.println(hopper.getProfile("foot_custom"));

        hopper.importOrLoad();

//        hopper.importOSM();
        System.out.println(hopper.getProfile("foot_custom"));
//        hopper.close();

        //테스트
//        BaseGraph graph = new BaseGraph.Builder(4).create();
//        LocationIndexTree index = new LocationIndexTree(graph.getBaseGraph(), graph.getDirectory());


        ////


//         특정 위치를 조회하기 위해 좌표 설정
        Graph graph = hopper.getBaseGraph();

        EdgeIterator edges = graph.getAllEdges();


        int count = 0;
        int tarcount = 0;

        NodeAccess nodeAccess = graph.getNodeAccess();

        System.out.println("node 조회");

        while (edges.next()) {
            int baseNode = edges.getBaseNode();  // 엣지 시작 노드
            int adjNode = edges.getAdjNode();    // 엣지 종료 노드
            double distance = edges.getDistance();  // 엣지 거리

            System.out.println();


            int edgeId = edges.getEdge();   //엣지 id

            IntsRef flags = edges.getFlags();

            EdgeIteratorState edgeState = graph.getEdgeIteratorState(edgeId, adjNode);

//            if (way.hasTag("highway", "footway") && way.hasTag("footway","crossing")&& way.hasTag("crossing", "traffic_signals")) {

//            System.out.println("Has traffic signal: " + edgeState.getValue("highway"));

//            // has_traffic_signal 속성을 확인하는 사용자 정의 로직
//            boolean hasTrafficSignal = isHasTrafficSignal(flags);
//            System.out.println("hasTrafficSignal = " + edgeState.getValue("foot_access"));
//
//            if (hasTrafficSignal) {
//                tarcount++;
//            }

            // baseNode의 위도와 경도를 가져오기
            double baseNodeLat = nodeAccess.getLat(baseNode);
            double baseNodeLon = nodeAccess.getLon(baseNode);

            // adjNode의 위도와 경도를 가져오기
            double adjNodeLat = nodeAccess.getLat(adjNode);
            double adjNodeLon = nodeAccess.getLon(adjNode);

//            System.out.println("Edge from Node " + baseNode + " to Node " + adjNode +
//                    ", Distance: " + distance + " meters");

            // 각 노드의 위도와 경도를 출력
//            System.out.println("Base Node Position: (" + baseNodeLat + ", " + baseNodeLon + ")");
//            System.out.println("Adj Node Position: (" + adjNodeLat + ", " + adjNodeLon + ")");
//
//            System.out.println("Has traffic signal: " + edges.getValue("has_traffic_signals"));

            count++;
            // 필요한 정보 출력
//            System.out.println("Edge from Node " + baseNode + " to Node " + adjNode + ", Distance: " + distance + " meters");
        }

//        System.out.println("edge count: " + count + " " + "traffic count: " + tarcount);


        // ... but for the hybrid mode we can customize the route calculation even at request time:
        // 1. a request with default preferences
        GHRequest req = new GHRequest().setProfile("foot_custom").
                addPoint(new GHPoint(startLat, startLon)).addPoint(new GHPoint(endLat, endLon));

        GHResponse res = hopper.route(req);
        if (res.hasErrors())
            throw new RuntimeException(res.getErrors().toString());

        assert Math.round(res.getBest().getTime() / 1000d) == 94;

        // 2. now avoid the secondary road and reduce the maximum speed, see docs/core/custom-models.md for an in-depth explanation
        // and also the blog posts https://www.graphhopper.com/?s=customizable+routing
        CustomModel model = new CustomModel();
        model.addToPriority(If("road_class == SECONDARY", MULTIPLY, "0.5"));

        // unconditional limit to 20km/h
        model.addToSpeed(If("true", LIMIT, "30"));

        req.setCustomModel(model);
        res = hopper.route(req);
        if (res.hasErrors())
            throw new RuntimeException(res.getErrors().toString());

        assert Math.round(res.getBest().getTime() / 1000d) == 184;

        hopper.close(); // 작업 후 리소스 해제

    }
}
