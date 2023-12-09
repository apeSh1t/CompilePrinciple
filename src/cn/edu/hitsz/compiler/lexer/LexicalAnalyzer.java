package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.*;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;
    private String codes;
    private List<Token> tokens = new LinkedList<>();

    /**
     * 存放所有的终态
     */
    private static final Set<Integer> finalStates = new HashSet<>(Arrays.asList(15, 17, 19, 20, 22, 23, 25, 26, 27, 28, 29, 30, 31));

    /**
     * 存放所有的无用符号
     */
    private static final List<Character> uselessChar = Arrays.asList(' ', '\n', '\t');
    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        // TODO: 词法分析前的缓冲区实现
        // 可自由实现各类缓冲区
        // 或直接采用完整读入方法
        codes = FileUtils.readFile(path);
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // TODO: 自动机实现的词法分析过程
        int slowPointer = 0, fastPointer = 0, length = codes.length();
        int currentState = 0, nextState = 0;
        char c;

        boolean isLetter = false, isDigit = false, isUseless = false;

        for(slowPointer=0; slowPointer<length; slowPointer++){
            // 外层for循环用于遍历整个代码，即遍历每一个token
            currentState = 0;
            nextState = 0;
            fastPointer = slowPointer;

            while(!(finalStates.contains(currentState)) && (fastPointer<length)){
                // 内层while循环用于遍历当前token的每一个字符，即找到当前token的位置，位于快慢指针之间
                c = codes.charAt(fastPointer);
                isLetter = Character.isLetter(c);
                isDigit = Character.isDigit(c);
                isUseless = uselessChar.contains(c);

                // 实验指导书状态转移逻辑
                nextState = switch (currentState){
                    case 0 -> switch (c){
                        case '*' -> 18;
                        case '=' -> 21;
                        case '(' -> 26;
                        case ')' -> 27;
                        case ';' -> 28;
                        case '+' -> 29;
                        case '-' -> 30;
                        case '/' -> 31;
                        default -> {
                            if (isLetter) yield 14;
                            else if (isDigit) yield 16;
                            else if (isUseless) yield 0;
                            else throw new NotImplementedException();
                        }
                    };

                    case 14 -> {
                        if (isLetter || isDigit) yield 14;
                        else yield 15;
                    }
                    case 16 -> {
                        if (isDigit) yield 16;
                        else yield 17;
                    }

                    case 18 -> {
                        if (c == '*') yield 19;
                        else yield 20;
                    }

                    case 21 -> {
                        if (c == '=') yield 22;
                        else yield 23;
                    }

                    // 如果检测到终态，那么进行下一轮检测
                    default -> 0;
                };

                if (finalStates.contains(nextState)){
                    Token tempToken;
                    tempToken = switch (nextState){
                        case 15 -> {
                            if ("return".equals(codes.substring(slowPointer, fastPointer))) yield  Token.simple("return");
                            else if("int".equals(codes.substring(slowPointer, fastPointer))) yield  Token.simple("int");
                            else{
                                if(!symbolTable.has(codes.substring(slowPointer, fastPointer))){
                                    symbolTable.add(codes.substring(slowPointer, fastPointer));
                                }
                                yield Token.normal("id", codes.substring(slowPointer, fastPointer));
                            }
                        }
                        case 17 -> Token.normal("IntConst", codes.substring(slowPointer, fastPointer));
                        case 19 -> Token.simple("**");
                        case 20 -> Token.simple("*");
                        case 22 -> Token.simple("==");
                        case 23 -> Token.simple("=");
                        case 26 -> Token.simple("(");
                        case 27 -> Token.simple(")");
                        case 28 -> Token.simple("Semicolon");
                        case 29 -> Token.simple("+");
                        case 30 -> Token.simple("-");
                        case 31 -> Token.simple("/");
                        default -> throw new NotImplementedException();
                    };
                    tokens.add(tempToken);
                }

                switch (currentState) {
                    case 0 -> {
                        if (isUseless) {
                            slowPointer++;
                        }
                    }
                    case 14, 16 -> {
                        if (c == ';') {
                            tokens.add(Token.simple("Semicolon"));
                        }
                    }
                    default -> {}
                }

                currentState = nextState;
                fastPointer++;
            }
            slowPointer = fastPointer - 1;
        }
        tokens.add(Token.eof());
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // TODO: 从词法分析过程中获取 Token 列表
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        return tokens;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }


}
