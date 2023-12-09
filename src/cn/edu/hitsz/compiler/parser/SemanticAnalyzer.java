package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.Stack;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {

    private Stack<Symbol> symbolStack = new Stack<Symbol>();
    private SymbolTable symbolTable;

    @Override
    public void whenAccept(Status currentStatus) {
        // TODO: 该过程在遇到 Accept 时要采取的代码动作
        symbolStack.clear();
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO: 该过程在遇到 reduce production 时要采取的代码动作
        int index = production.index();
        switch (index) {
            case 4:
                // S -> D id
                // 获取id的token和D的类型
                Token id_token = symbolStack.pop().getToken();
                SourceCodeType D_type = symbolStack.pop().getSourceCodeType();
                // 将id的类型设置为D的类型
                symbolTable.get(id_token.getText()).setType(D_type);
                // 占位 S
                symbolStack.push(new Symbol());
                break;

            case 5:
                // D -> int
                symbolStack.pop();
                symbolStack.push(new Symbol(SourceCodeType.Int));
                break;

            default:
                int length = production.body().size();
                while (length > 0) {
                    symbolStack.pop();
                    length--;
                }
                symbolStack.push(new Symbol());
                break;
        }
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO: 该过程在遇到 shift 时要采取的代码动作
        symbolStack.push(new Symbol(currentToken));
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO: 设计你可能需要的符号表存储结构
        // 如果需要使用符号表的话, 可以将它或者它的一部分信息存起来, 比如使用一个成员变量存储
        this.symbolTable = table;
    }
}

