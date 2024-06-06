+ NodeProxy {
	currentSettingsAsCompileString { | envir |
		var nameStr, accessStr = "a";
		var isAnon;

		envir = envir ? currentEnvironment;

		nameStr = envir.use { this.asCompileString };
		isAnon = nameStr.beginsWith("a = ");
		if (isAnon.not) { accessStr = nameStr };

		^this.nodeMap.asCode(accessStr, true);

	}
}