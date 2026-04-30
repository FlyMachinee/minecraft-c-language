# 用户手册

## C

- 单 main 函数
- 函数体为单行语句
- 函数体只支持返回由表达式运算的整数常量

### 运算符

#### 一元表达式

- 负号 -
- 按位取反 ~

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

#### 逗号表达式

- 括号运算符 ()

## 指令

### LA64

#### 整数

- 算术类运算
  * add.w
  * sub.w
  * nor
  * and
  * or
  * xor
  * mul.w
  * div.w
  * mod.w
  * addi.w
  * addi.d
  * andi
  * ori
  * xori
  * lu12i.w

- 移位类运算
  * sll.w
  * sra.w
  * slli.w
  * srai.w

- 访存
  * ld.w
  * ld.d
  * st.w
  * st.d

- 转移
  * jirl

## 宏指令

### LA64

- li.w 
  * li.w rd, imm32
  * 将一个 32 位立即数加载到寄存器 rd 中
  * 该宏指令会根据立即数的值选择使用 lu12i.w + ori 或 addi.w 来实现立即数加载操作

- ret
  * ret
  * 从当前过程返回
  * 该宏指令被展开为 jirl zero, ra, 0

- move
  * move rd, rj
  * 将寄存器 rj 的值复制到寄存器 rd 中
  * 该宏指令被展开为 or rd, rj, zero