package com.github.steanky.ethylene.example;

import com.github.steanky.ethylene.bridge.ConfigBridge;
import com.github.steanky.ethylene.collection.ConfigNode;
import com.github.steanky.ethylene.json.JsonCodec;

import java.io.IOException;

public class Main {
    public static final String JSON_STRING = "{ \"this is\" : \"some json\", \"oh look here is\" : " +
            "{ \"some more\" : \"json\" } }";

    public static void main(String[] args) throws IOException {
        simpleExample();
    }

    public static void simpleExample() throws IOException {
        //interprets JSON_STRING as json
        ConfigNode node = ConfigBridge.read(JSON_STRING, JsonCodec.INSTANCE);

        //prints "some json" without quotes
        System.out.println(node.get("this is").asString());

        //get the child node
        ConfigNode more = node.get("oh look here is").asNode();

        //prints "json" without quotes
        System.out.println(more.get("some more").asString());

        //you can also use paths to access nested elements!
        //prints "json" without quotes
        System.out.println(node.getElement("oh look here is", "some more").asString());
    }
}
