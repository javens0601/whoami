package com.javen.common.whoami.geo;

/**
 * @Description
 * @Author: enmonster
 * @CreateTime: 2025/7/28 14:21
 */
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.operation.distance.DistanceOp;

/**
 * 使用jts库从省市区县乡镇边界数据（AreaCity-JsSpider-StatsGov开源库）或geojson文件中查找出和任意点、线、面有相交的边界，内存占用低，性能优良。
 * <pre>
 * 可用于：
 *     - 调用 QueryPoint(lng, lat) 查询一个坐标点对应的省市区名称等信息；
 *     - 调用 ReadWKT_FromWkbsFile(where) 查询获取需要的省市区边界WKT文本数据。
 *
 * 部分原理：
 *      1. 初始化时，会将边界图形按网格动态的切分成小的图形，大幅减少查询时的几何计算量从而性能优异；
 *      2. 内存中只会保存小的图形的外接矩形（Envelope），小的图形本身会序列化成WKB数据（根据Init方式存入文件或内存），因此内存占用很低；
 *      3. 内存中的外接矩形（Envelope）数据会使用jts的STRTree索引，几何计算查询时，先从EnvelopeSTRTree中初步筛选出符合条件的边界，RTree性能极佳，大幅过滤掉不相关的边界；
 *      4. 对EnvelopeSTRTree初步筛选出来的边界，读取出WKB数据反序列化成小的图形，然后进行精确的几何计算（因为是小图，所以读取和计算性能极高）。
 *
 * jts库地址：https://github.com/locationtech/jts
 * </pre>
 *
 * <br>GitHub: https://github.com/xiangyuecn/AreaCity-Query-Geometry （github可换成gitee）
 * <br>省市区县乡镇区划边界数据: https://github.com/xiangyuecn/AreaCity-JsSpider-StatsGov （github可换成gitee）
 */
public class AreaCityQuery {
    /** 默认提供的0-9的10个静态实例，每个实例可以分别使用一个数据文件进行初始化和查询，当然自己调用new AreaCityQuery()创建一个新实例使用也是一样的 */
    static public final AreaCityQuery[] Instances=new AreaCityQuery[] {
            new AreaCityQuery(),new AreaCityQuery(),new AreaCityQuery(),new AreaCityQuery(),new AreaCityQuery()
            ,new AreaCityQuery(),new AreaCityQuery(),new AreaCityQuery(),new AreaCityQuery(),new AreaCityQuery()
    };

    /**
     * 几何计算查询出包含此坐标点的所有边界图形的属性数据（和此坐标点相交）：
     * <pre>
     * - 如果坐标点位于图形内部或边上，这个图形将匹配；
     * - 如果坐标点位于两个图形的边上，这两个图形将都会匹配；
     * - 如果图形存在孔洞，并且坐标点位于孔洞内（不含边界），这个图形将不匹配。
     * </pre>
     *
     * <br>输入坐标参数的坐标系必须和初始化时使用的geojson数据的坐标系一致，否则坐标可能会有比较大的偏移，导致查询结果不正确。
     * <br>如果还未完成初始化，或者查询出错，都会抛异常。
     * <br>本方法线程安全。
     *
     * <br><br>注意：如果此坐标位于界线外侧（如海岸线、境界线）时将不会有边界图形能够匹配包含（就算距离只相差1cm），此时如果你希望能匹配到附近不远的边界图形，请使用QueryPointWithTolerance方法
     *
     * @param lng 进度坐标值
     * @param lat 纬度坐标值
     * @param where 可以为null，可选提供一个函数，筛选属性数据（此数据已经过初步筛选），会传入属性的json字符串，如果需要去精确计算这个边界图形是否匹配就返回true，否则返回false跳过这条边界图形的精确计算
     * @param res 可以为null，如果提供结果对象，可通过此对象的Set_XXX属性控制某些查询行为，比如设置Set_ReturnWKTKey可以额外返回边界的WKT文本数据；并且本次查询的结果和统计数据将累加到这个结果内（性能测试用）。注意：此结果对象非线程安全
     */
    public QueryResult QueryPoint(double lng, double lat, Func<String,Boolean> where, QueryResult res) throws Exception{
        CheckInitIsOK();
        return QueryGeometry(Factory.createPoint(new Coordinate(lng, lat)), where, res);
    }
    /**
     * 先几何计算查询出包含此坐标点的所有边界图形的属性数据，此时和QueryPoint方法功能完全一致。
     * <br><br>当没有边界图形包含此坐标点时，会查询出和此坐标点距离最近的边界图形的属性数据，同一级别的边界图形只会返回距离最近的一条属性数据，比如：范围内匹配到多个市，只返回最近的一个市；级别的划分依据为属性中的deep值，deep值为空的为同的一级
     * ；结果属性中会额外添加PointDistance（图形与坐标的距离，单位米）、PointDistanceID（图形唯一标识符）两个值；由于多进行了一次范围查询，性能会比QueryPoint方法低些。
     * <br><br>本方法主要用途是解决：当坐标位于界线外侧（如海岸线、境界线）时QueryPoint方法将不会有边界图形能够匹配包含此坐标（就算距离只相差1cm），本方法将能够匹配到附近不远的边界图形数据。
     *
     * <br><br>更多参数文档请参考QueryPoint方法，本方法线程安全。
     *
     * @see #QueryPoint(double, double, Func, QueryResult)
     * @param toleranceMetre 距离范围容差值，单位米，比如取值2500，相当于一个以此坐标为中心点、半径为2.5km的圆形范围；当没有任何边界图形包含此坐标点时，会查询出与此坐标点的距离不超过此值 且 距离最近的边界图形属性数据；取值为0时不进行范围查找；取值为-1时不限制距离大小，会遍历所有数据导致性能极低
     */
    public QueryResult QueryPointWithTolerance(double lng, double lat, Func<String,Boolean> where, QueryResult res, int toleranceMetre) throws Exception {
        CheckInitIsOK();
        if(res!=null && res.Result==null) throw new Exception("不支持无Result调用");

        int resLen0=res==null?0:res.Result.size();
        Point point=Factory.createPoint(new Coordinate(lng, lat));
        QueryResult res1=QueryGeometry(point, where, res);
        if(res1.Result.size()>resLen0 || toleranceMetre==0) {
            return res1; //查找到了的就直接返回
        }

        Geometry geom;
        if(toleranceMetre>0) { //以点为中心，容差为半径，构造出一个圆，扩大到容差范围进行查找
            geom=CreateSimpleCircle(lng, lat, toleranceMetre, 24);
        } else { //不限制范围
            geom=CreateRect(-180, -90, 180, 90);
        }
        HashMap<String, Double> propDists=new HashMap<>();
        HashMap<String, Object[]> deepDists=new HashMap<>();
        DecimalFormat df=new DecimalFormat("0.00");
        res1.QueryCount--;
        res1=QueryGeometryProcess(geom, where, res1, new Func<Object[], Boolean>(){
            @Override
            public Boolean Exec(Object[] args) throws Exception {
                boolean add=false;
                String prop=(String)args[0];
                Geometry geom=(Geometry)args[1];
                String lineNo=(String)args[2];

                Coordinate[] ps=DistanceOp.nearestPoints(geom, point);
                double dist=Distance(ps[0].x, ps[0].y, ps[1].x, ps[1].y);
                Double exists=propDists.get(lineNo);
                if(exists==null || exists>dist) {//去重，相同一条数据只取距离最近的
                    Matcher m=Exp_OkGeoCsv_Deep.matcher(prop);
                    String deep=m.find()?m.group(1):"";
                    Object[] deepExists=deepDists.get(deep);
                    if(deepExists==null || (double)deepExists[0]>dist) {//去重，同一级别只取距离最近的
                        add=true;
                        propDists.put(lineNo, dist);
                        deepDists.put(deep, new Object[] { dist, lineNo });
                        prop=prop.substring(0, prop.length()-1)+", \"PointDistanceID\": "+lineNo+", \"PointDistance\": "+df.format(dist)+"}";
                        args[0]=prop;
                    }
                }
                return add;
            }
        });
        //清理掉结果中多余的数据，每一级取一个，同一数据取最后一个
        HashSet<String> ids=new HashSet<>(), exists=new HashSet<>();
        for(Object[] o : deepDists.values()) ids.add((String)o[1]);
        for(int i=res1.Result.size()-1;i>=resLen0;i--) {
            String prop=res1.Result.get(i);
            Matcher m=Exp_PointDistanceID.matcher(prop); m.find();
            String lineNo=m.group(1);
            if(!ids.contains(lineNo) || exists.contains(lineNo)) {
                res1.Result.remove(i);
            }else {
                exists.add(lineNo);
            }
        }
        return res1;
    }
    static private Pattern Exp_PointDistanceID=Pattern.compile("\"PointDistanceID[\\s\":]+(\\d+)");
    static private Pattern Exp_OkGeoCsv_Deep=Pattern.compile("\"deep[\\s\":]+(\\d+)");


