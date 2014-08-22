import org.checkerframework.framework.util.CheckerMain;import java.lang.String;

public class Test {

    public static void main(String[] args) {
        CheckerMain.main(new String[]{"/data/repositories/Test.java","-processor","org.checkerframework.checker.regex.RegexChecker"});
    }
}
