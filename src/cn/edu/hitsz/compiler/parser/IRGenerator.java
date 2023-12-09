package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {

    private SymbolTable symbolTable;
    private List<Instruction> instructionList = new ArrayList<>();    // 用于存储生成的 IR 指令
    private Stack<Symbol> symbolStack = new Stack<Symbol>();

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO
        symbolStack.push(new Symbol(currentToken));
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO
        switch (production.index()) {
            case 6 -> {
                // S -> id = E
                // 获取id的token和E的值
                var E_value = symbolStack.pop().getIrValue();
                symbolStack.pop();
                var id_token = symbolStack.pop().getToken();

                var text = id_token.getText();
                if (!symbolTable.has(text)) {
                    throw new RuntimeException(text + " not found in symbol table");
                }
                var name = IRVariable.named(text);                                    // 源代码中的变量名
                Instruction instruction = Instruction.createMov(name, E_value);
                instructionList.add(instruction);
                symbolStack.push(new Symbol());
            }

            case 7 -> {
                // S -> return E
                var E_value = symbolStack.pop().getIrValue();
                symbolStack.pop();
                Instruction instruction = Instruction.createRet(E_value);
                instructionList.add(instruction);
                symbolStack.push(new Symbol());
            }

            case 8 -> {
                // E -> E + A
                var A_value = symbolStack.pop().getIrValue();
                symbolStack.pop();
                var E_value = symbolStack.pop().getIrValue();
                var tempReg = IRVariable.temp();
                Instruction instruction = Instruction.createAdd(tempReg, E_value, A_value);
                instructionList.add(instruction);
                symbolStack.push(new Symbol(tempReg));
            }

            case 9 -> {
                // E -> E - A
                var A_value = symbolStack.pop().getIrValue();
                symbolStack.pop();
                var E_value = symbolStack.pop().getIrValue();
                var tempReg = IRVariable.temp();
                Instruction instruction = Instruction.createSub(tempReg, E_value, A_value);
                instructionList.add(instruction);
                symbolStack.push(new Symbol(tempReg));
            }

            case 10, 12 -> {
                // E -> A, A -> B
                var Right_value = symbolStack.pop().getIrValue();
                symbolStack.push(new Symbol(Right_value));
            }

            case 11 -> {
                // A -> A * B
                var B_value = symbolStack.pop().getIrValue();
                symbolStack.pop();
                var A_value = symbolStack.pop().getIrValue();
                var tempReg = IRVariable.temp();
                Instruction instruction = Instruction.createMul(tempReg, A_value, B_value);
                instructionList.add(instruction);
                symbolStack.push(new Symbol(tempReg));
            }

            case 13 -> {
                // B -> ( E )
                symbolStack.pop();
                var E_value = symbolStack.pop().getIrValue();
                symbolStack.pop();
                symbolStack.push(new Symbol(E_value));
            }

            case 14 -> {
                // B -> id
                var id_token = symbolStack.pop().getToken();
                var text = id_token.getText();
                if (!symbolTable.has(text)) {
                    throw new RuntimeException(text + " not found in symbol table");
                }
                var name = IRVariable.named(text);
                symbolStack.push(new Symbol(name));
            }

            case 15 -> {
                // B -> IntConst
                var int_token = symbolStack.pop().getToken();
                // 立即数
                var B_value = IRImmediate.of(Integer.parseInt(int_token.getText()));
                symbolStack.push(new Symbol(B_value));
            }

            default -> {
                int length = production.body().size();
                while (length > 0) {
                    symbolStack.pop();
                    length--;
                }
                symbolStack.push(new Symbol());
            }

        }
    }


    @Override
    public void whenAccept(Status currentStatus) {
        // TODO
        symbolStack.clear();
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO
        this.symbolTable = table;
    }

    public List<Instruction> getIR() {
        // TODO
        return instructionList;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}