    /**
     * 几何计算查询出和此图形（点、线、面）有交点的所有边界图形的属性数据（包括边界相交）。
     * <br>
     * <br>所有输入坐标参数的坐标系必须和初始化时使用的geojson数据的坐标系一致，否则坐标可能会有比较大的偏移，导致查询结果不正确。
     * <br>如果还未完成初始化，或者查询出错，都会抛异常。
     * <br>本方法线程安全。
     *
     * @param geom 任意格式的图形对象（点、线、面），可以通过wkt文本进行构造：geom=new WKTReader(AreaCityQuery.Factory).read("wkt字符串")
     * @param where 可以为null，可选提供一个函数，筛选属性数据（此数据已经过初步筛选），会传入属性的json字符串，如果需要去精确计算这个边界图形是否匹配就返回true，否则返回false跳过这条边界图形的精确计算
     * @param res 可以为null，如果提供结果对象，可通过此对象的Set_XXX属性控制某些查询行为，比如设置Set_ReturnWKTKey可以额外返回边界的WKT文本数据；并且本次查询的结果和统计数据将累加到这个结果内（性能测试用）。注意：此结果对象非线程安全
     */
    public QueryResult QueryGeometry(Geometry geom, Func<String,Boolean> where, QueryResult res) throws Exception{
        return QueryGeometryProcess(geom, where, res, null);
    }
    /**
     * 几何计算查询出和此图形（点、线、面）有交点的所有边界图形的属性数据（包括边界相交）。
     * <br>
     * <br>参数功能和QueryGeometry方法一致，多了一个process参数允许在匹配计算时进行自定义计算处理
     * <br>更多参数文档请参考QueryGeometry方法，本方法线程安全。
     *
     * @see #QueryGeometry(Geometry, Func, QueryResult)
     * @param process 当一条数据经过精确匹配后，加入到结果中前，会调用此函数进行自定义计算，返回true继续加入到结果中，返回false丢弃这条数据；提供本函数后的查询性能会比不提供时低些，因为未去重增加了重复计算量。
     *            <br><br><b>注意：初始化时一个完整边界图形会在网格划分后产生多个小图形，匹配的每个小图形都会算作一条数据参与自定义计算，会导致结果数据重复，因此需要自行对结果数据进行去重</b>
     *            <br><br>参数为一个数组：
     *            <br>[0]String：可读写，当前数据属性的json字符串，修改后的json内容会放到结果中
     *            <br>[1]Geometry：当前数据的图形对象，用于计算，为网格划分后的小图形
     *            <br>[2]String：为当前数据对应的完整图形的唯一标识符，用于数据去重
     */
    public QueryResult QueryGeometryProcess(Geometry geom, Func<String,Boolean> where, QueryResult res, Func<Object[], Boolean> process) throws Exception{
        CheckInitIsOK();
        if(res==null) res=new QueryResult();
        res.QueryCount++;
        long t_Start=System.nanoTime();
        if(res.StartTimeN==0) res.StartTimeN=t_Start;

        boolean returnWkt=res.Set_ReturnWKTKey!=null && res.Set_ReturnWKTKey.length()>0;
        if(returnWkt && WkbsFilePath.length()==0) {
            throw new Exception("Set_ReturnWKT错误，初始化时必须保存了wkbs结构化数据文件，或者用的wkbs文件初始化的，否则不允许查询WKT数据");
        }

        //先查找Envelope，基本不消耗时间
        @SuppressWarnings("rawtypes")
        List list=EnvelopeSTRTree.query(geom.getEnvelopeInternal());
        res.DurationN_EnvelopeHitQuery+=System.nanoTime()-t_Start;
        res.EnvelopeHitCount+=list.size();

        //进行精确查找
        String matchLines=",";
        for(int i=0,len=list.size();i<len;i++) {
            @SuppressWarnings("unchecked")
            Map<String, Object> store=(Map<String, Object>)list.get(i);

            byte[] wkbSub=null;
            String[] wkbPos=getWkbPos(store);
            String lineNo=wkbPos[0];
            int fullPos=Integer.parseInt(wkbPos[1]);
            int subPos=Integer.parseInt(wkbPos[2]);

            //如果wkb对应的这条数据已经有一个sub匹配了，就不需要在继续查询
            if(process==null && matchLines.indexOf(","+lineNo+",")!=-1) {//提供了process自定义处理，不去重
                continue;
            }

            //提供了where筛选
            if(where!=null) {
                if(!where.Exec(getProp(store))) {
                    continue;
                }
            }

            //读取wkb数据
            long t_IO=System.nanoTime();
            Geometry subGeom=null;
            if(ReadFromMemory) {
                //从内存中得到wkb数据 或 直接存的对象
                if(SetInitStoreInMemoryUseObject) {
                    subGeom=(Geometry)store.get("wkb");
                } else {
                    wkbSub=(byte[])store.get("wkb");
                }
            } else {
                wkbSub=ReadWkbFromFile(subPos);
            }
            res.DurationN_IO+=System.nanoTime()-t_IO;

            //转换回图形
            long t_GeometryParse=System.nanoTime();
            if(subGeom==null) {
                subGeom=new WKBReader(Factory).read(wkbSub);
            }
            res.DurationN_GeometryParse+=System.nanoTime()-t_GeometryParse;

            //是否精确匹配
            long t_Exact=System.nanoTime();
            boolean isMatch=subGeom.intersects(geom);
            res.DurationN_ExactHitQuery+=System.nanoTime()-t_Exact;
            if(isMatch) {
                String prop=getProp(store);
                if(process!=null) { // 自定义计算
                    t_Exact=System.nanoTime();
                    Object[] args=new Object[] { prop, subGeom, lineNo };
                    if(!process.Exec(args)) {
                        isMatch=false;
                    } else {
                        prop=(String)args[0];
                    }
                    res.DurationN_ExactHitQuery+=System.nanoTime()-t_Exact;
                }

                if(isMatch) {
                    if(returnWkt) { // 需要同时返回完整图形的wkt数据
                        t_IO=System.nanoTime();
                        byte[] wkbFull=ReadWkbFromFile(fullPos);
                        res.DurationN_IO+=System.nanoTime()-t_IO;

                        t_GeometryParse=System.nanoTime();
                        Geometry fullGeom=new WKBReader(Factory).read(wkbFull);
                        res.DurationN_GeometryParse+=System.nanoTime()-t_GeometryParse;

                        String wkt=new WKTWriter().write(fullGeom);
                        prop=prop.substring(0, prop.length()-1)+", \""+res.Set_ReturnWKTKey+"\": \""+wkt+"\"}";
                    }

                    if(res.Result!=null) {
                        res.Result.add(prop);
                    }
                    res.ExactHitCount++;

                    matchLines+=lineNo+",";
                }
            }

            if(res.Set_EnvelopeHitResult!=null) {//将初步筛选的结果存入数组，如果要求了的话
                String prop=getProp(store);
                prop="{\"_PolygonPointNum_\": "+subGeom.getNumPoints()+","+prop.substring(1);
                res.Set_EnvelopeHitResult.add(prop);
            }
        }

        res.EndTimeN=System.nanoTime();
        return res;
    }



    /**
     * 遍历所有边界图形的属性列表查询出符合条件的属性，然后返回图形的属性+边界图形WKT文本。
     * <br>读取到的wkt文本，可以直接粘贴到页面内渲染显示：https://xiangyuecn.github.io/AreaCity-JsSpider-StatsGov/assets/geo-echarts.html
     * <br>本方法可以用来遍历所有数据，提取感兴趣的属性内容（wktKey传null只返回属性），比如查询一个区划编号id对应的城市信息（城市名称、中心点）
     *
     * <br>
     * <br>注意：初始化时必须保存了wkbs结构化数据文件，或者用的wkbs文件初始化的，否则不允许查询WKT数据。
     * <br>如果还未完成初始化，或者查询出错，都会抛异常。
     * <br>本方法线程安全。
     *
     * @param wktKey 可以为null，比如填：wkt、polygon，作为json里的key: 存放wkt文本数据；如果传入空值，将只返回属性，不查询wkt文本数据；此参数会覆盖res.Set_ReturnWKTKey值
     * @param res 可以为null，如果提供结果对象，可通过此对象的Set_XXX属性控制某些查询行为，并且本次查询的结果和统计数据将累加到这个结果内（性能测试用）。注意：此结果对象非线程安全
     * @param where 必须提供一个函数，筛选属性数据（所有数据全过一遍），会传入属性的json字符串，如果需要匹配这个边界图形就返回true，否则返回false跳过这条边界图形
     * @param onFind 可选提供一个回调函数，每次查询到一条wkt数据后会通过onFind回传，String[]参数为[prop,wkt]；如果返回false数据将不会存入res结果中（也会忽略wktKey参数），需在回调中自行处理数据
     */
    public QueryResult ReadWKT_FromWkbsFile(String wktKey, QueryResult res, Func<String,Boolean> where, Func<String[], Boolean> onFind) throws Exception{
        CheckInitIsOK();
        if(res==null) res=new QueryResult();
        res.QueryCount++;
        long t_Start=System.nanoTime();
        if(res.StartTimeN==0) res.StartTimeN=t_Start;

        res.Set_ReturnWKTKey=wktKey;
        boolean returnWkt=res.Set_ReturnWKTKey!=null && res.Set_ReturnWKTKey.length()>0;
        boolean readWkt=returnWkt;
        if(onFind!=null) {
            readWkt=true;
        }
        if(readWkt && WkbsFilePath.length()==0) {
            throw new Exception("初始化时必须保存了wkbs结构化数据文件，或者用的wkbs文件初始化的，否则不允许查询WKT数据");
        }

        for(int i=0,iL=WKTDataStores.size();i<iL;i++) {
            HashMap<String, Object> store=WKTDataStores.get(i);

            //属性是否符合条件
            long t_Exact=System.nanoTime();
            String prop=getProp(store);
            boolean isFind=where.Exec(prop);
            res.DurationN_ExactHitQuery+=System.nanoTime()-t_Exact;
            if(!isFind) {
                continue;
            }

            String wkt=null;
            if(readWkt) {
                //读取wkb
                byte[] wkbFull=null;
                if(!store.containsKey("empty")) {
                    String[] wkbPos=getWkbPos(store);
                    int fullPos=Integer.parseInt(wkbPos[1]);

                    long t_IO=System.nanoTime();
                    wkbFull=ReadWkbFromFile(fullPos);
                    res.DurationN_IO+=System.nanoTime()-t_IO;
                }

                //转换回图形
                long t_GeometryParse=System.nanoTime();
                Geometry fullGeom;
                if(wkbFull!=null) {
                    fullGeom=new WKBReader(Factory).read(wkbFull);
                } else {
                    fullGeom=Factory.createPolygon();
                }

                //生成wkt
                wkt=new WKTWriter().write(fullGeom);
                res.DurationN_GeometryParse+=System.nanoTime()-t_GeometryParse;
            }

            boolean add=true;
            if(onFind!=null) {
                add=onFind.Exec(new String[] { prop, wkt });
            }
            if (add && res.Result!=null) {
                if(returnWkt) {
                    prop=prop.substring(0, prop.length()-1)+", \""+res.Set_ReturnWKTKey+"\": \""+wkt+"\"}";
                }
                res.Result.add(prop);
            }
            res.ExactHitCount++;
        }

        res.EndTimeN=System.nanoTime();
        return res;
    }



