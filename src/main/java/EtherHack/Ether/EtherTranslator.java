package EtherHack.Ether;

import EtherHack.utils.Logger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.core.Translator;

/**
 * 处理翻译功能的核心类
 * 负责加载指定目录下的翻译文件（.txt格式），并提供根据键获取翻译文本的方法
 */
public class EtherTranslator {
   /** 翻译文件存储路径（相对于游戏根目录） */
   private static final String TRANSLATIONS_PATH = "EtherHack/translations";
   /** 翻译内容存储容器：键为语言代码（如"ZH"、"EN"），值为该语言的键值对映射 */
   private Map<String, Map<String, String>> translations;

   /**
    * 构造方法：初始化翻译器
    */
   public EtherTranslator() {
      Logger.printLog("Initializing EtherTranslator...");
      this.translations = new HashMap<>();
   }

   /**
    * 加载所有翻译文件（.txt格式）
    * 会遍历TRANSLATIONS_PATH目录下的所有txt文件，解析其中的"键=值"对
    */
   public void loadTranslations() {
      File translationsDir = new File(TRANSLATIONS_PATH);
      File[] translationFiles = translationsDir.listFiles(EtherTranslator::lambda$loadTranslations$0);

      if (translationFiles == null) {
         Logger.printLog("Failed to load translations: no files found.");
         return;
      }

      for (File file : translationFiles) {
         // 提取语言代码（文件名去掉.txt后缀）
         String langCode = file.getName().replace(".txt", "");
         Map<String, String> langTranslations = new HashMap<>();

         try (BufferedReader reader = new BufferedReader(
                 new InputStreamReader(
                         new FileInputStream(file),
                         StandardCharsets.UTF_8 // 显式指定UTF-8编码读取
                 ))) {

            String line;
            while ((line = reader.readLine()) != null) {
               // 跳过空行和不含等号的行
               if (line.trim().isEmpty() || !line.contains("=")) continue;

               // 按第一个等号分割键值对（支持值中包含等号）
               String[] parts = line.split("=", 2);
               if (parts.length < 2) continue;

               String key = parts[0].trim();
               String value = parts[1].trim();

               // 清理值中的多余符号（末尾逗号、双引号）
               if (value.endsWith(",")) {
                  value = value.substring(0, value.length() - 1);
               }
               value = value.replaceAll("\"", "");

               langTranslations.put(key, value);
            }
         } catch (IOException e) {
            Logger.printLog("Failed to load translation file: " + file.getName());
            e.printStackTrace();
         }

         translations.put(langCode, langTranslations);
      }
   }

   /**
    * 根据键获取翻译文本（无变量替换）
    * @param key 翻译键
    * @return 翻译文本（无翻译时返回原键）
    */
   public String getTranslate(String key) {
      return getTranslate(key, null);
   }

   /**
    * 根据键获取翻译文本（支持变量替换）
    * @param key 翻译键
    * @param variables 变量表（键值对用于替换翻译文本中的{变量名}）
    * @return 翻译文本（无翻译时返回原键）
    */
   public String getTranslate(String key, KahluaTable variables) {
      if (key == null) {
         Logger.printLog("The translation key value was not obtained!");
         return "???";
      }

      // 获取当前游戏语言代码（如"ZH"、"EN"）
      String currentLang = Translator.getLanguage().name();
      Map<String, String> currentTranslations = translations.get(currentLang);

      // 语言无翻译时回退到英文
      if (currentTranslations == null) {
         Logger.printLog("No translations for language code: " + currentLang);
         currentTranslations = translations.get("EN");
         if (currentTranslations == null) return key;
      }

      String translatedText = currentTranslations.get(key);
      if (translatedText == null) {
         Logger.printLog("No translation for key: " + key + " for language: " + currentLang);
         return key;
      }

      // 变量替换（如翻译文本中有{name}，用variables中的对应值替换）
      if (variables != null && !variables.isEmpty()) {
         KahluaTableIterator iterator = variables.iterator();
         while (iterator.advance()) {
            String varKey = iterator.getKey().toString();
            String varValue = iterator.getValue().toString();
            translatedText = translatedText.replace("{" + varKey + "}", varValue);
         }
      }

      // 替换HTML换行符为实际换行
      translatedText = translatedText.replace("<br>", "\n");
      return translatedText;
   }

   /**
    * 文件过滤器：仅保留.txt后缀的文件
    */
   private static boolean lambda$loadTranslations$0(File dir, String name) {
      return name.endsWith(".txt");
   }
}