# 实现细节

## 代码结构

### 编译流程

```mermaid
graph TD

    subgraph Compiler [编译器]
        direction TD
        Lexer(词法分析)
        Parser(语法分析)
        TacGen(中间代码生成)

        subgraph AsmGen [汇编代码生成]
            direction TD
            TacToHighLevelAsm(中间代码至抽象汇编)
            HighLevelAsmToAsm(抽象汇编至汇编)
        end
    end

    subgraph Assembler [汇编器]
        direction TD
        ReplacePesudoAsm(替换伪指令)
        AsmToObjectFile(汇编至目标文件)
    end

    subgraph Linker [链接器]
        direction TD
        Redirect(重定位)
    end

    Start(输入源代码)
    --> |program.c| Lexer
    --> |tokens| Parser
    --> |AST| TacGen
    --> |TAC| TacToHighLevelAsm
    --> |HL Asm| HighLevelAsmToAsm
    --> |Asm / program.s| ReplacePesudoAsm
    --> |Asm| AsmToObjectFile
    --> |Object / program.o| Redirect
    --> |Executable| Stop(输出可执行文件)
```

### 模拟LA64流程

```mermaid
graph TD
    subgraph Emulator [模拟器]
        direction TD
        Load(加载可执行文件)
        Loop(模拟 CPU 取指执行)
        Return(返回 a0 寄存器值)
    end
    Start(开始)
    --> Load
    --> Loop

    Loop --> Loop
    Loop --> |halt| Return
    --> Stop(结束)
```