    /**
     * 调试用的，读取已在wkbs结构化文件中保存的网格划分图形WKT数据，用于核对网格划分情况。
     * <br>读取到的wkt文本，可以直接粘贴到页面内渲染显示：https://xiangyuecn.github.io/AreaCity-JsSpider-StatsGov/assets/geo-echarts.html
     *
     * @param wktKey 可以为null，比如填：wkt、polygon，作为json里的key: 存放wkt文本数据；如果传入空值，将只返回属性，不查询wkt文本数据；此参数会覆盖res.Set_ReturnWKTKey值
     * @param res 可以为null，如果提供结果对象，可通过此对象的Set_XXX属性控制某些查询行为，并且本次查询的结果和统计数据将累加到这个结果内（性能测试用）。注意：此结果对象非线程安全
     * @param where 必须提供一个函数，筛选属性数据（所有数据全过一遍），会传入属性的json字符串，如果需要匹配这个边界图形就返回true，否则返回false跳过这条边界图形
     * @param onFind 可选提供一个回调函数，每次查询到一条wkt数据后会通过onFind回传，String[]参数为[prop,wkt]；如果返回false数据将不会存入res结果中（也会忽略wktKey参数），需在回调中自行处理数据
     */
    public QueryResult Debug_ReadGeometryGridSplitsWKT(String wktKey, QueryResult res, Func<String,Boolean> where, Func<String[], Boolean> onFind) throws Exception {
        CheckInitIsOK();
        if(res==null) res=new QueryResult();
        res.QueryCount++;
        long t_Start=System.nanoTime();
        if(res.StartTimeN==0) res.StartTimeN=t_Start;

        res.Set_ReturnWKTKey=wktKey;
        boolean returnWkt=res.Set_ReturnWKTKey!=null && res.Set_ReturnWKTKey.length()>0;
        boolean readWkt=returnWkt;
        if(onFind!=null) {
            readWkt=true;
        }
        if(readWkt && WkbsFilePath.length()==0) {
            throw new Exception("初始化时必须保存了wkbs结构化数据文件，或者用的wkbs文件初始化的，否则不允许查询WKT数据");
        }

        for(int i=0,iL=WKTDataStores.size();i<iL;i++) {
            HashMap<String, Object> store=WKTDataStores.get(i);

            //属性是否符合条件
            long t_Exact=System.nanoTime();
            String prop=getProp(store);
            boolean isFind=where.Exec(prop);
            res.DurationN_ExactHitQuery+=System.nanoTime()-t_Exact;
            if(!isFind) {
                continue;
            }

            String wkt=null;
            if(readWkt) {
                String[] wkbPos=getWkbPos(store);
                ArrayList<Integer> subs=LineSubsPos.get(wkbPos[0]);
                if(subs==null) {
                    continue;
                }

                //读取所有的切块，转换回图形
                ArrayList<Polygon> pols=new ArrayList<Polygon>();
                for(int i2=0,i2L=subs.size();i2<i2L;i2++) {
                    long t_IO=System.nanoTime();
                    byte[] wkb=ReadWkbFromFile(subs.get(i2));
                    res.DurationN_IO+=System.nanoTime()-t_IO;

                    long t_GeometryParse=System.nanoTime();
                    Geometry subGeom=new WKBReader(Factory).read(wkb);

                    if(subGeom instanceof Polygon) {
                        pols.add((Polygon)subGeom);
                    } else {
                        for(int i3=0,i3L=subGeom.getNumGeometries();i3<i3L;i3++) {
                            pols.add((Polygon)subGeom.getGeometryN(i3));
                        }
                    }
                    res.DurationN_GeometryParse+=System.nanoTime()-t_GeometryParse;
                }
                Geometry geom;
                if(pols.size()==0) {
                    geom=Factory.createPolygon();
                } else {
                    geom=Factory.createMultiPolygon(pols.toArray(new Polygon[0]));
                }
                wkt=new WKTWriter().write(geom);
            }

            boolean add=true;
            if(onFind!=null) {
                add=onFind.Exec(new String[] { prop, wkt });
            }
            if (add && res.Result!=null) {
                if(returnWkt) {
                    prop=prop.substring(0, prop.length()-1)+", \""+res.Set_ReturnWKTKey+"\": \""+wkt+"\"}";
                }
                res.Result.add(prop);
            }
            res.ExactHitCount++;
        }

        res.EndTimeN=System.nanoTime();
        return res;
    }




    /**
     * 用加载数据到内存的模式进行初始化，边界图形数据存入内存中（内存占用和json数据文件大小差不多大，查询性能极高）；本方法可以反复调用但只会初始化一次，每次查询前都调用即可（查询会在初始化完成后进行）
     * <pre>
     * 支持文件(utf-8)：
     *  - *.wkbs saveWkbsFilePath生成的结构化数据文件，读取效率高。
     *  - *.json geojson文件，要求里面数据必须是一行一条数据
     *                     ，第一条数据的上一行必须是`"features": [`
     *                     ，最后一条数据的下一行必须是`]`打头
     *                     ，否则不支持解析，可尝试用文本编辑器批量替换添加换行符。
     * </pre>
     * 默认在内存中存储的是wkb格式数据（大幅减少内存占用），查询时会将wkb还原成图形对象，可通过设置 Instances[0-9].SetInitStoreInMemoryUseObject=true 来关闭这一过程减少性能损耗，在内存中直接存储图形对象，但内存占用会增大一倍多。
     *
     * @param dataFilePath 数据文件路径（支持：*.wkbs、*.json），从这个文件读取数据；如果autoUseExistsWkbsFile=true并且saveWkbsFilePath文件存在时（已生成了结构化数据文件），可以不提供此参数
     * @param saveWkbsFilePath 可选提供一个.wkbs后缀的文件路径：dataFile是wkbs时不可以提供；dataFile是geojson时，加载geojson解析的数据会自动生成此结构化数据文件；如果和dataFile都不提供wkbs文件时查询中将不允许获取WKT数据
     * @param autoUseExistsWkbsFile 当传true时：如果检测到saveWkbsFilePath对应文件已成功生成过了，将直接使用这个wkbs文件作为dataFile（直接忽略dataFilePath参数）；建议传true，这样只需要首次加载生成了结构文件，以后读取数据都非常快（数据更新时需删除wkbs文件）
     */
    public void Init_StoreInMemory(String dataFilePath, String saveWkbsFilePath, boolean autoUseExistsWkbsFile) {
        __Init(autoUseExistsWkbsFile, dataFilePath, saveWkbsFilePath, true);
    }
    /**
     * 用加载数据到结构化数据文件的模式进行初始化，推荐使用本方法初始化，边界图形数据存入结构化数据文件中，内存占用很低（查询时会反复读取文件对应内容，查询性能消耗主要在IO上，IO性能极高问题不大）；本方法可以反复调用但只会初始化一次，每次查询前都调用即可（查询会在初始化完成后进行）
     * <pre>
     * 支持文件(utf-8)：
     *  - *.wkbs saveWkbsFilePath生成的结构化数据文件，读取效率高。
     *  - *.json geojson文件，要求里面数据必须是一行一条数据
     *                     ，第一条数据的上一行必须是`"features": [`
     *                     ，最后一条数据的下一行必须是`]`打头
     *                     ，否则不支持解析，可尝试用文本编辑器批量替换添加换行符。
     * </pre>
     *
     * @param dataFilePath 数据文件路径（支持：*.wkbs、*.json），从这个文件读取数据；如果autoUseExistsWkbsFile=true并且saveWkbsFilePath文件存在时（已生成了结构化数据文件），可以不提供此参数
     * @param saveWkbsFilePath 不提供，或一个.wkbs后缀的文件路径：dataFile是wkbs时不可以提供；dataFile是geojson时，必须提供，加载geojson解析的数据会存入此文件
     * @param autoUseExistsWkbsFile 当传true时：如果检测到saveWkbsFilePath对应文件已成功生成过了，将直接使用这个wkbs文件作为dataFile（直接忽略dataFilePath参数）；建议传true，这样只需要首次加载生成了结构文件，以后读取数据都非常快（数据更新时需删除wkbs文件）
     */
    public void Init_StoreInWkbsFile(String dataFilePath, String saveWkbsFilePath, boolean autoUseExistsWkbsFile) {
        __Init(autoUseExistsWkbsFile, dataFilePath, saveWkbsFilePath, false);
    }







