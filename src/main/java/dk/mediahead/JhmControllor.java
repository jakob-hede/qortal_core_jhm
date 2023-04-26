package dk.mediahead;

import com.github.underscore.U;
//import com.github.underscore.Json;
//import com.github.underscore.Json.JsonObject;

//import org.qortal.controller.Controller;

import org.qortal.settings.Settings;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

public class JhmControllor {
//    public class JhmControllor extends Controller {

    //        default constructor:
    private JhmControllor() {
        System.out.println("JhmControllor constructor called");
    }

    //    default constructor:
//    private JhmControllor(String[] args) {
//////        super(args);
//////        super();
////        System.out.println("Java running JhmControllor");
////
////        super();


    public static void main(String[] args) {
        System.out.println("Java running JhmControllor");
        JhmControllor obj = new JhmControllor();
        obj.settingfy();
        //            obj.habitize();
        //            obj.construct();
        //            obj.readYamlFile();
        //            obj.writeJsonFile();
    }

    private void settingfy() {
        System.out.println("called settingfy");
        String settingsFileJsonNada = "/opt/projects/qortal_top/qortal_core_jhm/jhm/data/settings/settings.nada.json";
        String settingsFileJsonDefault = settingsFileJsonNada.replace("nada", "default");
        String settingsFileYamlDefault = settingsFileJsonDefault.replace(".json", ".yaml");
        boolean doChain = false;
        Settings.fileInstance(settingsFileJsonNada, doChain);
        Settings instance = Settings.getInstance();
//        String fields = instance.getFieldsString();
//        System.out.println("fields: " + fields);

        //        instantiate JsonObject:
//        JsonObject jsonObject = new JsonObject();
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> invalidsData = new HashMap<>();
//        invalidsData.put("dummyKey", 999);


        Field[] fields = instance.getDeclaredFieldsList();
//        Method[] methods = instance.getDeclaredMethodsList();
//        CharSequence[] sep = new String[]{", "};
        String[] stringArray = null;
//        List<String> stringList = null;
        String valueStr = null;
        for (Field field : fields) {
//            System.out.println("- field: " + field);
            valueStr = null;
            String name = field.getName();
            if (name.equals("instance")) {
                continue;
            }
            String nameTitle = name.substring(0, 1).toUpperCase() + name.substring(1);
            String accessorName = "get" + nameTitle;
            Object value = null;
            boolean found = true;
            try {
                Method method = instance.getClass().getMethod(accessorName);
                value = method.invoke(instance);
            } catch (Exception e) {
                found = false;
            }
            if (found) {
                if (value == null) {
                    System.out.format("- field: %s %s%n", name, accessorName);
                } else {
                    Type fieldType = field.getType();
                    if (value instanceof String[]) {
                        stringArray = (String[]) value;
                        valueStr = String.join(", ", stringArray);
                    } else {
                        valueStr = value.toString();
                    }
//                    if (name.equals("bitcoinNet")) {
//                        int noop = 0;
//                    }

                    System.out.format("- field: %s %s <%s> '%s'%n", name, accessorName, fieldType, valueStr);
                }
                if (value instanceof Enum<?>) {
                    int noop = 0;
//                    valueStr =  "<enum> MAIN";
                    invalidsData.put(name, "<enum> " + valueStr);
//                    invalidsData.put(name, "enum");
                    continue;
//                    value = valueStr;
                }
                data.put(name, value);
            }
        }

        //YAML:
        Yaml yaml = new Yaml();
        String yaml_str =  yaml.dumpAsMap(data);
        String invalids_yaml_str = "Invalids:\n" + yaml.dumpAsMap(invalidsData);
//        insert # at start of every line in invalids_yaml_str:
        invalids_yaml_str = invalids_yaml_str.replaceAll("(?m)^", "# ");

        yaml_str += "\n" +  invalids_yaml_str;

//        write to file
        try {
            FileWriter file = new FileWriter(settingsFileYamlDefault);
            file.write(yaml_str);
            file.flush();
            file.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//JSON:
        String json_str = U.toJson(data);
        String invalids_json_str = U.toJson(invalidsData);
//        insert # at start of every line in invalids_json_str:
        invalids_json_str = invalids_json_str.replaceAll("(?m)^", "# ");
        json_str += "\n" + invalids_json_str;
        System.out.println(json_str);
//        write to file
        try {
            FileWriter file = new FileWriter(settingsFileJsonDefault);
            file.write(json_str);
            file.flush();
            file.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        int noop = 0;

    }
}
