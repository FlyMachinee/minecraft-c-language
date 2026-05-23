# 用户手册

## C

- 多函数，参数数量任意
- 多文件
- 全局变量，局部变量，静态变量

### 类型

- int
- long
- unsigned int
- unsigned long

### 语句

- 标签语句
  * goto 标签语句
  * case 标签语句
  * default 标签语句
- 复合语句
- 选择语句
  * if 语句
  * if else 语句
  * switch 语句
- 循环语句
  * while 语句
  * do while 语句
  * for 语句
- 跳转语句
  * goto 语句
  * continue 语句
  * break 语句
  * return 语句

### 运算符

#### 基本表达式

- 整数常量
- 变量
- 括号表达式 ()

#### 后缀表达式

- 后缀自增 ++
- 后缀自减 --

#### 一元表达式

- 负号 -
- 按位取反 ~
- 逻辑非 !
- 前缀自增 ++
- 前缀自减 --

#### 二元表达式

- 加法 +
- 减法 -
- 乘法 *
- 除法 /
- 取模 %
- 逻辑左移 <<
- 算术右移 >>
- 按位与 &
- 按位或 |
- 按位异或 ^
- 逻辑与 &&
- 逻辑或 ||
- 等于 ==
- 不等于 !=
- 小于 <
- 大于 >
- 小于等于 <=
- 大于等于 >=

#### 赋值表达式

- 赋值 =
- 加法赋值 +=
- 减法赋值 -=
- 乘法赋值 *=
- 除法赋值 /=
- 取模赋值 %=
- 逻辑左移赋值 <<=
- 算术右移赋值 >>=
- 按位与赋值 &=
- 按位或赋值 |=
- 按位异或赋值 ^=

#### 条件表达式

- 条件表达式 ? :

## 指令

### LA64

#### 整数

- 算术类运算
  * add.w
  * add.d
  * sub.w
  * sub.d
  * slt
  * sltu
  * nor
  * and
  * or
  * xor
  * mul.w
  * mul.d
  * div.w
  * mod.w
  * div.wu
  * mod.wu
  * div.d
  * mod.d
  * div.du
  * mod.du
  * slti
  * sltui
  * addi.w
  * addi.d
  * lu52i.d
  * andi
  * ori
  * xori
  * lu12i.w
  * lu32i.d
  * pcalau12i

- 移位类运算
  * sll.w
  * srl.w
  * sra.w
  * sll.d
  * srl.d
  * sra.d
  * slli.w
  * slli.d
  * srli.w
  * srli.d
  * srai.w
  * srai.d

- 访存
  * ld.w
  * ld.d
  * st.w
  * st.d

- 转移
  * beqz
  * bnez
  * jirl
  * b
  * beq
  * bne
  * blt
  * bge
  * bltu
  * bgeu

## 宏指令

### LA64

- li.w 
  * li.w rd, imm32
  * 将一个 32 位立即数加载到寄存器 rd 中
  * 该宏指令会根据立即数的值选择使用 lu12i.w + ori 或 addi.w 来实现立即数加载操作

- li.d
  * li.d rd, imm64
  * 将一个 64 位立即数加载到寄存器 rd 中
  * 该宏指令会根据立即数的值选择使用 lu52i.d + lu32i.d + lu12i.w + ori 或退回 li.w 来实现立即数加载操作

- la.abs
  * la.abs rd, symbol
  * 将一个绝对符号地址加载到寄存器 rd 中，该符号必须为绝对符号
  * 该宏指令会使用 lu52i.d + lu32i.d + lu12i.w + ori 来实现符号地址加载操作

- la.pcrel
  * la.pcrel rd, symbol
  * 将一个相对符号地址加载到寄存器 rd 中，该符号必须为相对符号，范围为32位之内
  * 该宏指令会使用 pcalau12i + addi.d 来实现符号地址加载操作

- ret
  * ret
  * 从当前过程返回
  * 该宏指令被展开为 jirl zero, ra, 0

- move
  * move rd, rj
  * 将寄存器 rj 的值复制到寄存器 rd 中
  * 该宏指令被展开为 or rd, rj, zero

- sle
  * sle rd, rj, rk
  * 如果寄存器 rj 的值小于或等于寄存器 rk 的值，则将寄存器 rd 的值设置为 1，否则设置为 0
  * 该宏指令被展开为 slt rd, rk, rj; xori rd, rd, 1

- sge
  * sge rd, rj, rk
  * 如果寄存器 rj 的值大于或等于寄存器 rk 的值，则将寄存器 rd 的值设置为 1，否则设置为 0
  * 该宏指令被展开为 slt rd, rj, rk; xori rd, rd, 1

- sleu
    * sleu rd, rj, rk
    * 如果寄存器 rj 的值无符号小于或等于寄存器 rk 的值，则将寄存器 rd 的值设置为 1，否则设置为 0
    * 该宏指令被展开为 sltu rd, rk, rj; xori rd, rd, 1

- sgeu
    * sgeu rd, rj, rk
    * 如果寄存器 rj 的值无符号大于或等于寄存器 rk 的值，则将寄存器 rd 的值设置为 1，否则设置为 0
    * 该宏指令被展开为 sltu rd, rj, rk; xori rd, rd, 1

- seq
  * seq rd, rj, rk
  * 如果寄存器 rj 的值等于寄存器 rk 的值，则将寄存器 rd 的值设置为 1，否则设置为 0
  * 该宏指令被展开为 xor rd, rj, rk; sltui rd, rd, 1

- sne
  * sne rd, rj, rk
  * 如果寄存器 rj 的值不等于寄存器 rk 的值，则将寄存器 rd 的值设置为 1，否则设置为 0
  * 该宏指令被展开为 xor rd, rj, rk; sltu rd, zero, rd