package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {

    // 寄存器-变量分配表
    private final Map<Integer, IRVariable> registerVariableMap = new HashMap<>();
    // 最后的汇编代码
    private final List<String> assemblyCode = new ArrayList<>();
    // 中间代码
    private List<Instruction> originInstructions;

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // TODO: 读入前端提供的中间代码并生成所需要的信息
        this.originInstructions = originInstructions;
    }

    /**
     * 从寄存器中找到变量
     * @param variable 变量
     * @return 变量所在的寄存器，如果没有找到则返回-1
     */
    public int findVariableFromReg(IRVariable variable) {
    	for (Map.Entry<Integer, IRVariable> entry : registerVariableMap.entrySet()) {
    		if (entry.getValue().equals(variable)) {
    			return entry.getKey();
    		}
    	}
    	return -1;
    }

    /**
     * 判断是否还有空闲的寄存器
     * @return true表示还有空闲的寄存器，false表示没有空闲的寄存器
     */
    public boolean ifHasFreeReg() {
    	return registerVariableMap.size() < 7;
    }

    /**
     * 将变量写入寄存器
     * @param variable 变量
     * @return 变量所在的寄存器，如果没有空闲的寄存器则占据一个
     */
    public int writeVariable2Reg(IRVariable variable) {
    	int reg = -1;
    	if (ifHasFreeReg()) {
    		for (int i = 0; i <= 6; i++) {
    			if (!registerVariableMap.containsKey(i)) {
    				reg = i;
    				registerVariableMap.put(reg, variable);
    				break;
    			}
    		}
    	}
        else {
            for (int i = 0; i <= 6; i++){
                if (!registerVariableMap.containsKey(i)) {
                    reg = i;
                    registerVariableMap.put(reg, variable);
                    break;
                }
            }
        }
    	return reg;
    }


    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        // TODO: 执行寄存器分配与代码生成
        for (var instruction : originInstructions) {
            int resultReg = -1;
            int lhsReg = -1;
            int rhsReg = -1;

            switch (instruction.getKind()) {
                // MOV ADD SUB MUL RET
            case MOV:
            	if (instruction.getFrom().isImmediate()) {
                    // mov reg, imm
                    resultReg = writeVariable2Reg(instruction.getResult());
                    assemblyCode.add("li t" + resultReg + ", " + ((IRImmediate)instruction.getFrom()).getValue());
                }
                else {
                    // mov reg, reg
                    lhsReg = findVariableFromReg((IRVariable)instruction.getFrom());
                    if (lhsReg == -1) {
                        // lhs not in reg
                        lhsReg = writeVariable2Reg((IRVariable)instruction.getFrom());
                    }
                    resultReg = findVariableFromReg(instruction.getResult());
                    if (resultReg == -1) {
                        // result not in reg
                        resultReg = writeVariable2Reg(instruction.getResult());
                    }
                    assemblyCode.add("mv t" + resultReg + ", t" + lhsReg);

                    // 如果是临时变量，释放寄存器
                    if (((IRVariable)instruction.getFrom()).isTemp()) {
                        registerVariableMap.remove(lhsReg);
                    }
                }
                break;

            case ADD:
                if (instruction.getLHS().isImmediate()) {
                    // add reg, imm, reg
                    rhsReg = findVariableFromReg((IRVariable)instruction.getRHS());
                    if (rhsReg == -1) {
                        // rhs not in reg
                        rhsReg = writeVariable2Reg((IRVariable)instruction.getRHS());
                    }
                    resultReg = writeVariable2Reg(instruction.getResult());
                    assemblyCode.add("addi t" + resultReg + ", t" + rhsReg + ", " + ((IRImmediate)instruction.getLHS()).getValue());

                    if (((IRVariable)instruction.getRHS()).isTemp()) {
                        registerVariableMap.remove(rhsReg);
                    }
                }
                else if (instruction.getRHS().isImmediate()) {
                    // add reg, reg, imm
                    lhsReg = findVariableFromReg((IRVariable)instruction.getLHS());
                    if (lhsReg == -1) {
                        // lhs not in reg
                        lhsReg = writeVariable2Reg((IRVariable)instruction.getLHS());
                    }
                    resultReg = writeVariable2Reg(instruction.getResult());
                    assemblyCode.add("addi t" + resultReg + ", t" + lhsReg + ", " + ((IRImmediate)instruction.getRHS()).getValue());

                    if (((IRVariable)instruction.getLHS()).isTemp()) {
                        registerVariableMap.remove(lhsReg);
                    }
                }
                else {
                    // add reg, reg, reg
                    lhsReg = findVariableFromReg((IRVariable)instruction.getLHS());
                    if (lhsReg == -1) {
                        // lhs not in reg
                        lhsReg = writeVariable2Reg((IRVariable)instruction.getLHS());
                    }
                    rhsReg = findVariableFromReg((IRVariable)instruction.getRHS());
                    if (rhsReg == -1) {
                        // rhs not in reg
                        rhsReg = writeVariable2Reg((IRVariable)instruction.getRHS());
                    }
                    resultReg = writeVariable2Reg(instruction.getResult());
                    assemblyCode.add("add t" + resultReg + ", t" + lhsReg + ", t" + rhsReg);

                    // 如果是临时变量，释放寄存器
                    if (((IRVariable)instruction.getLHS()).isTemp()) {
                        registerVariableMap.remove(lhsReg);
                    }
                    if (((IRVariable)instruction.getRHS()).isTemp()) {
                        registerVariableMap.remove(rhsReg);
                    }
                }
                break;

            case SUB:
                if (instruction.getLHS().isImmediate()) {
                    // sub reg, imm, reg
                    rhsReg = findVariableFromReg((IRVariable)instruction.getRHS());
                    if (rhsReg == -1) {
                        // rhs not in reg
                        rhsReg = writeVariable2Reg((IRVariable)instruction.getRHS());
                    }
                    resultReg = writeVariable2Reg(instruction.getResult());

                    // 这种情况先将LHS写入一个临时寄存器，然后再进行减法运算
                    int tempReg = writeVariable2Reg(IRVariable.temp());
                    assemblyCode.add("li t" + tempReg + ", " + ((IRImmediate)instruction.getLHS()).getValue());
                    assemblyCode.add("sub t" + resultReg + ", t" + tempReg + ", t" + rhsReg);

                    registerVariableMap.remove(tempReg);
                    if (((IRVariable)instruction.getRHS()).isTemp()) {
                        registerVariableMap.remove(rhsReg);
                    }
                }
                else if (instruction.getRHS().isImmediate()) {
                    // sub reg, reg, imm
                    lhsReg = findVariableFromReg((IRVariable)instruction.getLHS());
                    if (lhsReg == -1) {
                        // lhs not in reg
                        lhsReg = writeVariable2Reg((IRVariable)instruction.getLHS());
                    }
                    resultReg = writeVariable2Reg(instruction.getResult());
                    assemblyCode.add("addi t" + resultReg + ", t" + lhsReg + ", -" + ((IRImmediate)instruction.getRHS()).getValue());

                    if (((IRVariable)instruction.getLHS()).isTemp()) {
                        registerVariableMap.remove(lhsReg);
                    }
                }
                else {
                    // sub reg, reg, reg
                    lhsReg = findVariableFromReg((IRVariable)instruction.getLHS());
                    if (lhsReg == -1) {
                        // lhs not in reg
                        lhsReg = writeVariable2Reg((IRVariable)instruction.getLHS());
                    }
                    rhsReg = findVariableFromReg((IRVariable)instruction.getRHS());
                    if (rhsReg == -1) {
                        // rhs not in reg
                        rhsReg = writeVariable2Reg((IRVariable)instruction.getRHS());
                    }
                    resultReg = findVariableFromReg(instruction.getResult());
                    if (resultReg == -1) {
                        // result not in reg
                        resultReg = writeVariable2Reg(instruction.getResult());
                    }
                    assemblyCode.add("sub t" + resultReg + ", t" + lhsReg + ", t" + rhsReg);

                    // 如果是临时变量，释放寄存器
                    if (((IRVariable)instruction.getLHS()).isTemp()) {
                        registerVariableMap.remove(lhsReg);
                    }
                    if (((IRVariable)instruction.getRHS()).isTemp()) {
                        registerVariableMap.remove(rhsReg);
                    }
                }
                break;

            case MUL:
                if (instruction.getLHS().isImmediate()) {
                    // mul reg, imm, reg
                    rhsReg = findVariableFromReg((IRVariable)instruction.getRHS());
                    if (rhsReg == -1) {
                        // rhs not in reg
                        rhsReg = writeVariable2Reg((IRVariable)instruction.getRHS());
                    }
                    resultReg = writeVariable2Reg(instruction.getResult());
                    assemblyCode.add("mul t" + resultReg + ", t" + rhsReg + ", " + ((IRImmediate)instruction.getLHS()).getValue());

                    if (((IRVariable)instruction.getRHS()).isTemp()) {
                        registerVariableMap.remove(rhsReg);
                    }
                }
                else if (instruction.getRHS().isImmediate()) {
                    // mul reg, reg, imm
                    lhsReg = findVariableFromReg((IRVariable)instruction.getLHS());
                    if (lhsReg == -1) {
                        // lhs not in reg
                        lhsReg = writeVariable2Reg((IRVariable)instruction.getLHS());
                    }
                    resultReg = writeVariable2Reg(instruction.getResult());
                    assemblyCode.add("mul t" + resultReg + ", t" + lhsReg + ", " + ((IRImmediate)instruction.getRHS()).getValue());

                    if (((IRVariable)instruction.getLHS()).isTemp()) {
                        registerVariableMap.remove(lhsReg);
                    }
                }
                else {
                    // mul reg, reg, reg
                    lhsReg = findVariableFromReg((IRVariable)instruction.getLHS());
                    if (lhsReg == -1) {
                        // lhs not in reg
                        lhsReg = writeVariable2Reg((IRVariable)instruction.getLHS());
                    }
                    rhsReg = findVariableFromReg((IRVariable)instruction.getRHS());
                    if (rhsReg == -1) {
                        // rhs not in reg
                        rhsReg = writeVariable2Reg((IRVariable)instruction.getRHS());
                    }
                    resultReg = findVariableFromReg(instruction.getResult());
                    if (resultReg == -1) {
                        // result not in reg
                        resultReg = writeVariable2Reg(instruction.getResult());
                    }
                    assemblyCode.add("mul t" + resultReg + ", t" + lhsReg + ", t" + rhsReg);

                    // 如果是临时变量，释放寄存器
                    if (((IRVariable)instruction.getLHS()).isTemp()) {
                        registerVariableMap.remove(lhsReg);
                    }
                    if (((IRVariable)instruction.getRHS()).isTemp()) {
                        registerVariableMap.remove(rhsReg);
                    }
                }
                break;

            case RET:
                lhsReg = findVariableFromReg((IRVariable)instruction.getReturnValue());
                if (lhsReg == -1) {
                    // lhs not in reg
                    lhsReg = writeVariable2Reg((IRVariable)instruction.getReturnValue());
                }
                assemblyCode.add("mv a0, t" + lhsReg);

            }
        }
    }


    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        // TODO: 输出汇编代码到文件
        FileUtils.writeLines(path, assemblyCode);
    }
}

