import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@org.checkerframework.checker.regex.qual.Regex interface A<@org.checkerframework.checker.regex.qual.Regex T> {
    @org.checkerframework.checker.regex.qual.Regex T doStuff(@org.checkerframework.checker.regex.qual.Regex T arg);
}

@org.checkerframework.checker.regex.qual.Regex class B {
    @org.checkerframework.checker.regex.qual.Regex
    public String doSomeCrazyShit(@org.checkerframework.checker.regex.qual.Regex String[] @org.checkerframework.checker.regex.qual.Regex [] @org.checkerframework.checker.regex.qual.Regex [] lol) {
        return "lol";
    }
}

@org.checkerframework.checker.regex.qual.Regex
public class Test extends B implements @org.checkerframework.checker.regex.qual.Regex A<Integer> {

    @org.checkerframework.checker.regex.qual.Regex String s1 = "hello";
    @org.checkerframework.checker.regex.qual.Regex String s2 = new @org.checkerframework.checker.regex.qual.Regex String("world");
    @org.checkerframework.checker.regex.qual.Regex String s3 = (@org.checkerframework.checker.regex.qual.Regex String) "lol";

    @org.checkerframework.checker.regex.qual.Regex String @org.checkerframework.checker.regex.qual.Regex [] a0;
    @org.checkerframework.checker.regex.qual.Regex String a1@org.checkerframework.checker.regex.qual.Regex[];

    @org.checkerframework.checker.regex.qual.Regex String @org.checkerframework.checker.regex.qual.Regex [][] a2;
    @org.checkerframework.checker.regex.qual.Regex String[] @org.checkerframework.checker.regex.qual.Regex [] a3;

    @org.checkerframework.checker.regex.qual.Regex String @org.checkerframework.checker.regex.qual.Regex [] a4[];
    @org.checkerframework.checker.regex.qual.Regex String[] a5@org.checkerframework.checker.regex.qual.Regex[];

    @org.checkerframework.checker.regex.qual.Regex String a6@org.checkerframework.checker.regex.qual.Regex[][];
    @org.checkerframework.checker.regex.qual.Regex String a7[]@org.checkerframework.checker.regex.qual.Regex[];

    @org.checkerframework.checker.regex.qual.Regex int i1;
    @org.checkerframework.checker.regex.qual.Regex Double d1;
    @org.checkerframework.checker.regex.qual.Regex List<@org.checkerframework.checker.regex.qual.Regex String @org.checkerframework.checker.regex.qual.Regex []> l4 = new @org.checkerframework.checker.regex.qual.Regex ArrayList<@org.checkerframework.checker.regex.qual.Regex String @org.checkerframework.checker.regex.qual.Regex []>();

    List<@org.checkerframework.checker.regex.qual.Regex ? extends Number> ln;
    List<@org.checkerframework.checker.regex.qual.Regex ? extends String> ls;
    List<@org.checkerframework.checker.regex.qual.Regex ? extends Pattern> lp;

    Object obj = new @org.checkerframework.checker.regex.qual.Regex Object();

    @Override
    public @org.checkerframework.checker.regex.qual.Regex String doSomeCrazyShit(@org.checkerframework.checker.regex.qual.Regex String[] @org.checkerframework.checker.regex.qual.Regex [] @org.checkerframework.checker.regex.qual.Regex [] lol) {
        return super.doSomeCrazyShit(lol);
    }

    @org.checkerframework.checker.regex.qual.Regex
    public String doStuff(@org.checkerframework.checker.regex.qual.Regex String r, @org.checkerframework.checker.regex.qual.Regex int a) throws @org.checkerframework.checker.regex.qual.Regex Error {
        @org.checkerframework.checker.regex.qual.Regex List<@org.checkerframework.checker.regex.qual.Regex String> rrrr = new @org.checkerframework.checker.regex.qual.Regex ArrayList<@org.checkerframework.checker.regex.qual.Regex String>();
        rrrr.add("asdfa");
        return "42";
    }

    @Override
    @org.checkerframework.checker.regex.qual.Regex
    public Integer doStuff(@org.checkerframework.checker.regex.qual.Regex Integer arg) {
        return null;
    }
}
