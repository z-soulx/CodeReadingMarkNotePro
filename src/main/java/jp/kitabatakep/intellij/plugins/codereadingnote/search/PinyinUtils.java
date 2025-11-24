package jp.kitabatakep.intellij.plugins.codereadingnote.search;

import java.util.*;

/**
 * 拼音工具类，用于支持中文拼音搜索
 * 使用简化的拼音映射表，支持多音字的常见读音
 */
public class PinyinUtils {
    // 简化的拼音映射表（仅包含常用字）
    private static final Map<Character, String[]> PINYIN_MAP = new HashMap<>();
    
    static {
        // 初始化常用字的拼音映射（这里只列举部分示例，实际使用中可以扩展）
        // 数字
        addPinyin('一', "yi");
        addPinyin('二', "er");
        addPinyin('三', "san");
        addPinyin('四', "si");
        addPinyin('五', "wu");
        addPinyin('六', "liu");
        addPinyin('七', "qi");
        addPinyin('八', "ba");
        addPinyin('九', "jiu");
        addPinyin('十', "shi");
        
        // 常用字
        addPinyin('如', "ru");
        addPinyin('家', "jia");
        addPinyin('的', "de", "di");
        addPinyin('是', "shi");
        addPinyin('在', "zai");
        addPinyin('有', "you");
        addPinyin('个', "ge");
        addPinyin('人', "ren");
        addPinyin('这', "zhe");
        addPinyin('中', "zhong");
        addPinyin('大', "da");
        addPinyin('为', "wei");
        addPinyin('上', "shang");
        addPinyin('们', "men");
        addPinyin('到', "dao");
        addPinyin('说', "shuo");
        addPinyin('国', "guo");
        addPinyin('我', "wo");
        addPinyin('以', "yi");
        addPinyin('要', "yao");
        addPinyin('他', "ta");
        addPinyin('时', "shi");
        addPinyin('来', "lai");
        addPinyin('用', "yong");
        addPinyin('你', "ni");
        addPinyin('生', "sheng");
        addPinyin('年', "nian");
        addPinyin('着', "zhe");
        addPinyin('就', "jiu");
        addPinyin('那', "na");
        addPinyin('和', "he");
        addPinyin('会', "hui");
        addPinyin('出', "chu");
        addPinyin('也', "ye");
        addPinyin('得', "de", "dei");
        addPinyin('里', "li");
        addPinyin('后', "hou");
        addPinyin('自', "zi");
        addPinyin('把', "ba");
        addPinyin('去', "qu");
        addPinyin('子', "zi");
        addPinyin('得', "de");
        addPinyin('想', "xiang");
        addPinyin('看', "kan");
        addPinyin('分', "fen");
        addPinyin('还', "hai", "huan");
        addPinyin('因', "yin");
        addPinyin('由', "you");
        addPinyin('从', "cong");
        addPinyin('两', "liang");
        addPinyin('长', "chang", "zhang");
        addPinyin('无', "wu");
        addPinyin('明', "ming");
        addPinyin('日', "ri");
        addPinyin('文', "wen");
        addPinyin('重', "zhong", "chong");
        addPinyin('信', "xin");
        addPinyin('注', "zhu");
        addPinyin('行', "xing", "hang");
        addPinyin('方', "fang");
        addPinyin('期', "qi");
        addPinyin('它', "ta");
        addPinyin('水', "shui");
        addPinyin('正', "zheng");
        addPinyin('体', "ti");
        addPinyin('通', "tong");
        addPinyin('但', "dan");
        addPinyin('加', "jia");
        addPinyin('问', "wen");
        addPinyin('工', "gong");
        addPinyin('三', "san");
        addPinyin('已', "yi");
        addPinyin('老', "lao");
        addPinyin('从', "cong");
        addPinyin('动', "dong");
        addPinyin('两', "liang");
        addPinyin('些', "xie");
        addPinyin('间', "jian");
        addPinyin('样', "yang");
        addPinyin('民', "min");
        addPinyin('得', "de");
        addPinyin('第', "di");
        addPinyin('新', "xin");
        addPinyin('书', "shu");
        addPinyin('物', "wu");
        addPinyin('见', "jian");
        addPinyin('主', "zhu");
        addPinyin('没', "mei");
        addPinyin('理', "li");
        addPinyin('当', "dang");
        addPinyin('起', "qi");
        addPinyin('面', "mian");
        addPinyin('定', "ding");
        addPinyin('回', "hui");
        addPinyin('部', "bu");
        addPinyin('者', "zhe");
        addPinyin('手', "shou");
        addPinyin('知', "zhi");
        addPinyin('理', "li");
        addPinyin('眼', "yan");
        addPinyin('志', "zhi");
        addPinyin('点', "dian");
        addPinyin('心', "xin");
        addPinyin('战', "zhan");
        addPinyin('向', "xiang");
        addPinyin('光', "guang");
        addPinyin('位', "wei");
        addPinyin('路', "lu");
        addPinyin('科', "ke");
        addPinyin('今', "jin");
        addPinyin('声', "sheng");
        addPinyin('合', "he");
        addPinyin('立', "li");
        addPinyin('代', "dai");
        addPinyin('员', "yuan");
        addPinyin('机', "ji");
        addPinyin('更', "geng");
        addPinyin('九', "jiu");
        addPinyin('您', "nin");
        addPinyin('每', "mei");
        addPinyin('风', "feng");
        addPinyin('级', "ji");
        addPinyin('跟', "gen");
        addPinyin('笑', "xiao");
        addPinyin('啊', "a");
        addPinyin('孩', "hai");
        addPinyin('万', "wan");
        addPinyin('少', "shao");
        addPinyin('直', "zhi");
        addPinyin('意', "yi");
        addPinyin('夜', "ye");
        addPinyin('比', "bi");
        addPinyin('阶', "jie");
        addPinyin('连', "lian");
        addPinyin('车', "che");
        addPinyin('重', "zhong");
        addPinyin('便', "bian");
        addPinyin('斗', "dou");
        addPinyin('马', "ma");
        addPinyin('哪', "na");
        addPinyin('化', "hua");
        addPinyin('太', "tai");
        addPinyin('指', "zhi");
        addPinyin('变', "bian");
        addPinyin('社', "she");
        addPinyin('似', "si");
        addPinyin('士', "shi");
        addPinyin('者', "zhe");
        addPinyin('干', "gan");
        addPinyin('石', "shi");
        addPinyin('满', "man");
        addPinyin('日', "ri");
        addPinyin('决', "jue");
        addPinyin('百', "bai");
        addPinyin('原', "yuan");
        addPinyin('拿', "na");
        addPinyin('群', "qun");
        addPinyin('究', "jiu");
        addPinyin('各', "ge");
        addPinyin('六', "liu");
        addPinyin('本', "ben");
        addPinyin('思', "si");
        addPinyin('解', "jie");
        addPinyin('立', "li");
        addPinyin('河', "he");
        addPinyin('村', "cun");
        addPinyin('八', "ba");
        addPinyin('难', "nan");
        addPinyin('早', "zao");
        addPinyin('论', "lun");
        addPinyin('吗', "ma");
        addPinyin('根', "gen");
        addPinyin('共', "gong");
        addPinyin('让', "rang");
        addPinyin('相', "xiang");
        addPinyin('研', "yan");
        addPinyin('今', "jin");
        addPinyin('其', "qi");
        addPinyin('书', "shu");
        addPinyin('坐', "zuo");
        addPinyin('接', "jie");
        addPinyin('应', "ying");
        addPinyin('关', "guan");
        addPinyin('信', "xin");
        addPinyin('觉', "jue");
        addPinyin('步', "bu");
        addPinyin('反', "fan");
        addPinyin('处', "chu");
        addPinyin('记', "ji");
        addPinyin('将', "jiang");
        addPinyin('千', "qian");
        addPinyin('找', "zhao");
        addPinyin('争', "zheng");
        addPinyin('领', "ling");
        addPinyin('或', "huo");
        addPinyin('师', "shi");
        addPinyin('结', "jie");
        addPinyin('块', "kuai");
        addPinyin('跑', "pao");
        addPinyin('谁', "shui");
        addPinyin('草', "cao");
        addPinyin('越', "yue");
        addPinyin('字', "zi");
        addPinyin('加', "jia");
        addPinyin('脚', "jiao");
        addPinyin('紧', "jin");
        addPinyin('爱', "ai");
        addPinyin('等', "deng");
        addPinyin('习', "xi");
        addPinyin('阵', "zhen");
        addPinyin('怕', "pa");
        addPinyin('月', "yue");
        addPinyin('青', "qing");
        addPinyin('半', "ban");
        addPinyin('火', "huo");
        addPinyin('法', "fa");
        addPinyin('题', "ti");
        addPinyin('建', "jian");
        addPinyin('赶', "gan");
        addPinyin('位', "wei");
        addPinyin('唱', "chang");
        addPinyin('海', "hai");
        addPinyin('七', "qi");
        addPinyin('女', "nv");
        addPinyin('任', "ren");
        addPinyin('件', "jian");
        addPinyin('感', "gan");
        addPinyin('准', "zhun");
        addPinyin('张', "zhang");
        addPinyin('团', "tuan");
        addPinyin('屋', "wu");
        addPinyin('离', "li");
        addPinyin('色', "se");
        addPinyin('脸', "lian");
        addPinyin('片', "pian");
        addPinyin('科', "ke");
        addPinyin('倒', "dao");
        addPinyin('睛', "jing");
        addPinyin('利', "li");
        addPinyin('世', "shi");
        addPinyin('刚', "gang");
        addPinyin('且', "qie");
        addPinyin('由', "you");
        addPinyin('送', "song");
        addPinyin('切', "qie");
        addPinyin('星', "xing");
        addPinyin('导', "dao");
        addPinyin('晚', "wan");
        addPinyin('表', "biao");
        addPinyin('够', "gou");
        addPinyin('整', "zheng");
        addPinyin('认', "ren");
        addPinyin('响', "xiang");
        addPinyin('雪', "xue");
        addPinyin('流', "liu");
        addPinyin('未', "wei");
        addPinyin('场', "chang");
        addPinyin('该', "gai");
        addPinyin('并', "bing");
        addPinyin('底', "di");
        addPinyin('深', "shen");
        addPinyin('刻', "ke");
        addPinyin('平', "ping");
        addPinyin('伟', "wei");
        addPinyin('忙', "mang");
        addPinyin('提', "ti");
        addPinyin('确', "que");
        addPinyin('近', "jin");
        addPinyin('亮', "liang");
        addPinyin('轻', "qing");
        addPinyin('讲', "jiang");
        addPinyin('农', "nong");
        addPinyin('古', "gu");
        addPinyin('黑', "hei");
        addPinyin('告', "gao");
        addPinyin('界', "jie");
        addPinyin('拉', "la");
        addPinyin('名', "ming");
        addPinyin('呀', "ya");
        addPinyin('土', "tu");
        addPinyin('清', "qing");
        addPinyin('阳', "yang");
        addPinyin('照', "zhao");
        addPinyin('办', "ban");
        addPinyin('史', "shi");
        addPinyin('改', "gai");
        addPinyin('历', "li");
        addPinyin('转', "zhuan");
        addPinyin('画', "hua");
        addPinyin('造', "zao");
        addPinyin('嘴', "zui");
        addPinyin('此', "ci");
        addPinyin('治', "zhi");
        addPinyin('北', "bei");
        addPinyin('必', "bi");
        addPinyin('服', "fu");
        addPinyin('雨', "yu");
        addPinyin('穿', "chuan");
        addPinyin('内', "nei");
        addPinyin('识', "shi");
        addPinyin('验', "yan");
        addPinyin('传', "chuan");
        addPinyin('业', "ye");
        addPinyin('菜', "cai");
        addPinyin('爬', "pa");
        addPinyin('睡', "shui");
        addPinyin('兴', "xing");
        addPinyin('形', "xing");
        addPinyin('量', "liang");
        addPinyin('咱', "zan");
        addPinyin('观', "guan");
        addPinyin('苦', "ku");
        addPinyin('体', "ti");
        addPinyin('众', "zhong");
        addPinyin('通', "tong");
        addPinyin('冲', "chong");
        addPinyin('合', "he");
        addPinyin('破', "po");
        addPinyin('友', "you");
        addPinyin('度', "du");
        addPinyin('术', "shu");
        addPinyin('饭', "fan");
        addPinyin('公', "gong");
        addPinyin('旁', "pang");
        addPinyin('房', "fang");
        addPinyin('极', "ji");
        addPinyin('南', "nan");
        addPinyin('枪', "qiang");
        addPinyin('读', "du");
        addPinyin('沙', "sha");
        addPinyin('岁', "sui");
        addPinyin('线', "xian");
        addPinyin('野', "ye");
        addPinyin('坚', "jian");
        addPinyin('空', "kong");
        addPinyin('收', "shou");
        addPinyin('算', "suan");
        addPinyin('至', "zhi");
        addPinyin('政', "zheng");
        addPinyin('城', "cheng");
        addPinyin('劳', "lao");
        addPinyin('落', "luo");
        addPinyin('钱', "qian");
        addPinyin('特', "te");
        addPinyin('围', "wei");
        addPinyin('弟', "di");
        addPinyin('胜', "sheng");
        addPinyin('教', "jiao");
        addPinyin('热', "re");
        addPinyin('展', "zhan");
        addPinyin('包', "bao");
        addPinyin('歌', "ge");
        addPinyin('类', "lei");
        addPinyin('渐', "jian");
        addPinyin('强', "qiang");
        addPinyin('数', "shu");
        addPinyin('乡', "xiang");
        addPinyin('呼', "hu");
        addPinyin('性', "xing");
        addPinyin('音', "yin");
        addPinyin('答', "da");
        addPinyin('哥', "ge");
        addPinyin('际', "ji");
        addPinyin('旧', "jiu");
        addPinyin('神', "shen");
        addPinyin('座', "zuo");
        addPinyin('章', "zhang");
        addPinyin('帮', "bang");
        addPinyin('啦', "la");
        addPinyin('受', "shou");
        addPinyin('系', "xi");
        addPinyin('令', "ling");
        addPinyin('跳', "tiao");
        addPinyin('非', "fei");
        addPinyin('何', "he");
        addPinyin('牛', "niu");
        addPinyin('取', "qu");
        addPinyin('入', "ru");
        addPinyin('岸', "an");
        addPinyin('敢', "gan");
        addPinyin('掉', "diao");
        addPinyin('忽', "hu");
        addPinyin('种', "zhong");
        addPinyin('装', "zhuang");
        addPinyin('顶', "ding");
        addPinyin('急', "ji");
        addPinyin('林', "lin");
        addPinyin('停', "ting");
        addPinyin('息', "xi");
        addPinyin('句', "ju");
        addPinyin('区', "qu");
        addPinyin('衣', "yi");
        addPinyin('般', "ban");
        addPinyin('报', "bao");
        addPinyin('叶', "ye");
        addPinyin('压', "ya");
        addPinyin('慢', "man");
        addPinyin('叔', "shu");
        addPinyin('背', "bei");
        addPinyin('细', "xi");
    }
    
