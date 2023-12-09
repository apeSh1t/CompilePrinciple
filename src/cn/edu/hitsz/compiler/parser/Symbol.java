package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.NonTerminal;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;

class Symbol {
    Token token;
    NonTerminal nonTerminal;
    IRValue irValue;
    SourceCodeType sourceCodeType;

    Symbol(Token token, NonTerminal nonTerminal, IRValue irValue, SourceCodeType sourceCodeType) {
        this.token = token;
        this.nonTerminal = nonTerminal;
        this.irValue = irValue;
        this.sourceCodeType = sourceCodeType;
    }

    public Symbol(Token token) {
        this(token, null, null, null);
    }

    public Symbol(NonTerminal nonTerminal) {
        this(null, nonTerminal, null, null);
    }

    public Symbol(IRValue irValue) {
        this(null, null, irValue, null);
    }

    public Symbol(SourceCodeType sourceCodeType) {
        this(null, null, null, sourceCodeType);
    }

    public Symbol() {
        this(null, null, null, null);
    }

    public boolean isToken() {
        return this.token != null;
    }

    public boolean isNonterminal() {
        return this.nonTerminal != null;
    }

    public boolean isIRValue() {
        return this.irValue != null;
    }

    public boolean isSourceCodeType() { return this.sourceCodeType != null; }

    public Token getToken() {
        return token;
    }

    public NonTerminal getNonTerminal() {
        return nonTerminal;
    }

    public IRValue getIrValue() {
        return irValue;
    }

    public SourceCodeType getSourceCodeType() {
        return sourceCodeType;
    }

}

