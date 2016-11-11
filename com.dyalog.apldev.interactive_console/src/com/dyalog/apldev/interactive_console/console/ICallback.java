package com.dyalog.apldev.interactive_console.console;

public interface ICallback <Ret, Arg> {

	Ret call(Arg arg);

}
