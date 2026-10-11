package net.flymachine.minecraftclanguage.content.logic.architecture.la64.abi.lp64d.callConv;

import java.util.List;

public record Signature(List<Placement> params, Placement ret, long stackSize) { }
