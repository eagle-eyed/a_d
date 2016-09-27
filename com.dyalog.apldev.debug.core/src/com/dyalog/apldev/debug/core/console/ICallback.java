package com.dyalog.apldev.debug.core.console;

public interface ICallback <Ret, Arg> {

	Ret call(Arg arg);

}
