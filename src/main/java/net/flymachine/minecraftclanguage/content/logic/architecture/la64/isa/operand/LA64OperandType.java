package net.flymachine.minecraftclanguage.content.logic.architecture.la64.isa.operand;

public enum LA64OperandType {
    GPR, FPR, SI12, SI20, UI12, OFFS16;

    public final static LA64OperandType[] FORMAT_3GPR_OPTYPE
        = new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.GPR, LA64OperandType.GPR};
    public final static LA64OperandType[] FORMAT_2GPR_SI12_OPTYPE
        = new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.GPR, LA64OperandType.SI12};
    public final static LA64OperandType[] FORMAT_2GPR_UI12_OPTYPE
        = new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.GPR, LA64OperandType.UI12};
    public final static LA64OperandType[] FORMAT_2GPR_OFFS16_OPTYPE
        = new LA64OperandType[]{LA64OperandType.GPR, LA64OperandType.GPR, LA64OperandType.OFFS16};

}
