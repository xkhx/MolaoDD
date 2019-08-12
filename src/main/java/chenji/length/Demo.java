package chenji.length;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.sobte.cqp.jcq.entity.CQDebug;
import com.sobte.cqp.jcq.entity.ICQVer;
import com.sobte.cqp.jcq.entity.IMsg;
import com.sobte.cqp.jcq.entity.IRequest;
import com.sobte.cqp.jcq.event.JcqAppAbstract;

import javax.swing.*;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static chenji.length.OperatorHelper.*;

/**
 * 本文件是JCQ插件的主类<br>
 * <br>
 * <p>
 * 注意修改json中的class来加载主类，如不设置则利用appid加载，最后一个单词自动大写查找<br>
 * 例：appid(com.example.demo) 则加载类 com.example.Demo<br>
 * 文档地址： https://gitee.com/Sobte/JCQ-CoolQ <br>
 * 帖子：https://cqp.cc/t/37318 <br>
 * 辅助开发变量: {@link JcqAppAbstract#CQ CQ}({@link com.sobte.cqp.jcq.entity.CoolQ
 * 酷Q核心操作类}), {@link JcqAppAbstract#CC
 * CC}({@link com.sobte.cqp.jcq.entity 酷Q码操作类}), 具体功能可以查看文档
 */
public class Demo extends JcqAppAbstract implements ICQVer, IMsg, IRequest {
    //private boolean chengeCard = false;
    //private double addLength = 0.0D;
    private String appDirectory;
    private HashMap<Long, Long> coolDown;
    private HashMap<Long, Integer> ban;
    private LinkedHashMap<Long, Double> cutDDMap;
    private LinkedHashMap<Long, Double> addDDMap;
    private LinkedHashMap<Long, Double> totalDDMap = new LinkedHashMap<>();
    //private boolean enableDrag;
    private Set<Long> admin;
    private JSONObject config;
    private Random random = new Random();

    /**
     * 用main方法调试可以最大化的加快开发效率，检测和定位错误位置<br/>
     * 以下就是使用Main方法进行测试的一个简易案例
     *
     * @param args 系统参数
     */
    public static void main(String[] args) {
        // CQ此变量为特殊变量，在JCQ启动时实例化赋值给每个插件，而在测试中可以用CQDebug类来代替他
        CQ = new CQDebug("run", "chenji.length.demo");// new CQDebug("应用目录","应用名称") 可以用此构造器初始化应用的目录
        CQ.logInfo("[JCQ] TEST Demo", "测试启动");// 现在就可以用CQ变量来执行任何想要的操作了
        // 要测试主类就先实例化一个主类对象
        Demo demo = new Demo();
        // 下面对主类进行各方法测试,按照JCQ运行过程，模拟实际情况
        demo.startup();// 程序运行开始 调用应用初始化方法
        demo.enable();// 程序初始化完成后，启用应用，让应用正常工作
        demo.ban.clear();
        demo.coolDown.clear();
        for (int i = 0; i < 5; i++) {
            demo.groupMsg(1, i, i, i, "false", "/拽长排行", 56);
            demo.groupMsg(1, i, i, i, "false", "/拽断排行", 56);
            demo.groupMsg(1, i, i, i, "false", "/贡献排行", 56);
        }
        demo.groupMsg(1, 233, 233, 1294790523, "false", "/cut 2200", 56);
        System.out.println(demo.getOriginalLength());
        System.out.println(demo.getCurrentLength());
        demo.disable();

        demo.exit();// 最后程序运行结束，调用exit方法

        //System.out.println(233);
    }

    /**
     * 打包后将不会调用 请不要在此事件中写其他代码
     *
     * @return 返回应用的ApiVer、Appid
     */
    public String appInfo() {
        // 应用AppID,规则见 http://d.cqp.me/Pro/开发/基础信息#appid
        String AppID = "chenji.length.demo";// 记住编译后的文件和json也要使用appid做文件名
        /*
         * 本函数【禁止】处理其他任何代码，以免发生异常情况。 如需执行初始化代码请在 startup 事件中执行（Type=1001）。
         */
        return CQAPIVER + "," + AppID;
    }