    /** 版本号，主要用于wkbs结构化文件的版本 **/
    static public final String Version="1.0";

    /** 性能优化的重要参数，用于将大的边界按网格拆分成小的边界，这个参数决定了每个小边界的坐标点数在这个值附近
     * <br>取值越小，查询性能越高；初始化拆分出来的Polygon会越多，占用内存也会相应增多，解析json文件、或生成wkbs文件会比较耗时。
     * <br>取值越大，查询性能越低；初始化拆分出来的Polygon会越少，占用内存也会越少，解析json文件、或生成wkbs文件会比较快。
     * <br>如果不清楚作用，请勿调整此参数；修改后，之前生成的wkbs结构化文件均会失效，初始化时会重新生成。
     * **/
    public int SetGridFactor=100;

    /** init时允许使用的最大线程数量，默认为不超过5 并且 不超过cpu核心数-1；线程数不要太多， 默认就好**/
    public int SetInitUseThreadMax=5;

    /** init采用的Init_StoreInMemory时，图形数据直接存到内存，不要转成wkb压缩内存，可进一步提升性能，但会增大一倍多的内存占用 **/
    public boolean SetInitStoreInMemoryUseObject=false;

    /**
     * init状态：0未初始化，1初始化中，2初始化完成，3初始化失败（InitInfo.ErrMsg为错误消息）
     */
    public int GetInitStatus() {
        return InitLock[0];
    }
    /** 检查init状态是否是2已初始化完成，未完成会抛出错误原因 **/
    public void CheckInitIsOK() throws Exception {
        if(InitLock[0]==3) {
            throw new Exception(InitInfo.ErrMsg);
        }
        if(InitLock[0]!=2) {
            throw new Exception("需要先Init完成后，再来进行查询调用");
        }
    }
    /** 将init状态设置为0（未初始化），允许重新Init **/
    public void ResetInitStatus() {
        synchronized (InitLock) {
            InitLock[0] = 0;
            EnvelopeSTRTree = null;
            WKTDataStores = null;
            LineSubsPos = null;
        }
    }


    /** 是否是通过Init_StoreInMemory初始化的 **/
    public boolean IsStoreInMemory() {
        return GetInitStatus()==2 && ReadFromMemory;
    }
    /** 是否是通过Init_StoreInWkbsFile初始化的 **/
    public boolean IsStoreInWkbsFile() {
        return GetInitStatus()==2 && !ReadFromMemory;
    }

    /**
     * init时的回调，可以绑定一个函数，接收InitInfo进度信息，回调时机：
     * <pre>
     * - 每处理一行数据会回调一次，返回false可以跳过处理一行数据，此时initInfo.CurrentLine_XX全部有值
     * - 处理完成时会回调一次(此时initInfo.CurrentLine_XX全部为空)
     * </pre>
     * 此回调线程安全。
     */
    public Func<QueryInitInfo, Boolean> OnInitProgress;
    /**
     * init时的进度信息
     */
    public QueryInitInfo GetInitInfo() {
        return InitInfo;
    }
    private QueryInitInfo InitInfo;






    /** jts的factory，可以用来创建Geometry **/
    static public GeometryFactory Factory=new GeometryFactory(new PrecisionModel(), 4326);


