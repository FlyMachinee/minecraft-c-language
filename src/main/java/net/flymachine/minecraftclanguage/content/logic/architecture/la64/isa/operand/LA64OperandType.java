package net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.operand;

import net.flymachine.minecraftclanguage.content.logic.architecture.la64.util.BitMath;

public enum LA64OperandType {
    GPR, FPR, UI5, UI6, SI12, SI20, UI12, OFFS16, OFFS21, OFFS26, CFR;

    public final static LA64OperandType[] FORMAT_2FPR_OPTYPE
        = new LA64OperandType[]{LA64OperandType.FPR, LA64OperandType.FPR};
    public final static LA64OperandType[] FORMAT_3GPR_OPTYPE
        = new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.GPR, LA64OperandType.GPR};
    public final static LA64OperandType[] FORMAT_3FPR_OPTYPE
        = new LA64OperandType[]{LA64OperandType.FPR, LA64OperandType.FPR, LA64OperandType.FPR};
    public final static LA64OperandType[] FORMAT_2GPR_UI5_OPTYPE
        = new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.GPR, LA64OperandType.UI5};
    public final static LA64OperandType[] FORMAT_2GPR_SI12_OPTYPE
        = new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.GPR, LA64OperandType.SI12};
    public final static LA64OperandType[] FORMAT_FPR_GPR_SI12_OPTYPE
        = new LA64OperandType[]{LA64OperandType.FPR, LA64OperandType.GPR, LA64OperandType.SI12};
    public final static LA64OperandType[] FORMAT_2GPR_UI12_OPTYPE
        = new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.GPR, LA64OperandType.UI12};
    public final static LA64OperandType[] FORMAT_2GPR_OFFS16_OPTYPE
        = new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.GPR, LA64OperandType.OFFS16};
    public final static LA64OperandType[] FORMAT_1GPR_OFFS21_OPTYPE
        = new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.OFFS21};
    public final static LA64OperandType[] FORMAT_OFFS26_OPTYPE
        = new LA64OperandType[]{LA64OperandType.OFFS26};

    public boolean representable(int imm) {
        return switch (this) {
            case GPR, FPR, UI5 -> BitMath.isUi5(imm);
            case UI6 -> BitMath.isUi6(imm);
            case SI12 -> BitMath.isSi12(imm);
            case SI20 -> BitMath.isSi20(imm);
            case UI12 -> BitMath.isUi12(imm);
            case OFFS16 -> BitMath.isOffs16(imm);
            case OFFS21 -> BitMath.isOffs21(imm);
            case OFFS26 -> BitMath.isOffs26(imm);
            case CFR -> (imm & ~0b111) == 0;
        };
    }

}