    public static Comparator<Double> reserved(Comparator<Double> comparator) {
        return comparator.reversed();
    }

    /**
     * 酷Q启动 (Type=1001)<br>
     * 本方法会在酷Q【主线程】中被调用。<br>
     * 请在这里执行插件初始化代码。<br>
     * 请务必尽快返回本子程序，否则会卡住其他插件以及主程序的加载。
     *
     * @return 请固定返回0
     */

    public int startup() {
        //Arrays.asList(1582952890L, 390807154L, 1838115958L);
        // 获取应用数据目录(无需储存数据时，请将此行注释)
        appDirectory = CQ.getAppDirectory();
        // 返回如：D:\CoolQ\app\com.sobte.cqp.jcq\app\com.example.demo\
        // 应用的所有数据、配置【必须】存放于此目录，避免给用户带来困扰。
        long time = System.currentTimeMillis();
        initJSON();
        loadJSON();
        updateTotalMap();
        System.out.println("Time:" + (System.currentTimeMillis() - time) + "ms");
        return 0;
    }

    void initJSON() {
        String configJson = getConfig(Paths.get(appDirectory, "config.json"));
        config = JSONObject.parseObject(configJson);
        config.putIfAbsent("addLength", 0.0D);
        config.putIfAbsent("enableDrag", false);
        config.putIfAbsent("chengeCard", false);
        config.putIfAbsent("admin", "[]");
        config.putIfAbsent("coolDown", new HashMap<Long, Long>());
        config.putIfAbsent("ban", new HashMap<Long, Integer>());
        config.putIfAbsent("cutDDMap", new LinkedHashMap<Long, Double>());
        config.putIfAbsent("addDDMap", new LinkedHashMap<Long, Double>());
    }

    void loadJSON() {
        admin = new HashSet<>(config.getJSONArray("admin").toJavaList(Long.class));
        coolDown = config.getJSONObject("coolDown").toJavaObject(new TypeReference<HashMap<Long, Long>>() {
        }.getType());
        ban = config.getJSONObject("coolDown").toJavaObject(new TypeReference<HashMap<Long, Integer>>() {
        }.getType());
        cutDDMap = config.getJSONObject("cutDDMap").toJavaObject(new TypeReference<LinkedHashMap<Long, Double>>() {
        }.getType());
        addDDMap = config.getJSONObject("addDDMap").toJavaObject(new TypeReference<LinkedHashMap<Long, Double>>() {
        }.getType());
    }

    /**
     * 酷Q退出 (Type=1002)<br>
     * 本方法会在酷Q【主线程】中被调用。<br>
     * 无论本应用是否被启用，本函数都会在酷Q退出前执行一次，请在这里执行插件关闭代码。
     *
     * @return 请固定返回0，返回后酷Q将很快关闭，请不要再通过线程等方式执行其他代码。
     */
    public int exit() {
        return 0;
    }

    /**
     * 应用已被启用 (Type=1003)<br>
     * 当应用被启用后，将收到此事件。<br>
     * 如果酷Q载入时应用已被启用，则在 {@link #startup startup}(Type=1001,酷Q启动)
     * 被调用后，本函数也将被调用一次。<br>
     * 如非必要，不建议在这里加载窗口。
     *
     * @return 请固定返回0。
     */
    public int enable() {
        enable = true;
        return 0;
    }

    void updateTotalMap() {
        totalDDMap.clear();
        totalDDMap.putAll(cutDDMap);
        addDDMap.forEach((k, v) -> totalDDMap.merge(k, v, OperatorHelper::add));
    }