    private int[] InitLock=new int[] { 0 };//0未初始化，1初始化中，2初始化完成，3初始化失败
    private boolean ReadFromMemory;
    private String WkbsFilePath;
    private STRtree EnvelopeSTRTree; //所有图形的外接矩形索引
    private List<HashMap<String,Object>> WKTDataStores; //WKT查询时需要读取的属性列表
    private HashMap<String, ArrayList<Integer>> LineSubsPos; //每行数据grid拆分后的数据在wkbs里面的存储位置
    private void __Init(boolean autoUseExistsWkbsFile, String dataFilePath, String saveWkbsFilePath, boolean readFromMemory) {
        if(InitLock[0] >= 2) {
            return;
        }
        synchronized (InitLock) {
            if(InitLock[0] >= 2) {
                return;
            }
            FileOutputStream fw=null;
            FileInputStream fr=null;
            BufferedReader read=null;

            InitLock[0]=1;
            try {
                InitInfo=new QueryInitInfo();
                InitInfo.StartTimeN = System.nanoTime();
                InitInfo.StartMemory_System = GetMemory_System();
                InitInfo.StartMemory_JavaRuntime = GetMemory_JavaRuntime();

                ReadFromMemory=readFromMemory;
                WkbsFilePath="";

                dataFilePath=dataFilePath==null?"":dataFilePath;
                saveWkbsFilePath=saveWkbsFilePath==null?"":saveWkbsFilePath;
                if(saveWkbsFilePath.length()>0) {
                    WkbsFilePath=saveWkbsFilePath;
                }else if(IsWkbsFilePath(dataFilePath)) {
                    WkbsFilePath=dataFilePath;
                }else if(!ReadFromMemory){
                    throw new Exception("Init_StoreInWkbsFile传入非wkbs文件时，必须提供saveWkbsFilePath");
                }
                if(saveWkbsFilePath.length()>0) {
                    if(!IsWkbsFilePath(saveWkbsFilePath)) {
                        throw new Exception("saveWkbsFilePath必须是.wkbs结尾");
                    }
                    if(IsWkbsFilePath(dataFilePath)) {
                        throw new Exception("dataFilePath是.wkbs文件时，不允许再提供saveWkbsFilePath");
                    }

                    if(autoUseExistsWkbsFile){//如果wkbs文件已存在，并且有效，就直接读取这个文件的数据
                        if(AvailableWkbsFile(saveWkbsFilePath)) {
                            dataFilePath=saveWkbsFilePath;
                            saveWkbsFilePath="";
                        }
                    }
                }
                InitInfo.DataFromWkbsFile=IsWkbsFilePath(dataFilePath);
                InitInfo.HasWkbsFile=WkbsFilePath.length()>0;
                InitInfo.FilePath_Data=dataFilePath;
                InitInfo.FilePath_SaveWkbs=saveWkbsFilePath;

                //打开文件
                fr=new FileInputStream(dataFilePath);
                read=new BufferedReader(new InputStreamReader(fr, "utf-8"));
                if(saveWkbsFilePath.length()>0) {
                    fw=new FileOutputStream(saveWkbsFilePath);
                }

                __InitProcess(dataFilePath, read, saveWkbsFilePath, fw);

                EnvelopeSTRTree.build();//立即生成索引树

                InitLock[0]=2;
            } catch (Exception e) {
                InitInfo.ErrMsg="初始化发生异常："+ErrorStack(e);
                InitLock[0]=3;
            } finally {
                try { if(fw!=null) fw.close(); } catch(Exception e) {}
                try { if(fr!=null) fr.close(); } catch(Exception e) {}
                try { if(read!=null) read.close(); } catch(Exception e) {}

                long t_gc=System.nanoTime();
                System.gc();//强制回收内存
                InitInfo.DurationN_JavaGC=System.nanoTime()-t_gc;

                InitInfo.CurrentLine_No=0;
                InitInfo.CurrentLine_Text="";
                InitInfo.CurrentLine_Prop="";

                InitInfo.EndTimeN = System.nanoTime();
                InitInfo.EndMemory_System = GetMemory_System();
                InitInfo.EndMemory_JavaRuntime = GetMemory_JavaRuntime();
            }

            //初始化完成了，回调一下进度
            if(OnInitProgress!=null) {
                try {
                    OnInitProgress.Exec(InitInfo);
                } catch (Exception e) { }
            }
        }
    }
    private void __InitProcess(String dataFilePath, BufferedReader dataFile, String saveWkbsFilePath, FileOutputStream saveWkbsFile) throws Exception {
        Exception[] threadError=new Exception[] { null };

        STRtree rtree=new STRtree();
        List<HashMap<String,Object>> wktDataStores=new ArrayList<>();
        List<HashMap<String,Object>> emptyGeoms=new ArrayList<>();
        HashMap<String, ArrayList<Integer>> lineSubsPos=new HashMap<>();

        boolean isWkbsFile=IsWkbsFilePath(dataFilePath);
        String IsStartErrMsg="未识别到geojson|wkbs数据，请检查初始化传入的文件是否正确。"
                +"注意：如果是geojson文件，要求里面数据必须是一行一条数据"
                +"，第一条数据的上一行必须是`\"features\": [`，最后一条数据的下一行必须是`]`打头"
                +"，否则不支持解析，可尝试用文本编辑器批量替换添加换行符。";
        boolean[] IsStart=new boolean[] { false };
        boolean[] IsEnd=new boolean[] { false };
        int[] LineNo=new int[] { 0 };
        HashMap<String, String[]> Strings=new HashMap<>();//prop字符串转成引用类型

        //写入wkbs文件，并记录已写入长度
        int[] saveWkbsFileLength=new int[] { 0 };
        Func<String, Object> SaveWkbsWrite=new Func<String, Object>() {
            @Override
            public Object Exec(String val) throws Exception {
                byte[] bs=val.getBytes("utf-8");
                saveWkbsFileLength[0]+=bs.length;
                saveWkbsFile.write(bs);
                return null;
            }
        };

        Func<Object, Object> ThreadExec=new Func<Object, Object>() {
            @Override
            public String Exec(Object val) throws Exception {
                while(true) {
                    int lineNo;
                    String line;
                    synchronized (dataFile) {//先读取一行内容，文件内容之类的识别不允许并行
                        if(threadError[0]!=null) throw threadError[0];
                        if(IsEnd[0]) break;

                        long t_fr=System.nanoTime();
                        line=dataFile.readLine();
                        InitInfo.DurationN_FileRead+=System.nanoTime()-t_fr;
                        if(line==null) {
                            //没有数据了
                            if(!IsStart[0]){
                                throw new Exception(IsStartErrMsg);
                            }
                            if(!IsEnd[0]){
                                throw new Exception("初始化传入的文件未发现结束位置，可能文件已损坏");
                            }
                            break;
                        }
                        lineNo=++LineNo[0];
                        line=line.trim();
                        if(line.length()==0)continue;

                        if(IsStart[0] && line.charAt(0)==']'){
                            //处理完成所有数据
                            IsEnd[0]=true;
                            break;
                        }
                        if(!IsStart[0]){
                            //等待开始标志
                            int fIdx=line.indexOf("\"features\"");
                            if(fIdx==0 || fIdx>0 && fIdx>=line.length()-14){
                                if(!line.endsWith("[")){
                                    throw new Exception("初始化传入的文件第"+lineNo+"行风格不对，不支持处理此文件");
                                }
                                IsStart[0]=true;

                                if(saveWkbsFile!=null) {//写入 wkbs 文件头，这里无需同步操作
                                    SaveWkbsWrite.Exec("/*******************"
                                            +"\n本wkbs文件是由 "+AreaCityQuery.class.getTypeName()+" 生成，为专用的结构化数据文件，用于边界图形数据加速解析。"
                                            +"\n@Version: "+Version
                                            +"\n@GridFactor: "+SetGridFactor
                                            +"\n@数据文件: "+dataFilePath
                                            +"\n@生成时间: "+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
                                            +"\n"
                                            +"\nGitHub: https://github.com/xiangyuecn/AreaCity-Query-Geometry （github可换成gitee）"
                                            +"\n省市区县乡镇区划边界数据: https://github.com/xiangyuecn/AreaCity-JsSpider-StatsGov （github可换成gitee）"
                                            +"\n*******************/"
                                            +"\n"
                                            +"\n\"features\": [");
                                }
                            }
                            continue;
                        }
                    }// synchronized end

                    //开始处理这一行数据

                    long r_t1=System.nanoTime();
                    //手工提取properties
                    String[] propStrPtr;
                    String propStr,wkbPosStr=lineNo+":0:0";
                    boolean wkbTypeIsParent=false,wkbTypeIsSub=false,wkbTypeIsEmpty=false;
                    int wkbIdx=0;
                    if(isWkbsFile){
                        int i0=line.indexOf(WKB_SP_Pos);
                        String wkbType=line.substring(0, i0);
                        if(wkbType.equals("Sub")) {
                            wkbTypeIsSub=true;
                        } else if(wkbType.equals("Full")) {
                            // NOOP
                        } else if(wkbType.equals("Parent")) {
                            wkbTypeIsParent=true;
                        } else if(wkbType.equals("Empty")) {
                            wkbTypeIsEmpty=true;
                        }

                        i0+=WKB_SP_Pos.length();
                        int i1=line.indexOf(WKB_SP_Prop, i0);
                        wkbPosStr=line.substring(i0, i1);

                        i1+=WKB_SP_Prop.length();
                        int i2=line.indexOf(WKB_SP_WKB, i1);
                        propStr=line.substring(i1, i2);
                        wkbIdx=i2+WKB_SP_WKB.length();
                    } else {
                        int i0=line.indexOf("properties\"");
                        int i1=line.indexOf("{", i0);
                        int i2=line.indexOf("}", i0);
                        propStr=line.substring(i1, i2+1);
                    }
                    //手工提取geometry类型
                    String typeStr="";
                    if(!isWkbsFile){
                        int iGeom=line.indexOf("geometry\"");

                        int i0=line.indexOf("type\"", iGeom);
                        int i1=line.indexOf("\"", i0+5);
                        int i2=line.indexOf("\"", i1+1);
                        typeStr=line.substring(i1+1, i2);
                    }
                    synchronized (InitInfo) {
                        InitInfo.DurationN_FileParse+=System.nanoTime()-r_t1;

                        InitInfo.CurrentLine_No=lineNo;
                        InitInfo.CurrentLine_Text=line;
                        InitInfo.CurrentLine_Prop=propStr;

                        //回调一下，顺带看看需不需要解析这条数据
                        if(OnInitProgress!=null) {
                            if(!OnInitProgress.Exec(InitInfo)) {
                                continue;
                            }
                        }

                        //在这个同步块里面顺带处理一下字符串转引用类型，减少字符串内存占用
                        propStrPtr=Strings.get(propStr);
                        if(propStrPtr==null) {
                            propStrPtr=new String[] { propStr };
                            Strings.put(propStr, propStrPtr);
                        }
                    }

                    //wkbs里面的非Sub图形，完整图形
                    if(isWkbsFile && !wkbTypeIsSub) {
                        synchronized (InitInfo) {
                            InitInfo.GeometryCount++;
                        }
                        if(!wkbTypeIsEmpty) {//empty的丢到下面统一处理
                            HashMap<String,Object> store=new HashMap<>();
                            store.put("prop", propStrPtr);
                            store.put("wkbPos", wkbPosStr);
                            synchronized (wktDataStores) {
                                wktDataStores.add(store);//存好WKT查询数据，一个数据只存一条就行了
                            }
                        }
                        if(wkbTypeIsParent) {//已经拆分了，上级完整图形无需再处理
                            continue;
                        }
                    }

                    //手工创建图形对象
                    long r_t2=System.nanoTime();
                    Geometry geomSrc;
                    if(isWkbsFile){
                        byte[] wkb=Hex2Bytes(line, wkbIdx);
                        geomSrc=new WKBReader(Factory).read(wkb);
                    } else {
                        if(!(typeStr.equals("Polygon") || typeStr.equals("MultiPolygon"))) {
                            throw new Exception("初始化传入的文件第"+lineNo+"行"+typeStr+"数据不是Polygon类，要求必须是Polygon或者MultiPolygon，并且json文件内一条数据一行");
                        }
                        geomSrc=JSONLineParse(Factory, line);
                    }
                    synchronized (InitInfo) {
                        InitInfo.DurationN_GeometryParse+=System.nanoTime()-r_t2;
                        if(!isWkbsFile) {
                            InitInfo.GeometryCount++;
                        }

                        if(geomSrc.isEmpty()){//空的存一下属性，边界就丢弃
                            HashMap<String,Object> store=new HashMap<>();
                            store.put("prop", propStrPtr);
                            store.put("wkbPos", wkbPosStr);
                            store.put("empty", true);
                            emptyGeoms.add(store);
                            continue;
                        }
                    }

                    //创建索引，将每个图形放到rtree，图形如果坐标点过多，先按网格拆成小的
                    long r_t3=System.nanoTime();
                    Geometry geomGrid=geomSrc;
                    if(!isWkbsFile) { //wkbs文件已经拆好了，非wkbs才需要按网格拆成小的
                        geomGrid=GeometryGridSplit(Factory, geomSrc, SetGridFactor);
                    }
                    int wkbMemoryLen=0;
                    int polygonNum=1;
                    if(geomGrid instanceof MultiPolygon) {
                        polygonNum = geomGrid.getNumGeometries();
                    }

                    int parentPos=0;
                    if(polygonNum>1 && saveWkbsFile!=null) {//有多个Polygon时，先存一个完整的父级
                        byte[] wkb=new WKBWriter().write(geomSrc);
                        synchronized (saveWkbsFile) {
                            parentPos=saveWkbsFileLength[0]+1;//+1 换行符
                            String wkbPos=lineNo+":"+parentPos+":"+parentPos; //编号:parent:sub 数据存储位置

                            SaveWkbsWrite.Exec("\nParent"+WKB_SP_Pos+wkbPos+WKB_SP_Prop+propStr+WKB_SP_WKB+Bytes2Hex(wkb));
                        }
                    }

                    for(int i0=0;i0<polygonNum;i0++) {
                        Polygon polygon;
                        if(geomGrid instanceof MultiPolygon) {//MultiPolygon 拆成 Polygon 减小范围
                            polygon=(Polygon)geomGrid.getGeometryN(i0);
                        }else{
                            polygon=(Polygon)geomGrid;
                        }

                        byte[] wkb=null;
                        String wkbPos=lineNo+":0:0";//编号:parent:sub 数据存储位置
                        if(saveWkbsFile!=null) {//需要保存到文件
                            synchronized (saveWkbsFile) {
                                wkbPos=(saveWkbsFileLength[0]+1)+"";//+1 换行符
                                String type="Sub";
                                if(polygonNum==1) {//自己本身就是完整的，无需parent
                                    type="Full";
                                    wkbPos=wkbPos+":"+wkbPos;
                                } else {
                                    wkbPos=parentPos+":"+wkbPos;
                                }
                                wkbPos=lineNo+":"+wkbPos;
                                wkb=new WKBWriter().write(polygon);
                                SaveWkbsWrite.Exec("\n"+type+WKB_SP_Pos+wkbPos+WKB_SP_Prop+propStr+WKB_SP_WKB+Bytes2Hex(wkb));
                            }
                        }

                        HashMap<String,Object> store=new HashMap<>();
                        store.put("prop", propStrPtr);
                        if(ReadFromMemory){//写入内存
                            if(SetInitStoreInMemoryUseObject) {
                                store.put("wkb", polygon);
                            }else {
                                if(wkb==null) {
                                    wkb=new WKBWriter().write(polygon);
                                }
                                wkbMemoryLen+=wkb.length;
                                store.put("wkb", wkb);
                            }
                        }

                        if(isWkbsFile) {//从wkbs文件读的数据，直接给数据位置值
                            wkbPos=wkbPosStr;
                        }
                        store.put("wkbPos", wkbPos);
                        String[] wkbPosArr=getWkbPos(store);

                        //构造外接矩形，放到rtree里面，非线程安全需同步操作
                        synchronized (rtree) {
                            rtree.insert(polygon.getEnvelopeInternal(), store);

                            //在这个同步块里面顺带把sub添加到这行数据的引用列表中
                            ArrayList<Integer> subs=lineSubsPos.get(wkbPosArr[0]);
                            if(subs==null) {
                                subs=new ArrayList<>();
                                lineSubsPos.put(wkbPosArr[0], subs);
                            }
                            subs.add(Integer.parseInt(wkbPosArr[2]));
                        }
                        if(i0==0 && !isWkbsFile) {
                            //这个只在查询完整wkt数据时才有用，一个数据只存一条就行了，wkbs的上面已经存好了
                            synchronized (wktDataStores) {
                                wktDataStores.add(store);
                            }
                        }
                    }
                    synchronized (InitInfo) {
                        InitInfo.DurationN_Index+=System.nanoTime()-r_t3;

                        if(ReadFromMemory){
                            if(InitInfo.WkbMemory==-1)InitInfo.WkbMemory=0;
                            InitInfo.WkbMemory+=wkbMemoryLen;
                        }

                        InitInfo.PolygonCount+=polygonNum;
                    }
                }
                return null;
            }
        };


        // 开启多线程处理读取的数据，留一个核打酱油
        int[] threadCount=new int[] { Math.max(1, Math.min(SetInitUseThreadMax, Runtime.getRuntime().availableProcessors()-1)) };
        InitInfo.UseThreadCount=threadCount[0];
        for(int i=0;i<threadCount[0];i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ThreadExec.Exec(null);
                    } catch(Exception e) {
                        if(threadError[0]==null) {
                            threadError[0]=e;
                        }
                    } finally {
                        synchronized (threadCount) { threadCount[0]--; }
                    }
                }
            }).start();
        }
        while(threadCount[0]>0) {
            try { Thread.sleep(10); }catch (Exception e) { }
        }
        if(threadError[0]!=null) {
            throw threadError[0];
        }

        if(!IsStart[0]){
            throw new Exception(IsStartErrMsg);
        }
        if(InitInfo.GeometryCount==0){
            throw new Exception("初始化传入的文件内没有数据");
        }

        //统一处理empty那些图形
        for(int i=0,iL=emptyGeoms.size();i<iL;i++) {
            HashMap<String,Object> store=emptyGeoms.get(i);
            wktDataStores.add(store);

            if(saveWkbsFile!=null) {
                String propStr=getProp(store);
                String lineNo=getWkbPos(store)[0];
                String wkbPos=(saveWkbsFileLength[0]+1)+"";//+1 换行符
                wkbPos=lineNo+":"+wkbPos+":"+wkbPos; //parent:sub 数据存储位置
                store.put("wkbPos", wkbPos);

                byte[] wkb=new WKBWriter().write(Factory.createPolygon());
                SaveWkbsWrite.Exec("\nEmpty"+WKB_SP_Pos+wkbPos+WKB_SP_Prop+propStr+WKB_SP_WKB+Bytes2Hex(wkb));
            }
        }

        if(saveWkbsFile!=null) {//写入 wkbs 文件结尾
            SaveWkbsWrite.Exec("\n]");
        }
        LineSubsPos=lineSubsPos;
        WKTDataStores=wktDataStores;
        EnvelopeSTRTree=rtree;
    }
    static private final String WKB_SP_Prop="|Prop:",WKB_SP_Pos="|Pos:",WKB_SP_WKB="|WKB:";


    private String getProp(Map<String,Object> store) {
        return ((String[])store.get("prop"))[0];
    }
    /**从保存的数据中提取出位置信息**/
    private String[] getWkbPos(Map<String,Object> store) {
        String str=(String)store.get("wkbPos");
        int p0=str.indexOf(':');
        int p1=str.indexOf(':', p0+1);
        return new String[] {
                str.substring(0, p0)
                ,str.substring(p0+1, p1)
                ,str.substring(p1+1)
        };
    }
    /**
     * 检测结构化数据文件是否有效
     */
    private boolean AvailableWkbsFile(String path) {
        File file=new File(path);
        if(!file.exists())return false;
        try(FileInputStream in=new FileInputStream(path)) {
            byte[] buffer=new byte[8*1024];
            int len=in.read(buffer);
            String txt=new String(buffer, "utf-8");
            if(!txt.contains("@Version: "+Version+"\n")) {
                return false;
            }
            if(!txt.contains("@GridFactor: "+SetGridFactor+"\n")) {
                return false;
            }

            in.skip(-len);// 请注意，倒车
            in.skip(file.length()-1);
            return in.read()==']';// 成功写入了结尾符号
        }catch (Exception e) {
            return false;
        }
    }
    /**
     * 从结构化数据文件中读取一条wkb数据
     */
    private byte[] ReadWkbFromFile(int pos) throws Exception {
        try(FileInputStream in=new FileInputStream(WkbsFilePath)) { // RandomAccessFile 没有区别，文件流无需缓存 新打开流不消耗性能，并发控制反而会影响性能
            in.skip(pos);
            ByteArrayOutputStream bs=new ByteArrayOutputStream();
            byte[] buffer=new byte[32*1024];
            int len=0;
            boolean isStart=false;
            int findLen=0;
            char[] FindChars=WKB_SP_WKB.toCharArray();
            while((len=in.read(buffer))!=-1) {
                int i0=0;
                if(!isStart) {//查找 WKB_SP_WKB
                    for(int i=0;i<len;i++) {
                        if(buffer[i]==FindChars[0]) {
                            findLen=1;
                            continue;
                        }
                        if(findLen==0)continue;
                        if(buffer[i]==FindChars[findLen]) {
                            findLen++;
                            if(findLen==FindChars.length) {
                                isStart=true;
                                i0=i+1;
                                break;
                            }
                        } else {
                            findLen=0;
                        }
                    }
                }
                //查找结尾的\n
                boolean isEnd=false;
                int i1=len;
                for(int i=i0;i<len;i++) {
                    if(buffer[i]=='\n') {
                        isEnd=true;
                        i1=i;
                        break;
                    }
                }

                if(i1-i0>0) {
                    bs.write(buffer, i0, i1-i0);
                }
                if(isEnd) {
                    byte[] byts=bs.toByteArray();
                    if(byts.length%2==1) {
                        throw new Exception("结构化数据内部存在错误");
                    }
                    return Hex2Bytes(byts, 0);
                }
            }
        }
        throw new Exception("结构化数据文件已损坏");
    }













    //===========================一些函数和方法===================================




    /** 通用回调接口 **/
    public interface Func<iT, oT> { oT Exec(iT val) throws Exception; }




    /**
     * 将一行JSON数据转换成Geometry对象，要求一个JSON图形数据必须占用一行文本，高性能！
     */
    static private Geometry JSONLineParse(GeometryFactory factory, String line) {
        ArrayList<__ParsePolygon> multiPols=new ArrayList<>();
        int iGeom=line.indexOf("geometry\"");
        for(int i=line.indexOf("coordinates\"", iGeom),L=line.length();i<L;) {
            __ParsePolygon pr=new __ParsePolygon(factory, line, i);
            if(pr.isFind) {
                i=pr.lastIndex;
                multiPols.add(pr);
            } else {
                break;
            }
        }

        if(multiPols.size()==1) {
            return multiPols.get(0).toPolygon(factory);
        } else if(multiPols.size()>1) {
            Polygon[] pols=new Polygon[multiPols.size()];
            for(int i=0,L=multiPols.size();i<L;i++) {
                pols[i]=multiPols.get(i).toPolygon(factory);
            }
            return factory.createMultiPolygon(pols);
        } else {
            return factory.createPolygon();
        }
    }

    static private class __ParsePolygon{
        public __ParsePolygon(GeometryFactory factory, String line, int index) {
            //手工提取图形坐标点
            ArrayList<Coordinate> points=new ArrayList<>();
            boolean isStart=false; int multiEnds=0,polEnds=0;
            int i0=index;
            for(int Li=line.length();i0<Li;i0++) {
                char c=line.charAt(i0);
                if(c==' ')continue;
                if(c=='}')break;
                if(!isStart) {
                    if(c=='[') isStart=true;
                    continue;
                }
                if(c==']') {
                    polEnds++;
                    if(polEnds==2) {// 环结束
                        LinearRing ring=factory.createLinearRing(points.toArray(new Coordinate[0]));
                        if(ring0==null) {
                            ring0=ring;
                        }else{
                            ringX.add(ring);
                        }
                        points=new ArrayList<>();
                    }
                    multiEnds++;
                    if(multiEnds==3) {// MultiPolygon结束
                        i0++;break;
                    }
                    continue;
                }
                polEnds=0;
                multiEnds=0;

                if(c==',' || c=='[')continue;

                StringBuilder lng=new StringBuilder(),lat=new StringBuilder();
                for(;i0<Li;i0++) {
                    c=line.charAt(i0);
                    if(c==' ') continue;
                    if(c==',') { i0++; break;}
                    lng.append(c);
                }
                for(;i0<Li;i0++) {
                    c=line.charAt(i0);
                    if(c==' ') continue;
                    if(c==']') { i0--; break; }
                    lat.append(c);
                }
                points.add(new Coordinate(
                        Double.parseDouble(lng.toString())
                        , Double.parseDouble(lat.toString())));
            }
            lastIndex=i0;
            isFind=ring0!=null;
        }

        public boolean isFind;
        public int lastIndex;

        private LinearRing ring0;
        private ArrayList<LinearRing> ringX=new ArrayList<>();
        public Polygon toPolygon(GeometryFactory factory) {
            if(ring0==null) {
                return factory.createPolygon();
            }
            LinearRing[] holes=null;
            if(ringX.size()>0) {
                holes=ringX.toArray(new LinearRing[0]);
            }
            return factory.createPolygon(ring0, holes);
        }
    }


    /**
     * 将坐标点数过多的边界，使用网格进行拆分成小块
     */
    static private Geometry GeometryGridSplit(GeometryFactory factory, Geometry geom, int gridFactor) {
        ArrayList<Polygon> pols=new ArrayList<>();
        if(geom instanceof Polygon) {
            __PolygonGridSplit(factory, gridFactor, pols, (Polygon)geom);
        } else {
            for(int i=0,L=geom.getNumGeometries();i<L;i++) {
                __PolygonGridSplit(factory, gridFactor, pols, (Polygon)geom.getGeometryN(i));
            }
        }
        if(pols.size()==1) {
            return pols.get(0);
        }
        return factory.createMultiPolygon(pols.toArray(new Polygon[0]));
    }
    static private void __PolygonGridSplit(GeometryFactory factory, int gridFactor, ArrayList<Polygon> pols, Polygon polygon) {
        int pointCount=polygon.getNumPoints();
        int gridPoint=(int)Math.round(1.0*pointCount/gridFactor);//最外层的1格平均分担点数，计算最外层网格边数
        if(gridPoint<2) {//没必要拆分了
            pols.add(polygon);
            return;
        }
        Envelope box=polygon.getEnvelopeInternal();

        //按最长的一边，对中切开，切成两块，然后递归去切
        double width=box.getMaxX()-box.getMinX();
        double height=box.getMaxY()-box.getMinY();
        int gridX=1,gridY=1;//xy轴列数
        if(width/(height*2)>1) {//x轴更长，切x轴，纬度简单*2 当做跟 经度 一样
            gridX++;
        } else {
            gridY++;
        }
        double xStep=width/gridX;
        double yStep=height/gridY;

        double x_0=box.getMinX(),y_00=box.getMinY();
        double x_1=box.getMaxX(),y_1=box.getMaxY();
        while(x_0-x_1<-xStep/2) {//注意浮点数±0.000000001的差异
            double x0=x_0, x1=x_0+xStep; x_0=x1;
            double y_0=y_00;
            while(y_0-y_1<-yStep/2) {
                double y0=y_0, y1=y_0+yStep; y_0=y1;
                Polygon gridItem=factory.createPolygon(new Coordinate[] {
                        new Coordinate(x0, y0)
                        , new Coordinate(x0, y1)
                        , new Coordinate(x1, y1)
                        , new Coordinate(x1, y0)
                        , new Coordinate(x0, y0)
                });
                Geometry chunk=polygon.intersection(gridItem);
                if(!chunk.isEmpty()) {
                    //如果有大的就继续拆分
                    if(chunk instanceof Polygon) {
                        __PolygonGridSplit(factory, gridFactor, pols, (Polygon)chunk);
                    } else {
                        for(int i2=0,L2=chunk.getNumGeometries();i2<L2;i2++) {
                            Geometry item=chunk.getGeometryN(i2);
                            if(item instanceof Polygon) { //偶尔出现LineString
                                __PolygonGridSplit(factory, gridFactor, pols, (Polygon)item);
                            }
                        }
                    }
                }
            }
        }
    }


    /** 计算两个坐标的距离，单位米 **/
    static public double Distance(double lng1, double lat1, double lng2, double lat2) {
        //采用Haversine formula算法，高德地图的js计算代码，比较简洁 https://www.cnblogs.com/ggz19/p/7551088.html
        double d=Math.PI/180;
        double f=lat1*d, h=lat2*d;
        double i=lng2*d - lng1*d;
        double e=(1 - Math.cos(h - f) + (1 - Math.cos(i)) * Math.cos(f) * Math.cos(h)) / 2;
        return 2 * 6378137 * Math.asin(Math.sqrt(e));
    }
    /** 以坐标点为中心，简单粗略的创建一个指定半径的圆，半径单位米，pointCount为构建圆的坐标点数（比如24个点，点越多越圆，最少3个点） **/
    static public Geometry CreateSimpleCircle(double lng, double lat, double radius, int pointCount) {
        //球面坐标不会算，转换成三角坐标简单点，经度代表值大约：0.01≈1km 0.1≈10km 1≈100km 10≈1000km
        double km=radius/1000;
        double a=km<5?0.01 :km<50?0.1 :km<500?1 :10;
        double b=Distance(lng, lat, lng+a, lat);
        double c=Distance(lng, lat, lng, lat+a);
        double rb=radius/b*a;
        double rc=radius/c*a;
        Coordinate[] arr=new Coordinate[pointCount+1];
        double n=0,step=360.0/pointCount,N=360-step/2; //注意浮点数±0.000000001的差异
        for(int i=0;n<N;i++,n+=step){
            double x=lng+rb*Math.cos(n*Math.PI/180);
            double y=lat+rc*Math.sin(n*Math.PI/180);
            arr[i]=new Coordinate(x, y);
        }
        arr[pointCount]=arr[0];
        return Factory.createPolygon(arr);
    }
    /** 通过两个坐标点构造一个矩形 **/
    static public Geometry CreateRect(double lng1, double lat1, double lng2, double lat2) {
        return Factory.createPolygon(new Coordinate[] {
                new Coordinate(lng1, lat1), new Coordinate(lng1, lat2)
                ,new Coordinate(lng2, lat2), new Coordinate(lng2, lat1)
                ,new Coordinate(lng1, lat1)
        });
    }



    /**判断是不是.wkbs结尾的文件路径**/
    static private boolean IsWkbsFilePath(String path) {
        return path.toLowerCase().endsWith(".wkbs");
    }

    /**二进制内容转成16进制文本(大写)**/
    static private String Bytes2Hex(byte[] bytes) {
        StringBuffer str=new StringBuffer();
        for (int i=0; i<bytes.length; i++) {
            byte b=bytes[i];
            int b1=(b>>4) & 0x0F;
            int b2=b & 0x0F;
            str.append((char)(b1<=9?'0'+b1:'A'+b1-10));
            str.append((char)(b2<=9?'0'+b2:'A'+b2-10));
        }
        return str.toString();
    }
    /**16进制文本转成二进制内容，从指定位置开始转**/
    static private byte[] Hex2Bytes(String hex, int start) {
        byte[] val=new byte[(hex.length()-start)/2];
        for(int i=start,n=0,len=hex.length();i<len;n++) {
            int c1=hex.charAt(i++);
            int c2=hex.charAt(i++);

            c1=c1<'A'?c1-'0':c1<'a'?c1-'A'+10:c1-'a'+10;
            c2=c2<'A'?c2-'0':c2<'a'?c2-'A'+10:c2-'a'+10;

            val[n]=(byte)((c1 << 4) | c2);
        }
        return val;
    }
    /**16进制数据转成二进制内容，从指定位置开始转**/
    static private byte[] Hex2Bytes(byte[] hex, int start) {
        byte[] val=new byte[(hex.length-start)/2];
        for(int i=start,n=0,len=hex.length;i<len;n++) {
            int c1=hex[i++];
            int c2=hex[i++];

            c1=c1<'A'?c1-'0':c1<'a'?c1-'A'+10:c1-'a'+10;
            c2=c2<'A'?c2-'0':c2<'a'?c2-'A'+10:c2-'a'+10;

            val[n]=(byte)((c1 << 4) | c2);
        }
        return val;
    }




    /**将错误堆栈转成字符串**/
    static private String ErrorStack(Throwable e) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
    /**纳秒显示成ms，小数点后有数字开始两位**/
    static private String Nano(double nanoTime) {
        String ts=nanoTime/1000000.0+"";
        BigDecimal big=new BigDecimal(ts);
        String s=ts.split("\\.")[1];
        int c=0;
        for(int i=0;i<s.length();i++) {
            if(s.charAt(i)>'0') {
                c+=2; break;
            }
            c++;
        }
        big=big.setScale(c, RoundingMode.HALF_UP);

        return big.toString()+"ms";
    }
    /**计算两个内存字节数差值，然后显示成MB**/
    static private String Memory(long memory) {
        return memory/1024/1024+"MB";
    }
    /**获取当前系统内存已用字节数**/
    static private long GetMemory_System() {
        try {
            OperatingSystemMXBean osMX = ManagementFactory.getOperatingSystemMXBean();
            Method totalFn=osMX.getClass().getMethod("getTotalPhysicalMemorySize");
            totalFn.setAccessible(true);
            Method freeFn=osMX.getClass().getMethod("getFreePhysicalMemorySize");
            freeFn.setAccessible(true);
            return (long)totalFn.invoke(osMX) - (long)freeFn.invoke(osMX);
        }catch (Exception e) {
            return 0;
        }
    }
    /**获取当前Java Runtime内存已用字节数**/
    static private long GetMemory_JavaRuntime() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }



    /** 初始化是的相关信息对象 **/
    static public class QueryInitInfo {
        /** 开始时间，纳秒 **/
        public long StartTimeN;
        /** 结束时间，纳秒 **/
        public long EndTimeN;

        /** 开始时的系统内存已用字节数 **/
        public long StartMemory_System;
        /** 结束时的系统内存已用字节数 **/
        public long EndMemory_System;

        /** 开始时的Java Runtime内存已用字节数 **/
        public long StartMemory_JavaRuntime;
        /** 结束时的Java Runtime内存已用字节数 **/
        public long EndMemory_JavaRuntime;
        /** 初始化使用的线程数量 **/
        public int UseThreadCount=1;

        /** 当前处理的文件行数，当全部处理完成时为0 **/
        public int CurrentLine_No;
        /** 当前处理的行完整内容，当全部处理完成时为空字符串 **/
        public String CurrentLine_Text="";
        /** 当前处理的行属性内容，当全部处理完成时为空字符串 **/
        public String CurrentLine_Prop="";

        /** 有效的图形数量，不包括空的图形 **/
        public int GeometryCount;
        /** 图形中的Polygon数量，一个图形包含1-n个Polygon，会用这些数量的Polygon外接矩形进行初步查找 **/
        public int PolygonCount;
        /** 如果缓存了wkb数据在内存，这里将会有wkb总字节数，未缓存为-1 **/
        public int WkbMemory=-1;

        /** 文件读取：文件内容读取耗时，纳秒 **/
        public long DurationN_FileRead;
        /** 文件解析：文件内容解析耗时，纳秒 **/
        public long DurationN_FileParse;
        /** 创建图形：内容解析成Geometry对象耗时，纳秒 **/
        public long DurationN_GeometryParse;
        /** Geometry对象进行索引耗时，纳秒 **/
        public long DurationN_Index;
        /** 初始化结尾调用System.gc()回收内存的耗时，纳秒 **/
        public long DurationN_JavaGC;

        /** 初始化时提供的数据文件路径 **/
        public String FilePath_Data;
        /** 初始化是提供的.wkbs后缀的结构化数据文件路径 **/
        public String FilePath_SaveWkbs;

        /**初始化时的数据是否是从wkbs结构化数据文件中读取；如果为false可能代表还未生成过wkbs文件，首次初始化可能会很慢**/
        public boolean DataFromWkbsFile;
        /**初始化时是否使用或保存了wkbs结构化数据文件，没有wkbs文件时查询中不允许获取WKT数据**/
        public boolean HasWkbsFile;
        /**初始化失败时的错误消息**/
        public String ErrMsg="";

        /** 初始化是否出现了错误 **/
        public boolean hasError() {
            return ErrMsg!=null && ErrMsg.length()>0;
        }
        @Override
        public String toString() {
            StringBuilder str=new StringBuilder();
            str.append("[v"+AreaCityQuery.Version+"]"
                    +(DataFromWkbsFile?"wkbs+":"")
                    +"已读取Geometry "+GeometryCount+" 个（Grid切分Polygon "+PolygonCount+" 个）");

            if(hasError()) {
                String errT="\n=============\n";
                str.append(errT+ErrMsg+errT);
            }

            str.append("\n");
            long tn=EndTimeN-StartTimeN;
            str.append("Init总耗时: "+tn/1000000+"ms");
            str.append("，平均: "+(GeometryCount==0?"-":Nano(tn*1.0/GeometryCount))+"/个Geometry，线程数: "+UseThreadCount);

            if(WkbMemory!=-1)str.append("\nWKB内存: "+Memory(WkbMemory));
            str.append("\n文件读取耗时: "+Nano(DurationN_FileRead));
            str.append("\n文件解析耗时: "+Nano(DurationN_FileParse/UseThreadCount)+"/线程，总: "+Nano(DurationN_FileParse));
            str.append("\n创建图形耗时: "+Nano(DurationN_GeometryParse/UseThreadCount)+"/线程，总: "+Nano(DurationN_GeometryParse));
            str.append("\n创建索引耗时: "+Nano(DurationN_Index/UseThreadCount)+"/线程，总: "+Nano(DurationN_Index));

            str.append("\n内存占用: "+Memory(EndMemory_JavaRuntime- StartMemory_JavaRuntime)+" (Java Runtime)");
            str.append(", "+Memory(EndMemory_System - StartMemory_System)+" (系统)");
            str.append(", Java GC耗时: "+Nano(DurationN_JavaGC));

            str.append("\nData文件: "+FilePath_Data);
            str.append("\nWkbs文件: "+FilePath_SaveWkbs);

            return str.toString();
        }
    }





    /** 查询控制+和结果信息对象 **/
    static public class QueryResult {
        /** 查询结果列表，为匹配的边界属性数据（prop json字符串）；如果设为null将只统计数据，不返回结果 **/
        public ArrayList<String> Result=new ArrayList<>();

        /** 查询开始时间，纳秒 **/
        public long StartTimeN;
        /** 查询结束时间，纳秒 **/
        public long EndTimeN;

        /** 查询过程中涉及到的IO耗时，纳秒 **/
        public long DurationN_IO;
        /** 查询过程中涉及到的图形对象解析耗时，纳秒 **/
        public long DurationN_GeometryParse;
        /** 从边界外接矩形中初步筛选耗时，纳秒 **/
        public long DurationN_EnvelopeHitQuery;
        /** 查询过程中精确查找耗时，纳秒 **/
        public long DurationN_ExactHitQuery;

        /** 外接矩形中初步筛选匹配到的矩形数量 **/
        public int EnvelopeHitCount;
        /** 精确查找到的边界数量 **/
        public int ExactHitCount;

        /** 本结果对象经过了几次查询（性能测试用） **/
        public int QueryCount;

        /** 将另一个的统计数据添加到这个里面来 **/
        public void Add(QueryResult other) {
            StartTimeN=Math.min(StartTimeN, other.StartTimeN);
            EndTimeN=Math.max(EndTimeN, other.EndTimeN);

            DurationN_IO+=other.DurationN_IO;
            DurationN_GeometryParse+=other.DurationN_GeometryParse;
            DurationN_EnvelopeHitQuery+=other.DurationN_EnvelopeHitQuery;
            DurationN_ExactHitQuery+=other.DurationN_ExactHitQuery;

            EnvelopeHitCount+=other.EnvelopeHitCount;
            ExactHitCount+=other.ExactHitCount;
            QueryCount+=other.QueryCount;
        }


        /** 如果不为null，几何计算查询时将会把从边界外接矩形中初步筛选时匹配到的中间结果写入到这个数组中（这些匹配项将参与精确匹配，数量越多性能越低下） **/
        public ArrayList<String> Set_EnvelopeHitResult=null;

        /** 查询结果中要额外包含对应的边界wkt文本，此参数会作为wkt文本在json里的key；必须初始化时保存了wkbs结构化数据文件，或者用的wkbs文件初始化的 **/
        public String Set_ReturnWKTKey=null;


        @Override
        public String toString() {
            StringBuilder str=new StringBuilder();
            long tn=EndTimeN-StartTimeN;
            str.append("查询"+QueryCount+"次共耗时: "+Nano(tn));
            str.append("，EnvelopeHitCount: "+EnvelopeHitCount);
            str.append("，ExactHitCount: "+ExactHitCount);
            str.append("，IO: "+Nano(DurationN_IO));
            str.append("，GeometryParse: "+Nano(DurationN_GeometryParse));
            str.append("，EnvelopeHitQuery: "+Nano(DurationN_EnvelopeHitQuery));
            str.append("，ExactHitQuery: "+Nano(DurationN_ExactHitQuery));

            if(QueryCount>1) {
                double count=QueryCount;
                str.append("\n单次查询耗时: "+Nano(tn/count));
                str.append("，EnvelopeHitCount: "+Math.round(EnvelopeHitCount*100/count)/100.0);
                str.append("，ExactHitCount: "+Math.round(ExactHitCount*100/count)/100.0);
                str.append("，IO: "+Nano(DurationN_IO/count));
                str.append("，GeometryParse: "+Nano(DurationN_GeometryParse/count));
                str.append("，EnvelopeHitQuery: "+Nano(DurationN_EnvelopeHitQuery/count));
                str.append("，ExactHitQuery: "+Nano(DurationN_ExactHitQuery/count));
            }

            if(Set_EnvelopeHitResult!=null) {
                str.append("\n\nEnvelopeHit初步筛选: "+Set_EnvelopeHitResult.size()+"条");
                for(int i=0;i<Set_EnvelopeHitResult.size();i++) {
                    String txt=Set_EnvelopeHitResult.get(i);
                    str.append("\nHit["+i+"] "+(txt.length()<500?txt:txt.substring(0, 500)+" ... "+txt.length()+"字"));
                }
            }
            if(Result!=null) {
                str.append("\n\n结果 Result: "+Result.size()+"条");
                for(int i=0;i<Result.size();i++) {
                    String txt=Result.get(i);
                    str.append("\n结果["+i+"] "+(txt.length()<500?txt:txt.substring(0, 500)+" ... "+txt.length()+"字"));
                }
            }
            return str.toString();
        }
    }

}
