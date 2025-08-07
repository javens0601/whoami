package com.javen.common.whoami.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.tomcat.util.json.JSONParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Description
 * @Author: javen
 * @CreateTime: 2025/7/21 18:59
 */
public class FieService
{
    public static void main(String[] args) throws Exception
    {

        Map<String, Set<String>> flsMap = new HashMap<>();
        Map<String, Set<String>> flsMap2 = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();

        //Path path = Path.of("/Users/javen/Desktop/flslog");
        Path path = Path.of("/Users/javen/Desktop/fldown1.log");

        List<String> lines = Files.readAllLines(path);
        for (String line : lines)
        {
            int start = line.indexOf("[");
            int last = line.lastIndexOf("]");

            String substring = line.substring(start + 1, last);
            //System.out.println(substring);
            int i = substring.lastIndexOf("}");
            String json = substring.substring(0, i+1);
            String[] split = substring.split(",");
            JsonNode jsonNode = mapper.readTree(json);

            ArrayNode taskList = jsonNode.withArrayProperty("taskList");
            String resCode = split[split.length - 1];

            for (JsonNode node : taskList)
            {
                String text = node.get("channelCode").asText();
                //System.out.println(text);
                //System.out.println(resCode);

                if (flsMap.containsKey(text)) {
                    Set<String> strings = flsMap.get(text);
                    strings.add(resCode);
                } else {
                    HashSet<String> hashSet = new HashSet<>();
                    hashSet.add(resCode);
                    flsMap.put(text, hashSet);
                }

                if (flsMap2.containsKey(resCode)) {
                    Set<String> strings = flsMap2.get(resCode);
                    strings.add(text);
                } else {
                    HashSet<String> hashSet = new HashSet<>();
                    hashSet.add(text);
                    flsMap2.put(resCode, hashSet);
                }


            }
        }
        flsMap.forEach((k,v) ->
                System.out.println(k + "\t" + v)
        );

        System.out.println("====================== ");

        flsMap2.forEach((k,v) ->
                System.out.println(k + "\t" + v)
        );

    }
}