    /**
     * 私聊消息 (Type=21)<br>
     * 本方法会在酷Q【线程】中被调用。<br>
     *
     * @param subType 子类型，11/来自好友 1/来自在线状态 2/来自群 3/来自讨论组
     * @param msgId   消息ID
     * @param fromQQ  来源QQ
     * @param msg     消息内容
     * @param font    字体
     * @return 返回值*不能*直接返回文本 如果要回复消息，请调用api发送<br>
     * 这里 返回 {@link IMsg#MSG_INTERCEPT MSG_INTERCEPT} - 截断本条消息，不再继续处理<br>
     * 注意：应用优先级设置为"最高"(10000)时，不得使用本返回值<br>
     * 如果不回复消息，交由之后的应用/过滤器处理，这里 返回 {@link IMsg#MSG_IGNORE MSG_IGNORE} -
     * 忽略本条消息
     */
    public int privateMsg(int subType, int msgId, long fromQQ, String msg, int font) {
        return MSG_IGNORE;
    }

    private void saveConfig(Path path, String data) {
        try {
            BufferedWriter bufferedWriter = Files.newBufferedWriter(path);
            bufferedWriter.write(data);
            bufferedWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getConfig(Path file) {
        String result = "";
        try {
            if (!Files.exists(file.getParent())) {
                Files.createDirectory(file.getParent());
            }
            if (!Files.exists(file)) {
                Files.createFile(file);
            }
            result = Files.lines(file).collect(Collectors.joining());
        } catch (Exception ex) {
            System.out.println("读取配置时出现错误");
            ex.printStackTrace();
        }
        return result.equals("") ? "{}" : result;
    }

    /***
     * 检测并将参数解析为double
     * @param args 要解析的参数
     * @param help 帮助信息
     * @param fromGroup 群号
     * @return 解析后的double，如果参数为空或解析出错发送错误信息并返回NaN
     */
    public double checkArgAndParseDouble(String[] args, String help, long fromGroup) {
        try {
            return Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            CQ.sendGroupMsg(fromGroup, "沉寂觉得你输入的不是一个数字呢，请你立即女装呢");
        } catch (NullPointerException e) {
            CQ.sendGroupMsg(fromGroup, "沉寂觉得你的参数写错了呢，请你立即女装(" + help + ")呢");
        }
        return Double.NaN;
    }

    public void changeValue(String configKey, double value, BiFunction<Double, Double, Double> operator) {
        config.put(configKey, operator.apply(config.getDouble(configKey), value));
    }

    public void toggleAndSendMsg(String feature, String configKey, long group) {
        boolean bool = config.getBoolean(configKey);
        config.put(configKey, !bool);
        String toggleName = bool ? "关闭" : "开启";
        CQ.sendGroupMsg(group, "沉寂" + toggleName + "了" + feature + "的功能");
    }

    public boolean ifNotSendMsg(long group, boolean bool) {
        if (!bool) {
            CQ.sendGroupMsg(group, "沉寂觉得你没权限来这样做呢，请你立即女装呢");
        }
        return bool;
    }

    /**
     * 应用将被停用 (Type=1004)<br>
     * 当应用被停用前，将收到此事件。<br>
     * 如果酷Q载入时应用已被停用，则本函数【不会】被调用。<br>
     * 无论本应用是否被启用，酷Q关闭前本函数都【不会】被调用。
     *
     * @return 请固定返回0。
     */
    public int disable() {
        enable = false;
        config.put("admin", JSON.toJSON(admin));
        config.put("coolDown", JSON.toJSON(coolDown));
        config.put("ban", JSON.toJSON(ban));
        config.put("cutDDMap", JSON.toJSON(cutDDMap));
        config.put("addDDMap", JSON.toJSON(addDDMap));
        saveConfig(Paths.get(appDirectory, "config.json"), config.toJSONString());
        return 0;
    }

    /**
     * 群消息 (Type=2)<br>
     * 本方法会在酷Q【线程】中被调用。<br>
     *
     * @param subType       子类型，目前固定为1
     * @param msgId         消息ID
     * @param fromGroup     来源群号
     * @param fromQQ        来源QQ号
     * @param fromAnonymous 来源匿名者
     * @param msg           消息内容
     * @param font          字体
     * @return 关于返回值说明, 见 {@link #privateMsg 私聊消息} 的方法
     */
    public int groupMsg(int subType, int msgId, long fromGroup, long fromQQ, String fromAnonymous, String msg,
                        int font) {
        //名片同步
        if (config.getBoolean("chengeCard")) {
            CQ.setGroupCard(fromGroup, 1582952890L, "莫老现在竟然有" + (getOriginalLength() + config.getDouble("addLength")) + "纳米");
        }

        if (msg.startsWith("/")) {
            String[] command = msg.split(" ");
            String root = command[0].substring(1);
            String[] args = command.length > 1 ? Arrays.copyOfRange(command, 1, command.length) : null;
            switch (root.toLowerCase()) {
                case "drag":
                    if (config.getBoolean("enableDrag")) {
                        drag(fromGroup, fromQQ);
                    }
                    return MSG_IGNORE;
                case "length":
                    sendLengthMsg(fromGroup, fromQQ);
                    return MSG_IGNORE;
                case "拽长排行":
                    CQ.sendGroupMsg(fromGroup, CC.at(fromQQ) + "\n拽长前十排行：\n" + addDDMap.entrySet().stream().sorted(Map.Entry.comparingByValue(reserved(Double::compare))).limit(10).map(entry -> "QQ：" + entry.getKey() + "|贡献长度：" + entry.getValue()).collect(Collectors.joining("\n")));
                    return MSG_IGNORE;
                case "拽断排行":
                    CQ.sendGroupMsg(fromGroup, CC.at(fromQQ) + "\n拽断前十排行：\n" + totalDDMap.entrySet().stream().sorted(Map.Entry.comparingByValue(Double::compare)).limit(10).map(entry -> "QQ：" + entry.getKey() + "|贡献长度：" + entry.getValue()).collect(Collectors.joining("\n")));
                    return MSG_IGNORE;
                case "贡献排行":
                    updateTotalMap();
                    CQ.sendGroupMsg(fromGroup, CC.at(fromQQ) + "\n贡献前十排行：\n" + totalDDMap.entrySet().stream().sorted(Map.Entry.comparingByValue(reserved(Double::compare))).limit(10).map(entry -> "QQ：" + entry.getKey() + "|贡献长度：" + entry.getValue()).collect(Collectors.joining("\n")));
                    return MSG_IGNORE;
                case "togglechengecardmode":
                    if (ifNotSendMsg(fromGroup, 1582952890L == fromQQ)) {
                        toggleAndSendMsg("自己群名片同步莫老长度", "chengeCard", fromGroup);
                        //名片同步
                        if (config.getBoolean("chengeCard")) {
                            CQ.setGroupCard(fromGroup, 1582952890L, "莫老现在竟然有" + (getOriginalLength() + config.getDouble("addLength")) + "纳米");
                        }
                    }
                    return MSG_IGNORE;
                case "toggleenabledragmode":
                    if (ifNotSendMsg(fromGroup, admin.contains(fromQQ))) {
                        toggleAndSendMsg("拽莫老", "enableDrag", fromGroup);
                    }
                    return MSG_IGNORE;
                case "cleardrag":
                    if (ifNotSendMsg(fromGroup, admin.contains(fromQQ))) {
                        CQ.sendGroupMsg(fromGroup, CC.at(fromQQ) + "清空了被揪长的部分");
                        config.put("addLength", 0.0D);
                    }
                    return MSG_IGNORE;
                case "cut":
                    if (ifNotSendMsg(fromGroup, admin.contains(fromQQ))) {
                        double cut = checkArgAndParseDouble(args, "输入要切断的数字", fromGroup);
                        if (!Double.isNaN(cut)) {
                            //防止addLength为负数
                            cut = subtract(config.getDouble("addLength"), cut) < 0 ? config.getDouble("addLength") : cut;
                            changeValue("addLength", cut, OperatorHelper::subtract);
                            CQ.sendGroupMsg(fromGroup, CC.at(fromQQ) + "[切丁丁] oh! 你切掉了莫老的丁丁" + cut + "纳米");
                        }
                    }
                    return MSG_IGNORE;
                case "admindrag":
                    if (ifNotSendMsg(fromGroup, admin.contains(fromQQ))) {
                        double drag = checkArgAndParseDouble(args, "输入要拉长数字", fromGroup);
                        if (!Double.isNaN(drag)) {
                            changeValue("addLength", drag, OperatorHelper::add);
                            CQ.sendGroupMsg(fromGroup, CC.at(fromQQ) + "[拽丁丁]你动用了管理员权限拽长了莫老丁丁" + drag + "纳米");
                        }
                    }
                    return MSG_IGNORE;
                default:
                    return MSG_IGNORE;
            }
        } else {
            //语义分析命令
            boolean dragKeywords = msg.contains("拽") || msg.contains("拉") || msg.contains("拔");
            if ((msg.contains("莫老") && msg.contains("丁丁") && msg.contains("长")) && !dragKeywords) {
                sendLengthMsg(fromGroup, fromQQ);
                return MSG_IGNORE;
            }
            if (config.getBoolean("enableDrag")) {
                if ((msg.contains("莫老") && msg.contains("丁丁")) && dragKeywords) {
                    drag(fromGroup, fromQQ);
                    return MSG_IGNORE;
                }
                if (msg.contains("莫老") && msg.contains("丁丁") && (msg.contains("算"))) {
                    CQ.sendGroupMsg(fromGroup, CC.at(fromQQ)
                            + "莫老丁丁的计算公式：(20.0D + ((System.currentTimeMillis() - 1564934400000L) / 864000) * 0.0056D)+被拽的长度");
                }
            }
            return MSG_IGNORE;
        }
    }

    private void sendLengthMsg(long fromGroup, long fromQQ) {
        CQ.sendGroupMsg(fromGroup, CC.at(fromQQ) + "莫老丁丁长度：" + (getOriginalLength() + config.getDouble("addLength")) + " 纳米，生长状况良好，其中有" + config.getDouble("addLength") + "是被众人揪起来的");
    }

    private void drag(long fromGroup, long fromQQ) {
        if (isCooling(fromQQ, fromGroup)) {
            double length = random.nextDouble() * 10d;
            if (length == 0.0d) {
                CQ.sendGroupMsg(fromGroup, CC.at(fromQQ) + "[拽丁丁]震惊，竟然没拽动莫老的丁丁，现在有"
                        + getCurrentLength() + "纳米  " + getDisplay(getCurrentLength()));
                return;
            }
            if (fromQQ == 602723113L || random.nextInt(100) < 10) {
                length = -length * 10 - 5;
            }
            //保留两位小数并确保最小值为±0.01
            length = length > 0 ? Math.max(toTwoPrecision(length), 0.01) : Math.max(toTwoPrecision(length), -0.01);
            //防止addLength为负数
            length = add(length, config.getDouble("addLength")) < 0 ? -config.getDouble("addLength") : length;
            changeValue("addLength", length, OperatorHelper::add);
            if (length < 0) {
                cutDDMap.merge(fromQQ, length, OperatorHelper::add);
                CQ.sendGroupMsg(fromGroup, CC.at(fromQQ) + "[拽丁丁]震惊，竟然拽断了" + Math.abs(length) + "纳米，现在有"
                        + getCurrentLength() + "纳米 \n" + getDisplay(getCurrentLength()));
            } else {
                addDDMap.merge(fromQQ, length, OperatorHelper::add);
                CQ.sendGroupMsg(fromGroup, CC.at(fromQQ) + "[拽丁丁]拽长了" + length + "纳米，现在有"
                        + getCurrentLength() + "纳米 \n" + getDisplay(getCurrentLength()));
            }
        }
    }

    private double getCurrentLength() {
        return add(getOriginalLength(), config.getDouble("addLength"));
    }

    private double getOriginalLength() {
        //(20.0D + ((System.currentTimeMillis() - 1564934400000L) / 864000) * 0.0056D)
        return add(20.0D, multiply(divide(System.currentTimeMillis() - 1564934400000L, 864000L), 0.0056D));
    }

    private String getDisplay(double l) {
        StringBuilder sb = new StringBuilder("8");
        while (l >= 1) {
            sb.append('=');
            l -= 10d;
        }
        sb.append('>');
        return sb.toString();
    }

    private void logMute(Long qq, Long fromGroup) {
        if (this.ban.containsKey(qq)) {
            int amount = this.ban.get(qq);
            amount++;
            this.ban.put(qq, amount);
            if (amount >= 5) {
                CQ.setGroupBan(fromGroup, qq, 240);
                CQ.sendPrivateMsg(qq, "你因为频繁拽莫老被禁言了");
            }
        } else {
            this.ban.put(qq, 1);
        }
    }

    private boolean isCooling(Long qq, Long gr) {
        if (this.coolDown.containsKey(qq)) {
            if (System.currentTimeMillis() - coolDown.get(qq) <= 60000) {
                logMute(qq, gr);
                CQ.sendPrivateMsg(qq, "一分钟内只能拽一次！若违规超过5次会被禁言4分钟");
                return false;
            }
        }
        this.ban.remove(qq);
        this.coolDown.put(qq, System.currentTimeMillis());
        return true;
    }

    /**
     * 讨论组消息 (Type=4)<br>
     * 本方法会在酷Q【线程】中被调用。<br>
     *
     * @param subtype     子类型，目前固定为1
     * @param msgId       消息ID
     * @param fromDiscuss 来源讨论组
     * @param fromQQ      来源QQ号
     * @param msg         消息内容
     * @param font        字体
     * @return 关于返回值说明, 见 {@link #privateMsg 私聊消息} 的方法
     */
    public int discussMsg(int subtype, int msgId, long fromDiscuss, long fromQQ, String msg, int font) {

        return MSG_IGNORE;
    }

    /**
     * 群文件上传事件 (Type=11)<br>
     * 本方法会在酷Q【线程】中被调用。<br>
     *
     * @param subType   子类型，目前固定为1
     * @param sendTime  发送时间(时间戳)// 10位时间戳
     * @param fromGroup 来源群号
     * @param fromQQ    来源QQ号
     * @param file      上传文件信息
     * @return 关于返回值说明, 见 {@link #privateMsg 私聊消息} 的方法
     */
    public int groupUpload(int subType, int sendTime, long fromGroup, long fromQQ, String file) {
        return MSG_IGNORE;
    }

    /**
     * 群事件-管理员变动 (Type=101)<br>
     * 本方法会在酷Q【线程】中被调用。<br>
     *
     * @param subtype        子类型，1/被取消管理员 2/被设置管理员
     * @param sendTime       发送时间(时间戳)
     * @param fromGroup      来源群号
     * @param beingOperateQQ 被操作QQ
     * @return 关于返回值说明, 见 {@link #privateMsg 私聊消息} 的方法
     */
    public int groupAdmin(int subtype, int sendTime, long fromGroup, long beingOperateQQ) {
        // 这里处理消息

        return MSG_IGNORE;
    }

    /**
     * 群事件-群成员减少 (Type=102)<br>
     * 本方法会在酷Q【线程】中被调用。<br>
     *
     * @param subtype        子类型，1/群员离开 2/群员被踢
     * @param sendTime       发送时间(时间戳)
     * @param fromGroup      来源群号
     * @param fromQQ         操作者QQ(仅子类型为2时存在)
     * @param beingOperateQQ 被操作QQ
     * @return 关于返回值说明, 见 {@link #privateMsg 私聊消息} 的方法
     */
    public int groupMemberDecrease(int subtype, int sendTime, long fromGroup, long fromQQ, long beingOperateQQ) {
        // 这里处理消息

        return MSG_IGNORE;
    }

    /**
     * 群事件-群成员增加 (Type=103)<br>
     * 本方法会在酷Q【线程】中被调用。<br>
     *
     * @param subtype        子类型，1/管理员已同意 2/管理员邀请
     * @param sendTime       发送时间(时间戳)
     * @param fromGroup      来源群号
     * @param fromQQ         操作者QQ(即管理员QQ)
     * @param beingOperateQQ 被操作QQ(即加群的QQ)
     * @return 关于返回值说明, 见 {@link #privateMsg 私聊消息} 的方法
     */
    public int groupMemberIncrease(int subtype, int sendTime, long fromGroup, long fromQQ, long beingOperateQQ) {
        // 这里处理消息

        return MSG_IGNORE;
    }

    /**
     * 好友事件-好友已添加 (Type=201)<br>
     * 本方法会在酷Q【线程】中被调用。<br>
     *
     * @param subtype  子类型，目前固定为1
     * @param sendTime 发送时间(时间戳)
     * @param fromQQ   来源QQ
     * @return 关于返回值说明, 见 {@link #privateMsg 私聊消息} 的方法
     */
    public int friendAdd(int subtype, int sendTime, long fromQQ) {
        // 这里处理消息

        return MSG_IGNORE;
    }

    /**
     * 请求-好友添加 (Type=301)<br>
     * 本方法会在酷Q【线程】中被调用。<br>
     *
     * @param subtype      子类型，目前固定为1
     * @param sendTime     发送时间(时间戳)
     * @param fromQQ       来源QQ
     * @param msg          附言
     * @param responseFlag 反馈标识(处理请求用)
     * @return 关于返回值说明, 见 {@link #privateMsg 私聊消息} 的方法
     */
    public int requestAddFriend(int subtype, int sendTime, long fromQQ, String msg, String responseFlag) {
        // 这里处理消息

        /*
         * REQUEST_ADOPT 通过 REQUEST_REFUSE 拒绝
         */

        // CQ.setFriendAddRequest(responseFlag, REQUEST_ADOPT, null); // 同意好友添加请求
        return MSG_IGNORE;
    }

    /**
     * 请求-群添加 (Type=302)<br>
     * 本方法会在酷Q【线程】中被调用。<br>
     *
     * @param subtype      子类型，1/他人申请入群 2/自己(即登录号)受邀入群
     * @param sendTime     发送时间(时间戳)
     * @param fromGroup    来源群号
     * @param fromQQ       来源QQ
     * @param msg          附言
     * @param responseFlag 反馈标识(处理请求用)
     * @return 关于返回值说明, 见 {@link #privateMsg 私聊消息} 的方法
     */
    public int requestAddGroup(int subtype, int sendTime, long fromGroup, long fromQQ, String msg,
                               String responseFlag) {
        // 这里处理消息

        /*
         * REQUEST_ADOPT 通过 REQUEST_REFUSE 拒绝 REQUEST_GROUP_ADD 群添加 REQUEST_GROUP_INVITE
         * 群邀请
         */
        /*
         * if(subtype == 1){ // 本号为群管理，判断是否为他人申请入群 CQ.setGroupAddRequest(responseFlag,
         * REQUEST_GROUP_ADD, REQUEST_ADOPT, null);// 同意入群 } if(subtype == 2){
         * CQ.setGroupAddRequest(responseFlag, REQUEST_GROUP_INVITE, REQUEST_ADOPT,
         * null);// 同意进受邀群 }
         */

        return MSG_IGNORE;
    }

    /**
     * 本函数会在JCQ【线程】中被调用。
     *
     * @return 固定返回0
     */
    public int menuA() {
        JOptionPane.showMessageDialog(null, "这是测试菜单A，可以在这里加载窗口");
        return 0;
    }

    /**
     * 本函数会在酷Q【线程】中被调用。
     *
     * @return 固定返回0
     */
    public int menuB() {
        JOptionPane.showMessageDialog(null, "这是测试菜单B，可以在这里加载窗口");
        return 0;
    }

}
