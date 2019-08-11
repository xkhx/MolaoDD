import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * @author D_xiaox(星空)
 * @since 2019/8/9
 */
public class Test {

    private static void saveConfig(String fileName, String data) {
        File file = new File(fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            bufferedWriter.write(data);
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        String fileName = "C:\\Users\\Administrator\\Desktop\\test\\config.json";
        FileReader fileReader = new FileReader(fileName);
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        StringBuilder stringBuilder = new StringBuilder();
        String line = bufferedReader.readLine();
        while (line != null) {
            stringBuilder.append(line);
            line = bufferedReader.readLine();
        }
        JSONObject jsonObject = JSONObject.parseObject(stringBuilder.toString());
        System.out.println(jsonObject.getString("test"));
        /*
        JSONObject jsonObject = new JSONObject();
        List<Long> admin = Arrays.asList(1582952890L, 390807154L, 1838115958L);
        jsonObject.put("admin", admin);
        List<Long> test = jsonObject.getJSONArray("admin").toJavaList(Long.class);
        System.out.println(test.size());
        jsonObject.put("test", 1);
        System.out.println(jsonObject.toJSONString());
        jsonObject.put("test", 2);
        System.out.println(jsonObject.toJSONString());

        saveConfig(fileName, jsonObject.toJSONString());

         */
    }
}