    private static void addPinyin(char ch, String... pinyins) {
        PINYIN_MAP.put(ch, pinyins);
    }
    
    /**
     * 获取字符的拼音
     */
    public static String[] getPinyin(char ch) {
        return PINYIN_MAP.getOrDefault(ch, new String[]{String.valueOf(ch)});
    }
    
    /**
     * 获取字符串的拼音首字母
     */
    public static String getFirstLetters(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        for (char ch : text.toCharArray()) {
            String[] pinyins = getPinyin(ch);
            if (pinyins.length > 0 && !pinyins[0].equals(String.valueOf(ch))) {
                result.append(pinyins[0].charAt(0));
            } else {
                result.append(ch);
            }
        }
        return result.toString().toLowerCase();
    }
    
    /**
     * 获取字符串的完整拼音
     */
    public static String getFullPinyin(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        for (char ch : text.toCharArray()) {
            String[] pinyins = getPinyin(ch);
            if (pinyins.length > 0) {
                result.append(pinyins[0]);
            }
        }
        return result.toString().toLowerCase();
    }
    
    /**
     * 检查文本是否匹配查询（支持拼音和拼音首字母）
     */
    public static boolean matches(String text, String query) {
        if (text == null || query == null) {
            return false;
        }
        
        text = text.toLowerCase();
        query = query.toLowerCase();
        
        // 直接包含
        if (text.contains(query)) {
            return true;
        }
        
        // 拼音首字母匹配
        String firstLetters = getFirstLetters(text);
        if (firstLetters.contains(query)) {
            return true;
        }
        
        // 完整拼音匹配
        String fullPinyin = getFullPinyin(text);
        if (fullPinyin.contains(query)) {
            return true;
        }
        
        return false;
    }
}

