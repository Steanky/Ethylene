package com.github.steanky.ethylene.example;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.bridge.ConfigBridge;
import com.github.steanky.ethylene.core.collection.ArrayConfigList;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import com.github.steanky.ethylene.codec.json.JsonCodec;
import com.github.steanky.ethylene.codec.toml.ConfigDate;
import com.github.steanky.ethylene.codec.toml.TomlCodec;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This class contains contrived examples to show how Ethylene can be used.
 */
public class Main {
    /**
     * Some made-up JSON data.
     */
    public static final String JSON_STRING = "{ \"this is\" : \"some json\", \"oh look here is\" : " +
            "{ \"some more\" : \"json\" }, \"and here is a number\" : 69, \"array\" : [ \"first\", \"second\" ] }";

    /**
     * Some made-up TOML data.
     */
    public static final String TOML_STRING = "string = \"toml string\"\ndate = 1979-05-27T07:32:00-08:00";

    /**
     * Entrypoint for contrived example program.
     * @param args program arguments, unused
     * @throws IOException if the JSON data is invalid
     */
    public static void main(String[] args) throws IOException {
        json();
        toml();

        //ConfigNode objects are also Java maps. this one acts like a LinkedHashMap (yes, it's also mutable)
        Map<String, ConfigElement> configMap = new LinkedConfigNode();

        //ConfigList objects are Java lists too. this one is backed by an ArrayList
        List<ConfigElement> configList = new ArrayConfigList();
    }

    /**
     * Demonstrates how you can read simple values from a JSON string.
     * @throws IOException if the JSON data we're reading is invalid
     */
    public static void json() throws IOException {
        //interprets JSON_STRING as json
        ConfigNode node = ConfigBridge.read(JSON_STRING, JsonCodec.INSTANCE);

        /*
        everything below here works exactly the same regardless of what kind of file format you're using!
        */

        //prints "some json" without quotes
        System.out.println(node.get("this is").asString());

        //get the child node
        ConfigNode more = node.get("oh look here is").asNode();

        //prints "json" without quotes
        System.out.println(more.get("some more").asString());

        //you can also use getElement access nested elements more conveniently
        //prints "json" without quotes
        System.out.println(node.getElement("oh look here is", "some more").asString());

        //prints "69" without quotes. nice.
        System.out.println(node.get("and here is a number").asNumber().intValue());

        //prints out "first" and then "second"
        for(ConfigElement element : node.get("array").asList()) {
            System.out.println(element.asString());
        }
    }

    /**
     * Reads some values from a TOML string and prints them.
     * @throws IOException if the TOML data is invalid
     */
    public static void toml() throws IOException {
        //interprets TOML_STRING as toml
        ConfigNode node = ConfigBridge.read(TOML_STRING, TomlCodec.INSTANCE);

        //prints "toml string" without quotes
        System.out.println(node.get("string").asString());

        //TOML has first-class support for dates, and Ethylene can take advantage of that too
        //this functionality (ConfigDate) is specific to the ethylene-toml module
        //the output of this is locale-dependent
        System.out.println(((ConfigDate)node.get("date")).getDate());

        //you can also call asObject and cast, as long as you know what the type is
        //this will print the same as the previous example
        Date date = (Date)node.get("date").asObject();
        System.out.println(date);
    }
}
